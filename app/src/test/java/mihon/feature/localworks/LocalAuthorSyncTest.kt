package mihon.feature.localworks

import eu.kanade.tachiyomi.data.backup.models.BackupManga
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.category.interactor.CreateCategoryWithName
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.manga.model.Manga
import tachiyomi.source.local.work.LocalAuthorGroup
import tachiyomi.source.local.work.LocalAuthorRules
import tachiyomi.source.local.work.LocalWorkCatalog
import tachiyomi.source.local.work.LocalWorkCover
import tachiyomi.source.local.work.LocalWorkDefinition
import tachiyomi.source.local.work.LocalWorkFolders
import tachiyomi.source.local.work.LocalWorkPath

class LocalAuthorSyncTest {
    private val first = LocalWorkPath("432長い草01", "one.zip")
    private val second = LocalWorkPath("432長い草02", "two.zip")
    private val group = LocalAuthorGroup("author:432:長", "432長い草", listOf(first.directory, second.directory))
    private val other = LocalAuthorGroup("author:423:黄", "423黄金", listOf("423黄金01"))
    private val catalog = mockk<LocalWorkCatalog>()
    private val rules = mockk<LocalAuthorRulesStore>()
    private val library = mockk<LocalWorkLibrary>()
    private val categories = mockk<GetCategories>()
    private val create = mockk<CreateCategoryWithName>()
    private val sync = LocalAuthorSync(catalog, rules, library, categories, create)
    private var tags = listOf(Category(7, LocalAuthorSync.CATEGORY_NAME, 0, 0))

    @BeforeEach
    fun setup() {
        coEvery { catalog.authors() } returns listOf(group)
        every { rules.get() } returns LocalAuthorRules()
        coEvery { catalog.works(any()) } returns listOf(first, second)
        coEvery { categories.await() } answers { tags }
        coEvery { create.await(any()) } answers {
            tags = tags + Category(9, firstArg(), 1, 0)
            CreateCategoryWithName.Result.Success
        }
        coEvery { library.syncAuthor(any(), any(), any()) } returns Manga.create()
    }

    @Test
    fun `stable author identity changes membership without colliding with ordinary work identities`() {
        val original = LocalWorkDefinition(listOf(first), authorKey = group.key)
        original.url shouldBe LocalWorkDefinition(listOf(second, first), authorKey = group.key).url
        (original.url == LocalWorkDefinition(listOf(first)).url) shouldBe false
        (original.url == LocalWorkDefinition(listOf(first), authorKey = other.key).url) shouldBe false
        LocalWorkDefinition.fromMemo(original.url, original.toMemo()) shouldBe original
        runCatching { LocalWorkDefinition(listOf(first), authorKey = "") }.isFailure shouldBe true
    }

    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    @Test
    fun `normal manga backup preserves stable author key definition and cover`() {
        val definition = LocalWorkDefinition(listOf(first, second), authorKey = group.key)
        val cover = LocalWorkCover(second, 4)
        val backup = BackupManga(
            source = 0,
            url = definition.url,
            title = group.name,
            thumbnailUrl = cover.toUrl(),
            memo = Json.encodeToString(JsonObject.serializer(), definition.toMemo()).toByteArray(),
            categories = listOf(7),
        )
        val decoded = ProtoBuf.decodeFromByteArray(
            BackupManga.serializer(),
            ProtoBuf.encodeToByteArray(BackupManga.serializer(), backup),
        )
        val manga = decoded.getMangaImpl()
        LocalWorkDefinition.fromMemo(manga.url, manga.memo) shouldBe definition
        LocalWorkCover.fromUrl(manga.thumbnailUrl!!) shouldBe cover
        decoded.categories shouldBe listOf(7L)
    }

    @Test
    fun `root author books are not adopted as nested work folders`() {
        val pref = mockk<Preference<String>>()
        val prefs = mockk<PreferenceStore>()
        every { prefs.getString(any(), any()) } returns pref
        every { pref.get() } returns ""
        val definition = LocalWorkDefinition(listOf(first, second), authorKey = group.key)
        LocalWorkFoldersStore(prefs).adoptExisting(
            listOf(
                Manga.create().copy(
                    source = 0,
                    url = definition.url,
                    title = group.name,
                    memo = definition.toMemo(),
                ),
            ),
        )
        io.mockk.verify(exactly = 0) { pref.set(any()) }
        LocalWorkFolders.decode("").folders shouldBe emptyList()
    }

    @Test
    fun `sync uses every freshly scanned root author and reuses the existing exact tag`() = runBlocking<Unit> {
        coEvery { catalog.authors() } returns listOf(group, other)
        val progress = mutableListOf<LocalAuthorSyncProgress>()
        sync.sync(progress::add) shouldBe LocalAuthorSyncResult(2, 2, 0, 0, emptyList())
        progress.last() shouldBe LocalAuthorSyncProgress(2, 2)
        coVerify(exactly = 1) { library.syncAuthor(group, listOf(first, second), 7) }
        coVerify(exactly = 1) { library.syncAuthor(other, listOf(first, second), 7) }
        coVerify(exactly = 0) { create.await(any()) }
    }

    @Test
    fun `manual root grouping is applied before importing and tag is created once`() = runBlocking<Unit> {
        tags = emptyList()
        every { rules.get() } returns LocalAuthorRules().save(listOf(group), "自定义名称", "manual-id")
        sync.sync {} shouldBe LocalAuthorSyncResult(1, 1, 0, 0, emptyList())
        sync.sync {} shouldBe LocalAuthorSyncResult(1, 1, 0, 0, emptyList())
        coVerify(exactly = 1) { create.await(LocalAuthorSync.CATEGORY_NAME) }
        coVerify(exactly = 2) {
            library.syncAuthor(match { it.key == "manual:manual-id" && it.name == "自定义名称" }, any(), 9)
        }
    }

    @Test
    fun `empty author does not create an empty category or book`() = runBlocking<Unit> {
        tags = emptyList()
        coEvery { catalog.works(any()) } returns emptyList()
        sync.sync {} shouldBe LocalAuthorSyncResult(1, 0, 1, 0, emptyList())
        coVerify(exactly = 0) { create.await(any()) }
        coVerify(exactly = 0) { library.syncAuthor(any(), any(), any()) }
    }

    @Test
    fun `per author failures do not discard successful directories and failure list is bounded`() = runBlocking<Unit> {
        val failures = (1..8).map { group.copy(key = "bad:$it", name = "bad$it") }
        coEvery { catalog.authors() } returns failures + other
        coEvery { catalog.works(match { it.key.startsWith("bad:") }) } throws IllegalStateException("Offline")
        val result = sync.sync {}
        result shouldBe LocalAuthorSyncResult(9, 1, 0, 8, failures.take(5).map { it.name })
        coVerify(exactly = 1) { library.syncAuthor(other, any(), 7) }
    }

    @Test
    fun `over limit author is reported without tag creation or partial truncation`() = runBlocking<Unit> {
        tags = emptyList()
        coEvery { catalog.works(any()) } returns List(1001) { LocalWorkPath(first.directory, "$it.zip") }
        sync.sync {} shouldBe LocalAuthorSyncResult(1, 0, 0, 1, listOf(group.name))
        coVerify(exactly = 0) { create.await(any()) }
        coVerify(exactly = 0) { library.syncAuthor(any(), any(), any()) }
    }

    @Test
    fun `cancellation is propagated rather than reported as an ordinary failed directory`() = runBlocking<Unit> {
        coEvery { catalog.works(any()) } throws CancellationException("Cancelled")
        runCatching { sync.sync {} }.exceptionOrNull()!!.let { (it is CancellationException) shouldBe true }
        coVerify(exactly = 0) { library.syncAuthor(any(), any(), any()) }
    }
}
