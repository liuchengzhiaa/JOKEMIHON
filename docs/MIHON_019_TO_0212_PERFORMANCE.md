# Mihon v0.19.0 → v0.21.2：书架为何更流畅、更稳定

## 范围与结论

按 v0.19.0 比较，而不是把所有 v0.19.x 当成同一个版本：后续 0.19.x 已经包含部分
内存/数据库修复。如果实际使用的是 0.19.6，其中一些收益并非这次升级才出现。

- 基线：e4de208cf75e273b425bf4b1fe4bd7bc3d9cc459。
- 目标：9612d955db3165e1578fdf9ff19ca9b7762c4a46（本项目当前上游基线）。
- 依据：已读取官方 Release notes、本地对应提交的实际 diff 和目标版本源码。
- 本次未运行 APK、未读取用户书库、未采集设备 trace/崩溃日志或做 A/B benchmark。
  以下是能从源码确认的机制及合理影响，不能量化提速倍数或归因某台手机的全部改善。

核心不是换了一种书架动画，而是减少数据库工作量、重复计算、主线程 I/O 和无意义的
后台刷新；同时保留可复用的页面状态，并修复多条独立的崩溃路径。

## 1. 先看完整的数据链

```text
LibraryTab 收集 LibraryViewModel.state
  → library 合并搜索、分类、收藏、追踪筛选和显示选项
  → getFavoritesFlow → GetLibraryManga.subscribe
  → MangaRepositoryImpl.getLibraryMangaAsFlow
  → libraryViewQueries.library → subscribeToList
  → SQLDelight asFlow → pausedWhileUnsubscribed → mapToList
  → 查询/映射结果 + 来源信息 + 下载计数
  → 过滤、分组、排序（Dispatchers.Default）
  → StateFlow → 书架网格/列表
```

查询更新的门控发生在 mapToList 之前，而不只是 UI 最后忽略结果。屏幕隐藏后暂缓
高频查询通知、合并中间更新，重新可见时继续；保留已有状态避免重新从空白加载。

关键路径：
- `app/src/main/java/eu/kanade/tachiyomi/ui/library/LibraryViewModel.kt`
- `domain/src/main/java/tachiyomi/domain/manga/interactor/GetLibraryManga.kt`
- `data/src/main/java/tachiyomi/data/manga/MangaRepositoryImpl.kt`
- `data/src/main/java/tachiyomi/data/QueryExtension.kt`
- `core/common/src/main/kotlin/tachiyomi/core/common/util/lang/SubscriberGate.kt`

## 2. 切回书架不必重新跑完整流水线

[#4071](https://github.com/mihonapp/mihon/pull/4071)，提交
[`260e31cd2`](https://github.com/mihonapp/mihon/commit/260e31cd2c777ea29f04501825e65b9e6aa236cf)：

- `SubscriberGate` 保留 Flow 和上次页面状态，而不是简单取消并重建全部订阅。
- 无订阅持续 5 秒后关闭门控；数据库和下载缓存的高频更新等待重新可见。
- `conflate()` 合并等待期间的中间更新，不把每一次旧变化都重放。
- 之前的 #3762 已做订阅生命周期优化；必须以最终 #4071 实现为准，不能把某个
  中间提交的 WhileSubscribed 行为当成 v0.21.2 的全部实现。

直接对应：从详情/阅读器/其他页回到书架时，更容易立即显示已有内容，减少闪空和
反复排序。只在对应 ViewModel 存活时成立；进程被系统杀死后的冷启动仍要重新加载。

## 3. 数据库只做书架真正需要的工作

- [表结构与视图调整 #3805](https://github.com/mihonapp/mihon/commit/93cc07511628961e5e8b6c57053f5f7f511a63d3)：
  v0.19.0 的章节/分类子查询先对相关全表聚合，外层才筛收藏；新版子查询先限制到
  收藏漫画的 ID。非书架浏览残留越多，减少无关聚合越有意义。
- [最近阅读时间查询优化](https://github.com/mihonapp/mihon/commit/5df78e305e9d48f5b0663a8ddd5d128e3d5d7828)：
  不再为计算 lastRead 总是把章节与阅读历史连接起来。普通条目直接按 history.manga_id
  取时间；仅排除了翻译组的条目需要走额外章节连接以维持原语义。
- 阶段性补齐查询索引（#2950），之后新 schema 又重建相应索引/唯一约束。不能把旧表
  索引名直接说成新版本仍在使用。最终 schema 例如收藏 partial index、history.manga_id
  索引及 manga/source+URL、chapter/manga+URL 唯一约束。

关键文件：`data/src/main/sqldelight/tachiyomi/view/libraryView.sq`、
`data/src/main/sqldelight/tachiyomi/data/{manga,chapter,history}.sq`。

直接对应：冷启动、返回后的数据刷新、章节状态改变时重算更轻，尤其是大数据库。
当前仍通过 subscribeToList 形成书架列表；这不是数据库层的分页书架。

## 4. 不为每一本书重复获取附加信息

- [追踪数据按需读取](https://github.com/mihonapp/mihon/commit/940272f75a228938ff9f122a382ad2bae52f7ce3)：
  只有启用追踪筛选或追踪评分排序时才订阅全部 tracks；普通浏览不再等这些数据。
- 来源按唯一 source ID 先取一次；下载计数先复用，再在 #4071 批量查缓存。
  没有下载的来源跳过昂贵的下载目录名构造。不是把旧逻辑的每本调用换成全盘扫描。
- 排序直接使用已有 Collator 的大小写处理，移除每次比较都创建 lowercase 字符串。
- `LibraryItem.Badges` 与实际计数/本地标志分离，显示开关不再让筛选回退到重复查询
  下载计数；相关历史提交为 c612a6ca8 和 f027de8c2。

直接对应：收藏越多，每次过滤、分组和排序省下的重复工作越明显。没有启用追踪功能
或没有本地下载时，某些优化的收益会小得多。

## 5. 把耗时工作移出主线程，减少封面磁盘访问

[#4068](https://github.com/mihonapp/mihon/pull/4068)，提交
[`c46317f09`](https://github.com/mihonapp/mihon/commit/c46317f099e6c14c19dce69dfab9482ea8089048)：

- 对磁盘、网络、封面和多个页面操作明确线程/作用域；书架 CPU 计算改用 Default。
  v0.19.0 书架本来就有 IO 协程，不应说旧版所有书架计算都在主线程。
- 自定义封面目录后台建立文件名索引；索引完成后，缓存 key 判断可查内存集合，避免
  每次给卡片取 key 都检查文件是否存在。索引未完成时仍有 exists() 回退。
- 部分初始化改为首次使用才创建；偏好与 Main dispatcher 的首次加载有后台预热。
- v0.19.4 已引入显式内存封面缓存预算调整
  [#2266](https://github.com/mihonapp/mihon/commit/4c9cfd8da5f9c67daa4b6401a910f979fd79179f)。
  目标源码调用 maxSizePercent(context)，这里不推断依赖内部默认百分比。

直接对应：更少磁盘等待和分配压力干扰滑动；减少 OOM 风险是合理机制解释，不是本次
设备内存测量。自定义封面索引主要帮助使用了自定义封面的判断链。

## 6. 降低后台写入和刷新对前台的干扰

#4071 包括：
- 翻页进度一般等停留 2 秒再写；章节完成立即保存，切章/生命周期相关保存路径会冲刷
  待保存进度，而不是每翻一页都触发数据库写入。
- 写入通过 mutex 保序，应用级作用域允许保存任务在阅读页关闭后继续执行。
- 没有放置桌面组件时不继续观察组件更新查询。
- 下载进度/书库更新通知节流，合并获取时间等写入。

直接对应：减少高频写入触发其他查询失效、争用 CPU/磁盘和通知刷新的连锁影响。
它们不是全部直接发生在书架组件里，却会影响书架同时运行时的流畅度。

## 7. 崩溃减少并非只有一种原因

### 数据库并发

[最终 WAL 配置](https://github.com/mihonapp/mihon/commit/61cd786e7f38c210e6a9ce4f95cafd7689ff982b)：
目标源码 `DatabaseBindings.kt` 使用 bundled SQLite、MultipleReadersSingleWriter，
isWal=true，普通设备 4 个 reader、低内存设备 1 个 reader，每连接 busy_timeout=3000ms。
这降低读写互相阻塞及短暂锁竞争直接报错的概率，不代表 SQLite 从此不可能锁冲突。
以最终 61cd786e7 为准；此前 38e93086c 的中间实现已经被修订。

### 错误隔离与具体缺陷

- 书架跨分类范围选择增加 category/漫画锚点与索引存在性检查，避免 -1 索引路径
  （a6ee4bc59）。
- 应用级协程作用域使用 SupervisorJob；非 debug 构建的未捕获子任务异常记录/上报，
  不再直接拉倒整个应用（f9c5fa901）。这是部分错误的隔离，不是错误被修好或任务成功。
- v0.21.1 处理 WebView renderer 退出、部分旧三星系统启动和扩展安装竞态。
- v0.21.2 按 host 协调 Cloudflare 挑战，同站并发请求等待共享处理，避免各自打开一个
  WebView 引发资源暴涨（5ca2a3932）；并修复退出/返回手势、阅读器边界等崩溃。
- 升级迁移额外处理重复数据/损坏索引等旧数据库情况；升级过程与日常书架加载应分开。

若用户旧崩溃实际来自阅读器/扩展/WebView，则书架看起来更稳定也可能是这些全局修复的
结果；没有对应崩溃栈不能定案，更不能把“几乎没有发生”当成零崩溃保证。

## 8. 不该误归因的新特性

实际比较确认两个版本的 `LazyLibraryGrid.kt` 与 `LibraryPager.kt` 完全相同。
懒布局、分类页相邻页限制以及图片 fetch=8 / decode=3 的并发限制在 v0.19.0 已存在。
不能把它们宣传为 v0.21.2 新加入的性能方案，也不能说新版已经把整库读取变成分页。

[官方 v0.20.0 说明](https://github.com/mihonapp/mihon/releases/tag/v0.20.0) 已说明数据库提速；
[官方 v0.21.2 说明](https://github.com/mihonapp/mihon/releases/tag/v0.21.2) 包含 0.21.0 的性能
改进和 0.21.1/0.21.2 的稳定性补丁。最关键的书架/跨页优化主体在 0.21.0，不是单个
0.21.2 小补丁。后续 JOKEMIHON 定制应保留这些数据流、缓存预算与并发边界。

## 复核方式

只读、不会切换工作区的命令（普通可信 Git 终端）：

```powershell
git show e4de208:data/src/main/sqldelight/tachiyomi/view/libraryView.sq
git diff 93cc07511^ 93cc07511 -- data/src/main/sqldelight/tachiyomi/view/libraryView.sq
git diff 5df78e305^ 5df78e305 -- data/src/main/sqldelight/tachiyomi/view/libraryView.sq
git diff 260e31cd2^ 260e31cd2 -- app/src/main/java/eu/kanade/tachiyomi/ui/library/LibraryViewModel.kt data/src/main/java/tachiyomi/data/QueryExtension.kt
git diff --quiet e4de208 9612d95 -- app/src/main/java/eu/kanade/presentation/library/components/LazyLibraryGrid.kt app/src/main/java/eu/kanade/presentation/library/components/LibraryPager.kt
```

最后一条已执行，exit 0。需要做定量结论时，应在相同设备、相同可恢复书库、相同封面
缓存状态下对比冷启动/切回时间、帧耗时、峰值堆内存、GC 和崩溃栈；本次没有做这些测试。