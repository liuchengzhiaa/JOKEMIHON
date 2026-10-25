# JOKEMIHON handoff

## Current slice: joke.6 legacy tag compatibility (2026-10-25, BUILD/PUBLICATION PENDING)

Owner reports existing library tags absent in manager cards and tag picker. Source
confirms LocalSource legacy manga.url = raw original directory name, chapter.url =
parent/entry; code43 looked up only virtual-work hashes and picker ignored explicit
category target IDs. No phone DB was supplied/inspected; runtime acceptance pending.
Continued owner no-test workflow: no unit/Lint/format/device checks, compile only.

- Add read-only one-to-one aliases from completed author scan: a legacy favorite must
  contain exactly one current work, and all stored chapter URLs must match it. Exact
  virtual favorite wins. Alias build async/guarded against stale work/favorite snapshots.
- Category prefill uses actual explicit manga ID when editing; legacy local identity
  validated as one original parent segment. Save edits that same existing row, not a
  new hashed single. Batch prefill/add resolver rechecks live full-directory coverage.
  Open preserves original manga ID; new/unmatched singles still prepare virtual rows.
- Multiple-work parent tags are NOT silently inherited. Show related original and
  collection tag cards in preview/import/picker, with explicit original-entry editor
  in preview. Own tags highlighted in normal grid; related tags secondary/read-only.
  Author rows show aggregate label marker; child/group cards show contains-tag hint
  when only related entries carry tags. No false favorite Check for partial children.
- Existing exact child/group separation retained; no automatic copying of tags or
  favorites, no schema/backup/memo migration. Existing backups retain IDs/categories;
  aliases rebuilt from same paths + restored library data. Legacy reader behavior
  remains upstream for the explicitly reused original book (no automatic file writes).
- Candidate0.21.2-joke.6/code44, same package/signing config. Pending source review,
  Release compile, push/tag/public APK delivery. Owner should back up then verify
  single-work legacy, multi-work parent context, virtual exact tags and batch behavior.

---

## Current slice: joke.5 logical work directories/tags (2026-10-25, PUBLISHED; NOT TESTED)

Owner asked to implement the agreed separation plus lightweight tag badges and a
searchable tag-grid picker. Continued the requested NO-test workflow: no unit/Lint/
format/static-signature/emulator/device tests; only compile/package and delivery.

- Work selection now creates a persistent logical directory WITHOUT favoriting or
  changing tags. Click its card for children, long-press or Directory options for
  directory import/categories/reader. Children always open/import their exact single
  identity; removed previous member-to-parent fallback. Nested work dirs not offered.
- Existing ordered LocalWorkDefinition URL identities unchanged; old favorited
  combinations are adopted as directory metadata, not duplicated/remapped. Folder
  contents are immutable after creation; name entered at creation, chosen order kept.
- Directory metadata app-state preference + bounded validated LocalWorkFolders model
  (10k dirs,100k total member references,8MiB encoded; max1000 per dir). Anchored by
  first physical parent name, not mutable author alias. Unrecorded physical files stay.
  Author manual groups still field107. New optional field108 travels with Library,
  independent of App settings and whether directories are favorited. Restore validates
  complete record before replacement; absent108 preserves current directories.
- Folder page cover stored independently; child-preview first-page choice needs no
  manga creation. Directory-reader long-press stores portable page ref into both
  manga and metadata, including unadded dirs. Media files never changed. Child covers,
  exact tags and progress remain independent; legacy-only progress inheritance unchanged.
- Card label vector badge reads exact favorite named-tag membership, no extra media
  decoding/per-card scans. Standard library flow supplies live category IDs. Default-
  only favorites have Check badge, no named-tag Label. Folder has Folder badge.
- Choose tags button opens searchable lazy grid; filled means selected, partial state
  explicit. Cancel discards nested draft. Done choosing returns to import dialog;
  outer Save/Confirm applies explicit deltas, preserving untouched mixed tags.
- Existing default-off removal option kept ONLY on directory import; creation never
  removes original favorites, and children/dirs otherwise remain independent.
- Published 0.21.2-joke.5 / code43, existing package/private signer configuration.
- First compile failed2m16s ONLY because Label was in AutoMirroredRounded, not Rounded;
  reused existing icon/import, committed fix. Final app:assembleRelease passed9m11s.
  Existing test fixture constructor adjusted for new injected store, NOT executed.
  No unit/Lint/format/static-signature/emulator/device/behavior tests this slice.
- Reviewed16 unique source/resource/fixture/docs files; source commits5b8f368b8 and
  438de729ced6e71ac5048230cfc1d489050d423a pushed/read back. Immutable annotated
  v0.21.2-joke.5 targets438de729c; subsequent receipt-only docs do not rebuild APK.
- PUBLIC prerelease ID423237475, published2026-10-25T08:05:51Z; draft=false,
  prerelease=true, not promoted to latest/stable. Existing stable remains joke.2.
- Release: https://github.com/liuchengzhiaa/JOKEMIHON/releases/tag/v0.21.2-joke.5
- APK: https://github.com/liuchengzhiaa/JOKEMIHON/releases/download/v0.21.2-joke.5/JOKEMIHON.apk
- APK67900535 bytes, SHA2569baacf1eb16e3f00a9b18e6437d9aee526950499774669e4db6ac1e3c23dfdbc.
  Three remote asset sizes/digests match local APK/checksum/manifest (67900535/80/1982
  bytes). Anonymous public API readback confirms public APK URL; no whole-download
  test repeated. Receipt ignored .tools/releases/v0.21.2-joke.5/publication-receipt.json.
- Only reviewed source/docs and Release APK/checksum/manifest uploaded; no keys,
  local config/auth caches/media/backups. Signer offline backup still owner-pending.
- Safest next action: owner backs up Library+Chapters+Categories BEFORE installing;
  phone check creation without import, directory vs child independent tags/covers,
  old combination compatibility, badges/picker and new-phone ordinary backup/restore.
  Build success/older tests are NOT runtime acceptance for this preview.

---

## Current slice: joke.4 optional removal of originals (2026-10-25, PUBLISHED; NOT TESTED)

Owner explicitly requested this option and NO tests; do not run unit tests, Lint,
format checks, emulator or phone tests for this slice. Compile/package only.

- Added default-off checkbox to combined import only. Fully covered local virtual
  singles/bundles and legacy physical-directory library entries can be unfavorited.
- Full virtual memo coverage required; malformed definitions remain untouched.
  Legacy coverage uses live folder works AND stored chapter URLs; partial originals
  stay. New saved target is always excluded from removal. New collection/categories
  are saved first; removal failure may leave duplicates rather than lose originals.
- Only favoriteAt becomes null. Never delete media, chapter/history/category rows.
  Old tags no longer display the unfavorited item; new tags are explicitly selected.
  Progress inheritance is unchanged (legacy parents only, one-time, no later sync).
- Same package/signer, candidate 0.21.2-joke.4 / code42. No new backup schema needed.
- app:assembleRelease passed in 9m42s; code42 universal signed/minified APK packaged.
  No unit/Lint/format/static-signature/device/behavior tests were run, by request.
- Source/main ea772c0fa161da903f917441c3c5e03ae8586e83 pushed/read back;
  immutable annotated v0.21.2-joke.4 targets this source commit.
- PUBLIC prerelease ID423210499, published 2026-10-25T07:20:38Z; draft=false,
  prerelease=true; previous stable/latest remains joke.2 (not promoted).
- Release: https://github.com/liuchengzhiaa/JOKEMIHON/releases/tag/v0.21.2-joke.4
- APK: https://github.com/liuchengzhiaa/JOKEMIHON/releases/download/v0.21.2-joke.4/JOKEMIHON.apk
- APK67873035 bytes, SHA256 0db37a2cc44a0cc5ea622ff2f66f6a2efc39f47f82cd8f0b4a50576dc75c6899.
  Three remote asset sizes/digests match local APK/checksum/manifest; anonymous public
  API readback confirms published APK URL. No repeated whole-download check this slice.
  Local receipt: .tools/releases/v0.21.2-joke.4/publication-receipt.json (ignored).
- Reviewed10-file source upload and later receipt-only docs; no keys/config/media/APKs
  in Git. Signer configuration unchanged; owner offline backup remains pending.
- Safest next step: owner creates Library+Chapters+Categories backup BEFORE replacing
  originals, installs preview and checks on phone. Re-add can restore original favorite
  membership; backups created AFTER removal omit unfavorited originals. No behavioral
  guarantees inferred from successful compilation or previous-version test counts.

---

## Latest delivery: joke.3 local manager preview (2026-10-25, PUBLISHED)

Owner requested compact UI, manual logical-directory merge/rename, long-press-only
cover checkboxes, removable tags and backup clarity. Explicitly requested simple
validation and APK handoff for owner testing, NOT another extended emulator run.
Completed code/source/static verification and public APK delivery; phone UI acceptance
is still PENDING, not implied by the previous joke.2 device receipt below.

### Publication receipt

- Preview: https://github.com/liuchengzhiaa/JOKEMIHON/releases/tag/v0.21.2-joke.3
- APK: https://github.com/liuchengzhiaa/JOKEMIHON/releases/download/v0.21.2-joke.3/JOKEMIHON.apk
- Package app.jokemihon, 0.21.2-joke.3 / code41; same pinned RSA4096/v2 signer.
- Source/APK commit c2f1fd2872178c757da6961af8e06240c514c312 / embedded c2f1fd287;
  annotated tag v0.21.2-joke.3 immutable at that commit. Normal push/readback verified.
- Release ID 423199881, published 2026-10-25T06:58:05Z;
  draft=false, prerelease=true, PUBLIC. The previous stable/latest remains joke.2;
  this preview has a direct public URL. Actions remain disabled.
- Reviewed18-file source upload; exactly3 release assets, all remote sizes/digests
  verified before publication: APK67871487 bytes, checksum81, manifest3634.
- APK SHA256 d96c1773059f68f6ce63d2634c785c65dabfe8e9e5220fa413cd5c184c019342.
- Independent opener without token/auth/cookies downloaded entire APK, HTTP200,
  matching size and hash. Receipt ignored .tools/releases/v0.21.2-joke.3/
  public-download-check/receipt.json. Keys/config/tools/media/backup files not uploaded.
- Subsequent docs-only receipt advances main; no identical APK rebuild, tag move or
  asset overwrite. Working source scope is this Mihon fork, not JOKESONG.

### Implementation and basic verification

- Rounded compact directory cards/search/grid. Root long-press ordered selection:
  merge2+ with first name by default, rename single, repeated flat merge, dissolve to
  auto grouping. UUID/name + original directory membership saved in app-state preference.
  No physical files, existing manga identities/titles/progress or DB schema altered.
  Missing recorded folders stay remembered; new unrecorded folders stay auto grouped.
- Work checkboxes/select-all hidden until long-press; clear/back/last deselection exits.
- Current category memberships load into tri-state checkboxes; explicit add/remove only,
  untouched mixed tags preserved. Removing all named tags keeps favorite in Default.
  Existing member preview edits actual saved single/collection categories, no accidental
  standalone duplicate. Category-only edits require no media availability.
- Optional backup field107 carries validated manual-rule JSON with Library, independent
  of App settings. Restore Library with field replaces manual rules only after complete
  validation; older absent-field backups keep current rules. Existing manga memo/page
  covers and chapter progress keep original backup fields. Include Chapters/Categories.
- Focused37 tests passed; 11 added (8 grouping/backup +3 category service). Initial
  combined task failed only a max-line-length format error; reflowed, no suppression.
- Full testDebugUnitTest/lintDebug/app+source-local spotlessCheck passed in8m18s:
  12 JUnit XML suites,162 tests, zero failures/errors/skips. Inspected15 Lint XMLs:
  zero Error/Fatal,152 warnings. Resource parity49 keys; git diff --check passes.
- app:assembleRelease passed in9m10s. Static universal APK checks: package/code41/name,
  same signer pin,4 ABIs, min26/target36, embedded source commit, updater/telemetry false,
  size/hash. Build used isolated D: JDK21/cache and serial/in-process3GB settings.
  Dedicated Gradle daemon stopped; NO AVD/physical-device actions this slice.
- Local logs, test reports and release artifacts remain ignored under .tools.

### Pending owner acceptance / safest next step

Install preview over previous JOKEMIHON AFTER a normal backup. Check both revised
screens, long-press selection, merge/rename/dissolve across restart, removal of tags,
and ordinary backup/restore of new manual groups on owner phone. No code41 device
startup/upgrade/UI/restore test has been performed by Codex. Do not label it device-
verified or reuse joke.2 screenshots as new evidence. Report owner findings for code42.

Backups contain references/metadata, NOT comics/gallery-cover caches. Preserve relative
paths and grant storage after restore. Legacy/new progress stays independent after
one-time inheritance. Dissolve is auto regrouping, not nested undo history. Normal
backup bounds/semantics in LOCAL_WORKS.md. Signer offline backup of BOTH ignored master
and properties still owner-pending; never delete/regenerate/upload either.

---

## Previous delivery: local works manager (2026-10-25, PUBLISHED AND VERIFIED)

Completed the owner-approved non-destructive local works manager. More > 本地作品管理
uses original SAF storage and keeps old folders/library intact. Source and tested APK
are synchronized to the public owner's repository. No physical phone or JOKESONG edits.

### Delivery receipt

- Release: https://github.com/liuchengzhiaa/JOKEMIHON/releases/tag/v0.21.2-joke.2
- APK: https://github.com/liuchengzhiaa/JOKEMIHON/releases/download/v0.21.2-joke.2/JOKEMIHON.apk
- Source/APK commit f4630606ea10b0d89d1cccb5270c2ff35b573740; embedded SHA f4630606e. Immutable annotated
  tag v0.21.2-joke.2 resolves to that commit. Normal main push and API readback verified.
- Package app.jokemihon, 0.21.2-joke.2, versionCode40; same pinned RSA4096 signer as code39,
  v2 signature, min26/target36, all four ABIs. Upstream updater/telemetry remain false.
- Release ID 423178263, published 2026-10-25T06:01:53Z; draft=false,
  prerelease=false, PUBLIC. Existing Actions remain disabled; no workflows restored.
- All 3 remote sizes/digests matched before publication: APK 67846047 bytes,
  checksum81 bytes, build-manifest5791 bytes. No secrets/media/keys/tools uploaded.
- APK SHA256: 1cfb358513abe0e4677c2a697f877d54f4f8656d20b512e5f99645ca33a095f0.
- Independent urllib opener WITHOUT login/auth headers/cookies downloaded the WHOLE
  public APK: HTTP200, exact size/SHA256 match the installed/tested universal Release.
  Receipt: ignored .tools/releases/v0.21.2-joke.2/public-download-check/receipt.json.
- Source upload set: 25 reviewed source/resource/test/docs files. This subsequent
  docs-only receipt advances main; do not rebuild the same APK, move tag or overwrite assets.

### Verification

- Full testDebugUnitTest, lintDebug, app/source-local spotlessCheck passed in7m37s.
  All 11 Debug JUnit XMLs: 151 tests, zero failures/errors/skips, including 26 new
  model/service tests (17+9). All 15 Lint XMLs: zero Error/Fatal, 149 warnings.
  33 default/zh-CN resource keys match; git diff --check passed.
- Signed/minified/resource-shrunk app:assembleRelease passed in9m58s. Exact universal
  APK metadata, signature pin, four ABIs and generated BuildConfig inspected.
- Actual Release upgraded published code39 in JOKEMIHON_CategoryWall_Test, Android16/
  API36 Google APIs x86_64. After first add, all 31 original manga rows, original
  chapter and all categories/legacy memberships matched pre-upgrade fixture exactly.
- Real system SAF grant; nine physical folders -> five groups; five split folders ->
  eight flat work cards. Real covers, manual six-click order/ordinals, arrow reorder
  and automatic 作品D 上（六部曲） title checked. Detail lists six chapters in that order.
- Legacy read/bookmark/page inheritance checked. Two works in one unsplit author
  folder separately added to category03. Re-add exact same six kept manga102 and
  page-cover/progress, unioned categories01+09 without duplicate creation.
- ZIP natural page order read; second (blue) page selected through normal reader
  menu. Portable page index1 saved; blue library cover survives cold restart.
- Actual normal .tachibk created via UI and decoded:34 manga, three virtual definitions,
  original ordered chapters and page cover. Saved coherent DB snapshot, removed ONLY
  three synthetic virtual entries from derived test DB, then actual UI restore rebuilt
  all three; order/categories/read state and blue cover survived. JPEG-directory last
  chapter reads three pages afterward. Normal manga Refresh preserved six chapters,
  metadata and selected cover.
- All 34 synthetic files under local retained EXACT filenames/SHA256, including old
  cover.jpg/.noxml; no new media/metadata files. Empty crash buffer, no FATAL/app ANR/
  SQLite/OOM hits; eight app exit records were intentional test FORCE STOP only.
- Full screenshots/XML, consistent DB snapshots, file manifest and logs remain ignored
  in .tools/verification/local-works-ui. Dedicated AVD shut down after verification.

### Limits and safest next action

- See LOCAL_WORKS.md for semantics. Existing progress is copied once; old/new entries
  then advance independently. Different orders intentionally mean different bundles.
  Batch adds are sequential/resumable, not one transaction. Max1000 per operation.
- Backup contains references, NOT comics; relative paths + SAF grant are required.
  Reader-page covers are portable; gallery overrides remain upstream private cache.
- No physical-phone/full extension/tracker/network-reader regression or large real-library
  benchmark. RAR/7z/tar/EPUB not device-tested in this slice; ZIP/CBZ covers and ZIP/JPEG
  reading exercised. Lint149 warnings remain. Release had an upstream Compose stack-
  trace mapping warning in BaseUpdatesGridGlanceWidget; build and application checks pass.
- Emulator first boot System UI ANR recovered by Wait; NOT an app ANR. Test harness
  initially force-stopped one re-add too early; proper wait/retry verified category union.
  A full-window swipe initially triggered system Home; corrected to content-area swipe,
  existing activity resumed HOT. Exit parser initially matched subreason; corrected
  word-boundary reason parsing confirms only USER REQUESTED/FORCE STOP. No code
  changes or hidden test suppression were used to reinterpret these harness failures.
- Owner offline backup of BOTH ignored signer and keystore.properties is still pending.
  Do not regenerate/delete either or upload them. Safest next action: owner phone
  acceptance using new Release after a normal backup; preserve original local layout.

---

## Previous delivery: category wall (2026-10-25, PUBLISHED AND VERIFIED)

The completed slice is delivered to the public owner repository. JOKESONG source,
physical phones and personal reading data were not touched. Notes below the first
separator are historical checkpoints, not current pending-publication statements.

### Delivery receipt

- Release: https://github.com/liuchengzhiaa/JOKEMIHON/releases/tag/v0.21.2-joke.1
- APK: https://github.com/liuchengzhiaa/JOKEMIHON/releases/download/v0.21.2-joke.1/JOKEMIHON.apk
- Source/APK commit b0a183dd8061e85c100e66415df3f9aeec07e721; embedded SHA
  b0a183dd8. Annotated tag v0.21.2-joke.1 resolves to the exact source commit.
- Normal main push and API readback verified before publication. This subsequent
  documentation-only receipt advances main; release tag/APK stay immutable at the
  tested code commit. No identical APK rebuild or tag move is needed.
- Release ID 423083096, published 2026-10-25T01:27:14Z; draft=false, prerelease=false.
  Repository PUBLIC; inherited Actions remain disabled.
- All 3 remote asset sizes/digests matched local files before publishing:
  JOKEMIHON.apk: 67794839 bytes; JOKEMIHON.apk.sha256: 80 bytes; build-manifest.json: 3369 bytes.
- APK SHA256: ececfa895e8f03999dac33c7522207498d68c41c971035d4dad5c16b388e3851.
- Independent urllib opener WITHOUT auth/cookies/token read public API and downloaded
  the whole APK: HTTP 200, exact size and SHA256 match the installed/tested universal
  Release. Local receipt: ignored
  .tools/releases/v0.21.2-joke.1/public-download-check/receipt.json.
- Exact 28-file source upload reviewed: source/resources/tests/docs/public helpers
  and public certificate pin only. Keys/APK/config/tool/auth caches stayed ignored.

### Final verification

- testDebugUnitTest lintDebug :app:spotlessCheck passed (11m 12s): 9 Debug JUnit
  suites, 125 tests, 0 failures/errors. All 15 module Lint XMLs inspected: 0 Error/Fatal,
  146 warnings remain. Narrow category-wall tests 7/7 and formatting passed.
- Actual production packer smoke: 8 boundaries, 10000-category case, 500 seeded
  property cases PASS. git diff --check and resource XML parsing passed.
- :app:assembleRelease passed (15m 14s), R8/resource shrinking enabled. Exact universal
  APK (not Debug/ABI-only substitute) verified: app.jokemihon, 0.21.2-joke.1, code 39,
  minSdk 26, targetSdk 36, 4 ABIs. Signature v2, RSA4096, one signer matches public pin.
  Generated COMMIT_SHA and updater=false/telemetry=false inspected.
- Installed exact published Release on isolated Android 16 / API 36 Google APIs x86_64
  AVD JOKEMIHON_CategoryWall_Test. Blank DB exported before inserting 40 synthetic
  categories / 30 fake local books; no media/chapters/covers. Onboarding skipped only
  for fixture setup, not fully tested. AVD stopped after tests; no phone install.
- 14 saved XML/log assertions PASS: English/Chinese controls; expansion; independent
  vertical scroll retaining book 01; direct category 40 empty-page jump; selected
  highlight and expansion retained; collapse synchronization; rotation; 150% font;
  two-line long-label selection; cold restart; existing count switch hide/restore
  via UI; horizontal page swipe to book 08 and synchronized highlight; no FATAL/own
  ANR/SQLite/OOM in final captured log; only deliberate force-stop exit reasons.
- Screenshots for normal wall, category 40, landscape, 150% font inspected. Evidence and
  scripts remain ignored .tools/verification; public build manifest gives test scope.

### Limits and safest next action

- First emulator boot showed one System UI not responding dialog; observed Wait
  recovered. No app.jokemihon crash/ANR found. Do not claim global crash-free behavior.
- Landscape expanded wall crowds/clips upstream empty-category illustration;
  collapse for book area. Selection/scrolling/state restoration still work.
- Not a physical-phone test, performance benchmark or complete reader/extension,
  tracking-login, backup/restore or migration regression. Inherited Comick
  app.mihon callback unchanged; co-installed OAuth flows not validated.
- Independent app.jokemihon coexists with official Mihon; does not automatically
  import its library. Back up original before restore; confirm download directories
  and permissions separately. Upstream updater and telemetry stay disabled.
- Persistent private signer: ignored .signing/jokemihon-release.p12 AND
  keystore.properties. Public certificate SHA256:
  f31ab9d9683c6e60fc92083a2493d754969ab10ae4b765871642a946bf5330a5.
  Owner OFFLINE backup of BOTH remains pending. Local cached duplicate is NOT an
  offline backup. NEVER upload, delete, regenerate or silently rotate the signer.
- Owner should try the APK on a phone and report density/height preferences.
  Future code slices preserve package/signer, increment versionCode above 39, verify,
  and publish NEW tags/assets. No release overwrite. Receipt-only docs need no APK.

---

## Pre-release source checkpoint (historical, superseded by receipt above)

Owner selected a multi-row category wall, not a searchable picker. Work is in this
independent Mihon checkout; JOKESONG source is untouched.

### Implemented

- Fixed right-side expand/collapse arrow; original horizontal tabs remain collapsed.
- Expanded variable-width chips retain order/count preference, selected highlight,
  two-line names and 48dp minimum height. Height is min(280dp, 35% screen height),
  with vertical lazy rows. It stays expanded after a direct stable-ID pager jump.
- rememberSaveable retains expansion through activity recreation; selected row is
  brought into view only when offscreen. No new cover fetch/query/schema migration.
- Independent name/icon/package: JOKEMIHON / app.jokemihon (debug .dev).
  Candidate 0.21.2-joke.1 / versionCode 39. Upstream updater/telemetry disabled;
  release links point to liuchengzhiaa/JOKEMIHON.
- Dedicated persistent signer is ignored .signing/jokemihon-release.p12, configured
  by ignored keystore.properties. Public certificate SHA256 is pinned in
  tools/release-signing-certificate.sha256. NEVER regenerate or upload the key.
  Owner offline backup of BOTH files remains pending; no offline backup claimed.
- Fixed inherited Lint issues without disabling checks: presentation-core default
  splash color and observable calendar locale/weekday cache invalidation.

### Verified source/build checks

- git diff --check and resource XML parsing passed; app:spotlessCheck passed.
- tools/test-category-wall.ps1: 8 boundary cases, 10000-category case and 500
  deterministic property cases PASS against the actual production Kotlin packer.
- Narrow :app:testDebugUnitTest --tests '*CategoryWallRowsTest': 7 tests PASS.
- Final testDebugUnitTest lintDebug :app:spotlessCheck: BUILD SUCCESSFUL in 11m12s.
  Parsed all 9 Debug JUnit suites: 125 tests, 0 failures, 0 errors.
  Inspected all 15 module Lint XMLs: 0 Error/Fatal, 146 warnings remain (including
  upstream/dependency warnings and a screen-height heuristic in the wall).
  Log: ignored .tools/verification/final-checks.log. Do not rely on exit code alone:
  upstream app lint abortOnError=false previously hid 3 locale errors.
- Earlier Debug APK assembly passed; metadata and dedicated certificate inspected.
  That Debug APK predates the calendar Lint fix and is NOT the release deliverable.
- Minified Release build, actual Release UI/startup, remote push and publication
  are still pending. No custom Release/APK is published yet.

### Host build environment / reproducibility

- Bundled Android Studio JDK21 crashes in jimage.dll; left untouched. System JDK17
  cannot run Metro1.4.5. Verified official Adoptium21 lives in ignored
  .tools/jdk/jdk-21.0.12.1+1. Archive SHA256:
  f9d6e191ab098c0d416e7d588a24420a8621cd2f4720dab2459b8b7b2d2d8b4e.
- C: filled during initial wrapper extraction. Removed ONLY this task's failed
  gradle-9.8.1-bin extraction; unrelated caches/files untouched. Keep Gradle home and
  temp on D: via .tools/Set-BuildEnv.ps1 / docs/RELEASING.md. SDK path is ignored.
- Memory-safe flags (PowerShell argument array): --max-workers=1,
  '-Pkotlin.compiler.execution.strategy=in-process',
  '-Dorg.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8', --console=plain.
- Isolated empty AVD JOKEMIHON_CategoryWall_Test (Android36 x86_64) is currently
  stopped for build memory. Port5588, separate ADB server5038, localhost:5589.
  Start helper .tools/emulator/Start-TestEmulator.ps1; guard exact AVD name first.
  .tools/verification/ui_probe.py prepares only a blank test library with 40 fake
  categories / 30 fake books, preserving raw baseline separately. No personal media.
  Release fixture injection requires root on this guarded userdebug AVD only.

### Safest next action

Review/stage exact source+docs (never .tools/.signing/keystore.properties), make a
local source commit, then build minified assembleRelease against that commit.
Inspect package/version/signer and test the actual universal Release APK on the
isolated AVD: expansion, vertical scrolling, far empty category, collapse sync,
rotation and large font. If successful, push main normally, publish unique
v0.21.2-joke.1 with JOKEMIHON.apk + SHA256/build manifest, read back remote refs/assets
and anonymously verify the download. Record a documentation-only receipt afterward.
Physical-phone and complete extension/tracker regression remain outside these checks.

---

## Latest continuation: public access and library research (2026-10-25)

This section supersedes the PRIVATE visibility statements in the historical
initialization entry below. The owner explicitly requested PUBLIC visibility so
future APK downloads do not need GitHub sign-in.

### Completed

- Changed only liuchengzhiaa/JOKEMIHON to PUBLIC through the authenticated CLI.
- Verified API visibility=PUBLIC, Actions still disabled and no Releases present.
- Independently verified without authentication: repository API private=false,
  visibility=public, default_branch=main and repository page HTTP 200.
- Updated AGENTS, README, release policy, TASKS and CHANGELOG for public delivery.
- Added docs/MIHON_019_TO_0212_PERFORMANCE.md: source/release-note comparison with
  exact baseline e4de208cf75e273b425bf4b1fe4bd7bc3d9cc459 (v0.19.0) and target
  9612d955db3165e1578fdf9ff19ca9b7762c4a46 (v0.21.2), real data-flow trace,
  relevant commits and explicit limits. Most core changes land in v0.21.0; later
  patches address crashes/migration/Cloudflare resource storms.
- No app/Gradle/signing changes, Android tests/builds, user media reads or device
  installation. No custom APK exists. This is a docs/configuration slice, not an
  APK release. Android build commands remain in docs/RELEASING.md.

### Verification and delivery

- Source baseline/target commits confirmed locally; selected diffs read directly.
- LazyLibraryGrid.kt and LibraryPager.kt are identical between the two tags
  (git diff --quiet exit 0); these are not new lazy-loading changes.
- git diff --check passed; reviewed upload set contains only seven Markdown files.
  No app/Gradle/signing/workflow source modifications. Commit/push succeeded;
  GitHub API verified remote main=4147715d30595861627d492951f3d17330522814.
  This receipt is a documentation-only follow-up; no APK rebuild/release is needed.
- No device traces or controlled performance/crash-rate benchmark: conclusions
  describe observed source mechanisms, not measured speedups or proof of zero bugs.

### Safest next action

Public visibility and research documentation are delivered. Next confirm the
first customization scope, establish independent app identity and dedicated stable
signer, run verification and publish the first tested APK. Keep public uploads free
of secrets and retain archived upstream workflows until a reviewed pipeline exists.

---

## 2026-10-25: repository initialization

Owner requests an independent JOKEMIHON GitHub repository. Future completed code
updates must be committed/pushed there and accompanied by a downloadable APK.
This requirement is persistent in AGENTS.md and docs/RELEASING.md.

### Implementation

- Local project directory: mihon, a sibling of the existing JOKESONG checkout.
- Base: Mihon v0.21.2, commit 9612d955db3165e1578fdf9ff19ca9b7762c4a46.
- Restored upstream history from the original shallow checkout.
- Owner repository: https://github.com/liuchengzhiaa/JOKEMIHON (PRIVATE).
- Source branch main; original local custom/v0.21.2 branch remains at upstream base.
- origin targets the owner; upstream remains reference-only with its push disabled.
- Inherited Actions disabled at repository level before any source push; original
  workflow files archived unchanged under docs/upstream-workflows (not executable).
- Added delivery/continuity documentation, README notice and local-secret/binary
  ignore rules. No application/Gradle source changes in this slice.
- No JOKESONG files modified; no Claude invocation and no device install.

### Verification and delivery status

- Authenticated owner verified through official GitHub CLI: liuchengzhiaa (keyring).
- Repository creation confirmed; initially empty and PRIVATE.
- Upstream Git integrity check passed before initialization; exact v0.21.2 base
  verified. Full-history git fsck --no-reflogs and git diff --check passed.
- Reviewed upload set: AGENTS.md, three docs, README.md, CHANGELOG.md and .gitignore;
  plus three unchanged upstream workflows moved to docs/upstream-workflows;
  no app/Gradle modifications. Ignore probes cover APK/AAB, signing key,
  local.properties, keystore.properties, .env and local tool/auth cache paths.
- Remote settings rechecked: PRIVATE and actions.enabled=false. First source push
  was rejected because OAuth scope lacks workflow permission. Archived inherited
  workflows outside .github/workflows rather than expanding account permissions.
  Retry succeeded without adding OAuth scopes. GitHub API read-back confirmed
  remote main equals local 1999e94e9b263a7ac256f2fb9b48946ab86fb082.
- Delivery receipt: repository PRIVATE, default branch main, Actions disabled,
  0 workflow runs and 0 Releases verified remotely. This receipt is a docs-only
  follow-up commit; no identical APK rebuild/release is required.
- Android unit tests/lint/build/device checks NOT RUN: documentation/repository setup
  only, not a material application code update. No custom APK exists or is published.
- See docs/RELEASING.md for exact candidate verification/build commands.

### Safest next action

1. Repository initialization is delivered; confirm requested product customization.
2. Establish independent name/icon/package, disabled or replaced updater/telemetry
   and a persistent dedicated signing key.
3. Build/verify the first custom APK, then publish to a new versioned Release.

### Constraints and pending setup

- Private downloads require authorized GitHub sign-in; do not make public without
  explicit owner instruction.
- JDK/SDK/NDK/dependency availability has not been tested for this checkout.
- No custom signer/package/version policy or publishing script has been configured.
- Inherited upstream README/download links and workflows are not custom releases.