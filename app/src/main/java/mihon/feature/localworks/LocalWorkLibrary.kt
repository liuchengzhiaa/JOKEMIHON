package mihon.feature.localworks

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.data.cache.CoverCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaRemoteUpdate
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.source.local.LocalSource
import tachiyomi.source.local.work.LocalWorkCatalog
import tachiyomi.source.local.work.LocalWorkCover
import tachiyomi.source.local.work.LocalWorkDefinition
import tachiyomi.source.local.work.LocalWorkPath
import kotlin.time.Clock

/** JOKEMIHON: portable definitions live in the existing backed-up manga memo, never beside user media. */
@Inject
@SingleIn(AppScope::class)
class LocalWorkLibrary(
    private val mangaRepository: MangaRepository,
    private val chapterRepository: ChapterRepository,
    private val catalog: LocalWorkCatalog,
    private val getCategories: GetCategories,
    private val coverCache: CoverCache,
) {
    private val mutation = Mutex()

    suspend fun add(
        works: List<LocalWorkPath>,
        combined: Boolean,
        title: String,
        author: String,
        categoryIds: List<Long>,
    ): List<Manga> = withContext(Dispatchers.IO) {
        mutation.withLock {
            LocalWorkDefinition(works) // Validate count, uniqueness and paths before any mutation.
            catalog.requireAvailableWorks(works)
            val validIds = getCategories.await().map { it.id }.toSet()
            require(categoryIds.all { it == 0L || it in validIds }) { "Category no longer exists" }
            val progress = inheritedProgress(works)
            val definitions = if (combined) {
                listOf(LocalWorkDefinition(works))
            } else {
                works.map {
                    LocalWorkDefinition(listOf(it))
                }
            }
            definitions.map { definition ->
                val name = if (combined) title else definition.members.first().displayName
                val manga = prepare(definition, name, author, progress)
                // Add tags, never silently remove existing tags when an existing work is added again.
                val previous = getCategories.await(manga.id).map { it.id }.filter { it > 0 }
                mangaRepository.setMangaCategories(manga.id, (previous + categoryIds.filter { it > 0 }).distinct())
                check(
                    mangaRepository.update(
                        MangaUpdate(manga.id) {
                            if (!manga.favorite) favoriteAt = Clock.System.now()
                        },
                    ),
                ) { "Could not add local work to library" }
                mangaRepository.getMangaById(manga.id)
            }
        }
    }

    suspend fun open(work: LocalWorkPath, author: String): Manga = withContext(Dispatchers.IO) {
        mutation.withLock {
            catalog.requireAvailable(work)
            prepare(LocalWorkDefinition(listOf(work)), work.displayName, author, inheritedProgress(listOf(work)))
        }
    }

    // Import legacy progress once for newly created chapters. Old manga/chapter/history rows are never rewritten.
    private suspend fun inheritedProgress(works: List<LocalWorkPath>): Map<String, Chapter> {
        val wanted = works.mapTo(HashSet()) { it.relativeUrl }
        val result = HashMap<String, Chapter>()
        for (directory in works.map { it.directory }.distinct()) {
            val legacy = mangaRepository.getMangaByUrlAndSourceId(directory, LocalSource.ID) ?: continue
            chapterRepository.getChapterByMangaId(legacy.id).filter { it.url in wanted }.forEach { result[it.url] = it }
        }
        return result
    }

    private suspend fun prepare(
        definition: LocalWorkDefinition,
        title: String,
        author: String,
        inherited: Map<String, Chapter>,
    ): Manga {
        require(title.isNotBlank() && title.length <= 1024)
        val existing = mangaRepository.getMangaByUrlAndSourceId(definition.url, LocalSource.ID)
        val manga = existing ?: mangaRepository.insertNetworkManga(
            listOf(
                Manga.create().copy(
                    url = definition.url,
                    source = LocalSource.ID,
                    title = title,
                    author = author,
                    memo = definition.toMemo(),
                    thumbnailUrl = LocalWorkCover(definition.members.first()).toUrl(),
                    initialized = true,
                    chapterFlags = Manga.CHAPTER_SORT_ASC or Manga.CHAPTER_SORTING_SOURCE,
                ),
            ),
        ).single()
        LocalWorkDefinition.fromMemo(manga.url, manga.memo)
        val stored = chapterRepository.getChapterByMangaId(manga.id).mapTo(HashSet()) { it.url }
        val missing = definition.members.mapIndexedNotNull { index, path ->
            if (path.relativeUrl in stored) return@mapIndexedNotNull null
            val old = inherited[path.relativeUrl]
            Chapter.create().copy(
                mangaId = manga.id,
                url = path.relativeUrl,
                name = path.displayName,
                chapterNumber = (index + 1).toDouble(),
                sourceOrder = (definition.members.lastIndex - index).toLong(),
                dateFetch = Clock.System.now(),
                read = old?.read ?: false,
                bookmark = old?.bookmark ?: false,
                lastPageRead = old?.lastPageRead ?: 0,
            )
        }
        if (missing.isNotEmpty()) chapterRepository.updateFromRemote(emptyList(), missing, emptyList())
        return manga
    }

    suspend fun setPageCover(mangaId: Long, chapterUrl: String, page: Int) = withContext(Dispatchers.IO) {
        mutation.withLock {
            val manga = mangaRepository.getMangaById(mangaId)
            require(manga.source == LocalSource.ID)
            val definition = LocalWorkDefinition.fromMemo(manga.url, manga.memo)
            val path = LocalWorkPath.fromRelativeUrl(chapterUrl)
            require(path in definition.members)
            val cover = LocalWorkCover(path, page)
            check(
                mangaRepository.updateRemote(
                    MangaRemoteUpdate(
                        id = manga.id,
                        title = null,
                        author = manga.author,
                        artist = manga.artist,
                        description = manga.description,
                        genre = manga.genre,
                        status = manga.status,
                        thumbnailUrl = cover.toUrl(),
                        updateStrategy = manga.updateStrategy,
                        memo = manga.memo,
                        initialized = manga.initialized,
                        coverLastModified = Clock.System.now(),
                    ),
                ),
            ) { "Could not save local work cover" }
            coverCache.deleteFromCache(manga, true)
        }
    }
}
