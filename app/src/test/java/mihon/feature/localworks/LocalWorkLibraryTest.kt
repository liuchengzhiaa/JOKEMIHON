package mihon.feature.localworks

import eu.kanade.tachiyomi.data.cache.CoverCache
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterRemoteUpdate
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.chapter.service.getChapterSort
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaRemoteUpdate
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.source.local.work.LocalAuthorGroup
import tachiyomi.source.local.work.LocalWorkCatalog
import tachiyomi.source.local.work.LocalWorkCover
import tachiyomi.source.local.work.LocalWorkDefinition
import tachiyomi.source.local.work.LocalWorkPath

class LocalWorkLibraryTest {
    private val mangas = mutableMapOf<Long, Manga>()
    private val chapters = mutableMapOf<Long, List<Chapter>>()
    private val tags = mutableMapOf<Long, List<Long>>()
    private val mangaRepo = mockk<MangaRepository>()
    private val chapterRepo = mockk<ChapterRepository>()
    private val catalog = mockk<LocalWorkCatalog>()
    private val categories = mockk<GetCategories>()
    private val covers = mockk<CoverCache>()
    private val first = LocalWorkPath("423黄金01", "作品D 上.zip")
    private val second = LocalWorkPath("423黄金05", "作品B.zip")
    private val library = LocalWorkLibrary(
        mangaRepo,
        chapterRepo,
        catalog,
        categories,
        covers,
        mockk<LocalWorkFoldersStore>(relaxed = true),
    )
    private var nextManga = 100L
    private var nextChapter = 1000L

    @BeforeEach
    fun setup() {
        every { catalog.requireAvailable(any()) } just Runs
        every { catalog.requireAvailableWorks(any()) } just Runs
        coEvery { categories.await() } returns listOf(Category(1, "one", 0, 0), Category(2, "two", 1, 0))
        coEvery { categories.await(any()) } coAnswers
            { tags[firstArg<Long>()].orEmpty().map { Category(it, "tag", it, 0) } }
        coEvery { mangaRepo.getMangaByUrlAndSourceId(any(), any()) } coAnswers {
            mangas.values.firstOrNull { it.url == firstArg<String>() && it.source == secondArg<Long>() }
        }
        coEvery { mangaRepo.getMangaById(any()) } coAnswers { mangas.getValue(firstArg()) }
        coEvery { mangaRepo.insertNetworkManga(any()) } coAnswers {
            firstArg<List<Manga>>().map { it.copy(id = nextManga++).also { mangas[it.id] = it } }
        }
        coEvery { mangaRepo.setMangaCategories(any(), any()) } coAnswers { tags[firstArg()] = secondArg() }
        coEvery { mangaRepo.update(any()) } coAnswers {
            val update = firstArg<MangaUpdate>()
            val original = mangas.getValue(update.id)
            mangas[update.id] = original.copy(favoriteAt = update.favoriteAt ?: original.favoriteAt)
            true
        }
        coEvery { mangaRepo.updateRemote(any()) } coAnswers {
            val update = firstArg<MangaRemoteUpdate>()
            val original = mangas.getValue(update.id)
            mangas[update.id] = original.copy(
                title = update.title ?: original.title,
                author = update.author,
                thumbnailUrl = update.thumbnailUrl,
                memo = update.memo,
                coverLastModified = update.coverLastModified,
            )
            true
        }
        coEvery { chapterRepo.getChapterByMangaId(any(), any()) } coAnswers { chapters[firstArg()].orEmpty() }
        coEvery { chapterRepo.updateFromRemote(any(), any(), any()) } coAnswers {
            check(firstArg<List<Long>>().isEmpty()) // Sync must never delete old reading rows.
            for (update in thirdArg<List<ChapterRemoteUpdate>>()) {
                for ((id, rows) in chapters.toMap()) {
                    chapters[id] = rows.map {
                        if (it.id == update.id) {
                            it.copy(sourceOrder = update.sourceOrder, chapterNumber = update.chapterNumber)
                        } else {
                            it
                        }
                    }
                }
            }
            secondArg<List<Chapter>>().map {
                it.copy(id = nextChapter++).also { chapter ->
                    chapters[chapter.mangaId] = chapters[chapter.mangaId].orEmpty() + chapter
                }
            }
        }
        every { covers.deleteFromCache(any(), any()) } returns 1
    }

    @Test
    fun `new virtual work inherits legacy state without rewriting legacy entries`() = runBlocking<Unit> {
        val legacy = Manga.create().copy(id = 1, source = 0, url = first.directory, title = "legacy")
        val oldChapter = Chapter.create().copy(
            id = 5,
            mangaId = 1,
            url = first.relativeUrl,
            read = true,
            bookmark = true,
            lastPageRead = 19,
        )
        mangas[1] = legacy
        chapters[1] = listOf(oldChapter)
        val added = library.add(listOf(first), false, first.displayName, "author", listOf(2)).single()
        added.favorite shouldBe true
        chapters.getValue(added.id).single().let { listOf(it.read, it.bookmark, it.lastPageRead) } shouldBe
            listOf(true, true, 19)
        tags[added.id] shouldBe listOf(2L)
        mangas[1] shouldBe legacy
        chapters[1] shouldBe listOf(oldChapter)
        coVerify(exactly = 0) { mangaRepo.update(match { it.id == 1L }) }
    }

    @Test
    fun `readding retains progress and unions categories without duplicates`() = runBlocking<Unit> {
        val added = library.add(listOf(first), false, "ignored", "author", listOf(1)).single()
        val original = chapters.getValue(added.id).single().copy(read = true, lastPageRead = 8)
        chapters[added.id] = listOf(original)
        val again = library.add(listOf(first), false, "ignored", "author", listOf(2)).single()
        again.id shouldBe added.id
        mangas.size shouldBe 1
        chapters[added.id] shouldBe listOf(original)
        tags[added.id] shouldBe listOf(1L, 2L)
    }

    @Test
    fun `separate import retains original filenames and creates distinct identities`() = runBlocking<Unit> {
        val added = library.add(listOf(second, first), false, "ignored", "author", emptyList())
        added.map { it.title } shouldBe listOf(second.displayName, first.displayName)
        added.map { it.url }.distinct().size shouldBe 2
        added.forEach { chapters.getValue(it.id).size shouldBe 1 }
    }

    @Test
    fun `combined chapter order matches click order even when names sort differently`() = runBlocking<Unit> {
        val added = library.add(listOf(second, first), true, "作品B（两部曲）", "author", emptyList()).single()
        val stored = chapters.getValue(added.id)
        stored.sortedWith(getChapterSort(added)).map { it.url } shouldBe listOf(second.relativeUrl, first.relativeUrl)
        LocalWorkDefinition.fromMemo(added.url, added.memo).members shouldBe listOf(second, first)
        LocalWorkCover.fromUrl(added.thumbnailUrl!!).path shouldBe second
    }

    @Test
    fun `deleted category refuses operation before creating entries`() = runBlocking<Unit> {
        runCatching { library.add(listOf(first), false, "title", "author", listOf(999)) }.isFailure shouldBe true
        mangas.isEmpty() shouldBe true
        chapters.isEmpty() shouldBe true
    }

    @Test
    fun `missing source file refuses operation before creating entries`() = runBlocking<Unit> {
        every { catalog.requireAvailableWorks(listOf(first)) } throws IllegalStateException("Missing")
        runCatching { library.add(listOf(first), false, "title", "author", emptyList()) }.isFailure shouldBe true
        mangas.isEmpty() shouldBe true
    }

    @Test
    fun `chapter initialization failure leaves no broken favorite`() = runBlocking<Unit> {
        coEvery { chapterRepo.updateFromRemote(any(), any(), any()) } throws IllegalStateException("DB failure")
        runCatching { library.add(listOf(first), false, "title", "author", emptyList()) }.isFailure shouldBe true
        mangas.values.single().favorite shouldBe false
        coVerify(exactly = 0) { mangaRepo.update(any()) }
    }

    @Test
    fun `page cover remains in allowed members and preserves metadata`() = runBlocking<Unit> {
        val added = library.add(listOf(first, second), true, "two", "author", emptyList()).single()
        library.setPageCover(added.id, second.relativeUrl, 7)
        val updated = mangas.getValue(added.id)
        LocalWorkCover.fromUrl(updated.thumbnailUrl!!) shouldBe LocalWorkCover(second, 7)
        updated.memo shouldBe added.memo
        updated.title shouldBe added.title
        runCatching { library.setPageCover(added.id, "outside/other.zip", 0) }.isFailure shouldBe true
    }

    @Test
    fun `preview creates no favorite and concurrent identical adds are idempotent`() = runBlocking<Unit> {
        val preview = library.open(first, "author")
        preview.favorite shouldBe false
        val one = async { library.add(listOf(first), false, "title", "author", emptyList()).single() }
        val two = async { library.add(listOf(first), false, "title", "author", emptyList()).single() }
        one.await().id shouldBe two.await().id
        mangas.size shouldBe 1
        chapters.getValue(preview.id).size shouldBe 1
    }

    @Test
    fun `readd can explicitly remove a category without clearing other tags or progress`() = runBlocking<Unit> {
        val added = library.add(listOf(first), false, "title", "author", listOf(1, 2)).single()
        val original = chapters.getValue(added.id).single().copy(read = true, lastPageRead = 4)
        chapters[added.id] = listOf(original)
        val again = library.add(listOf(first), false, "title", "author", emptyList(), listOf(1)).single()
        again.id shouldBe added.id
        tags[added.id] shouldBe listOf(2L)
        chapters[added.id] shouldBe listOf(original)
    }

    @Test
    fun `edit categories can remove the last tag without requiring media or changing favorites`() = runBlocking<Unit> {
        val added = library.add(listOf(first), false, "title", "author", listOf(1)).single()
        every { catalog.requireAvailableWorks(any()) } throws IllegalStateException("Media offline")
        library.editCategories(added.id, emptyList(), listOf(1))
        tags[added.id] shouldBe emptyList()
        mangas[added.id] shouldBe added
        coVerify(exactly = 0) { mangaRepo.updateRemote(any()) }
    }

    @Test
    fun `category dialog reads existing tags and represents a missing work as empty`() = runBlocking<Unit> {
        library.add(listOf(first), false, "title", "author", listOf(1))
        library.categoryMembership(listOf(first, second), false) shouldBe listOf(setOf(1L), emptySet())
        library.categoryMembership(listOf(first, second), true) shouldBe listOf(emptySet())
        mangas.size shouldBe 1
    }

    @Test
    fun `author sync creates one independent book and does not mutate old library entries`() = runBlocking<Unit> {
        val original = library.add(listOf(first, second), true, "Existing collection", "author", listOf(1)).single()
        val group = LocalAuthorGroup("author:423:黄", "423黄金", listOf(first.directory, second.directory))
        val added = library.syncAuthor(group, listOf(first, second), 2)
        added.favorite shouldBe true
        added.title shouldBe group.name
        tags[added.id] shouldBe listOf(2L)
        (original.id == added.id) shouldBe false
        mangas[original.id] shouldBe original
        tags[original.id] shouldBe listOf(1L)
        LocalWorkDefinition.fromMemo(added.url, added.memo).authorKey shouldBe group.key
    }

    @Test
    fun `author resync retains identity state and cover while adding chapters`() = runBlocking<Unit> {
        val group = LocalAuthorGroup("author:423:黄", "423黄金", listOf(first.directory, second.directory))
        val added = library.syncAuthor(group, listOf(first), 1)
        val old = chapters.getValue(added.id).single().copy(read = true, bookmark = true, lastPageRead = 17)
        chapters[added.id] = listOf(old)
        library.setPageCover(added.id, first.relativeUrl, 5)
        val cover = mangas.getValue(added.id).thumbnailUrl
        val synced = library.syncAuthor(group.copy(name = "新名称"), listOf(second, first), 2)
        synced.id shouldBe added.id
        synced.title shouldBe "新名称"
        synced.thumbnailUrl shouldBe cover
        tags[added.id] shouldBe listOf(1L, 2L)
        mangas.size shouldBe 1
        val kept = chapters.getValue(added.id).first { it.id == old.id }
        listOf(kept.read, kept.bookmark, kept.lastPageRead) shouldBe listOf(true, true, 17)
        chapters.getValue(added.id).sortedWith(getChapterSort(synced)).map { it.url } shouldBe
            listOf(second.relativeUrl, first.relativeUrl)
        LocalWorkDefinition.fromMemo(synced.url, synced.memo).members shouldBe listOf(second, first)
        val again = library.syncAuthor(group.copy(name = "新名称"), listOf(second, first), 2)
        again shouldBe synced
        chapters.getValue(added.id).size shouldBe 2
        tags[added.id] shouldBe listOf(1L, 2L)
    }

    @Test
    fun `unavailable author or category creates no book`() = runBlocking<Unit> {
        val group = LocalAuthorGroup("author:423:黄", "423黄金", listOf(first.directory))
        runCatching { library.syncAuthor(group, listOf(first), 999) }.isFailure shouldBe true
        runCatching { library.syncAuthor(group, listOf(second), 1) }.isFailure shouldBe true
        mangas.isEmpty() shouldBe true
        every { catalog.requireAvailableWorks(any()) } throws IllegalStateException("Offline")
        runCatching { library.syncAuthor(group, listOf(first), 1) }.isFailure shouldBe true
        mangas.isEmpty() shouldBe true
    }

    @Test
    fun `failed author initialization never publishes a broken favorite`() = runBlocking<Unit> {
        val group = LocalAuthorGroup("author:423:黄", "423黄金", listOf(first.directory))
        coEvery { chapterRepo.updateFromRemote(any(), any(), any()) } throws IllegalStateException("DB failure")
        runCatching { library.syncAuthor(group, listOf(first), 1) }.isFailure shouldBe true
        mangas.values.single().favorite shouldBe false
        tags.isEmpty() shouldBe true
    }
}
