package mihon.feature.localworks

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.data.cache.CoverCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterRemoteUpdate
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaRemoteUpdate
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.source.local.LocalSource
import tachiyomi.source.local.work.LocalAuthorGroup
import tachiyomi.source.local.work.LocalWorkCatalog
import tachiyomi.source.local.work.LocalWorkCover
import tachiyomi.source.local.work.LocalWorkDefinition
import tachiyomi.source.local.work.LocalWorkFolder
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
    private val folders: LocalWorkFoldersStore,
) {
    private val mutation = Mutex()

    suspend fun add(
        works: List<LocalWorkPath>,
        combined: Boolean,
        title: String,
        author: String,
        categoryIds: List<Long>,
        removeCategoryIds: List<Long> = emptyList(),
        removeOriginals: Boolean = false,
        cover: LocalWorkCover? = null,
    ): List<Manga> = withContext(Dispatchers.IO) {
        mutation.withLock {
            LocalWorkDefinition(works) // Validate count, uniqueness and paths before any mutation.
            catalog.requireAvailableWorks(works)
            val validIds = getCategories.await().map { it.id }.toSet()
            require(categoryIds.all { it == 0L || it in validIds }) { "Category no longer exists" }
            require(!removeOriginals || (combined && works.size > 1))
            require(cover == null || (combined && cover.path in works))
            // Resolve coverage before adding anything. Partial originals must remain visible.
            val originals = if (removeOriginals) coveredOriginals(works) else emptyList()
            val progress = inheritedProgress(works)
            val definitions = if (combined) {
                listOf(LocalWorkDefinition(works))
            } else {
                works.map {
                    LocalWorkDefinition(listOf(it))
                }
            }
            val added = definitions.map { definition ->
                val name = if (combined) title else definition.members.first().displayName
                val stored = existingTarget(definition, allowLegacySingle = !combined)
                val manga = stored?.takeIf { !LocalWorkDefinition.isWorkUrl(it.url) }
                    ?: prepare(definition, name, author, progress, cover)
                if (combined && cover != null && manga.thumbnailUrl != cover.toUrl()) updateCover(manga, cover)
                // Only explicitly toggled tags are changed; mixed/untouched batch tags remain intact.
                val previous = getCategories.await(manga.id).map { it.id }.filter { it > 0 }
                mangaRepository.setMangaCategories(
                    manga.id,
                    (previous + categoryIds.filter { it > 0 }).distinct().filterNot { it in removeCategoryIds },
                )
                check(
                    mangaRepository.update(
                        MangaUpdate(manga.id) {
                            if (!manga.favorite) favoriteAt = Clock.System.now()
                        },
                    ),
                ) { "Could not add local work to library" }
                mangaRepository.getMangaById(manga.id)
            }
            // Only unfavorite after the new entry and its categories are saved successfully.
            // Keep original rows, chapters, history, categories and all physical files untouched.
            val addedIds = added.mapTo(HashSet()) { it.id }
            val removals = originals.filterNot { it in addedIds }.map { id ->
                MangaUpdate(id) { favoriteAt = null }
            }
            if (removals.isNotEmpty()) {
                check(mangaRepository.updateAll(removals)) { "Could not remove original entries from library" }
            }
            added
        }
    }

    /* Sync one root author to an existing tag. Inputs are the live group and its complete ordered works;
       returns the independent author manga. Other favorites, tags, covers and reading state are retained. */
    suspend fun syncAuthor(author: LocalAuthorGroup, works: List<LocalWorkPath>, categoryId: Long): Manga =
        withContext(Dispatchers.IO) {
            mutation.withLock {
                val definition = LocalWorkDefinition(works, authorKey = author.key)
                require(works.all { it.directory in author.directories })
                catalog.requireAvailableWorks(works)
                require(categoryId > 0 && getCategories.await().any { it.id == categoryId }) {
                    "Category no longer exists"
                }
                val prepared = prepare(definition, author.name, author.name, inheritedProgress(works))
                val previous = getCategories.await(prepared.id).map { it.id }.filter { it > 0 }
                if (categoryId !in previous) {
                    mangaRepository.setMangaCategories(prepared.id, previous + categoryId)
                }
                if (!prepared.favorite) {
                    check(mangaRepository.update(MangaUpdate(prepared.id) { favoriteAt = Clock.System.now() })) {
                        "Could not add author directory to library"
                    }
                }
                mangaRepository.getMangaById(prepared.id)
            }
        }

    private suspend fun coveredOriginals(works: List<LocalWorkPath>): List<Long> {
        val selected = works.toHashSet()
        val selectedUrls = works.mapTo(HashSet()) { it.relativeUrl }
        val directories = works.mapTo(HashSet()) { it.directory }
        val originals = mutableListOf<Long>()
        for (manga in mangaRepository.getFavoritesBySourceId(LocalSource.ID).first()) {
            if (LocalWorkDefinition.isWorkUrl(manga.url)) {
                // Invalid definitions cannot establish full coverage: leave them in the library.
                val definition = runCatching {
                    LocalWorkDefinition.fromMemo(manga.url, manga.memo)
                }.getOrNull() ?: continue
                if (definition.members.all { it in selected }) originals.add(manga.id)
            } else if (manga.url in directories) {
                // Use live contents as well as stored chapters, never just the selected folder name.
                val contents = catalog.works(LocalAuthorGroup(manga.url, manga.title, listOf(manga.url)))
                val chapters = chapterRepository.getChapterByMangaId(manga.id)
                if (contents.isNotEmpty() && contents.all { it in selected } &&
                    chapters.all { it.url in selectedUrls }
                ) {
                    originals.add(manga.id)
                }
            }
        }
        return originals
    }

    suspend fun categoryMembership(works: List<LocalWorkPath>, combined: Boolean): List<Set<Long>> =
        withContext(Dispatchers.IO) {
            val definitions = if (combined) {
                listOf(LocalWorkDefinition(works))
            } else {
                works.map {
                    LocalWorkDefinition(listOf(it))
                }
            }
            definitions.map { definition ->
                val manga = existingTarget(definition, allowLegacySingle = !combined)
                if (manga == null) emptySet() else getCategories.await(manga.id).map { it.id }.filter { it > 0 }.toSet()
            }
        }

    suspend fun categoryMembershipForId(mangaId: Long): List<Set<Long>> = withContext(Dispatchers.IO) {
        val manga = mangaRepository.getMangaById(mangaId)
        requireLocalIdentity(manga)
        listOf(getCategories.await(manga.id).map { it.id }.filter { it > 0 }.toSet())
    }

    private fun requireLocalIdentity(manga: Manga) {
        require(manga.source == LocalSource.ID)
        if (LocalWorkDefinition.isWorkUrl(manga.url)) {
            LocalWorkDefinition.fromMemo(manga.url, manga.memo)
        } else {
            LocalWorkPath(manga.url, "validation") // A real local parent name, never an arbitrary path/URL.
        }
    }

    // Read-only compatibility mapping from a completed author scan; never infer identity from titles.
    suspend fun legacySingleAliases(
        works: List<LocalWorkPath>,
        favorites: Collection<Manga>,
    ): Map<String, String> = withContext(Dispatchers.IO) {
        val legacy = favorites.filterNot { LocalWorkDefinition.isWorkUrl(it.url) }.associateBy { it.url }
        buildMap {
            for ((directory, members) in works.groupBy { it.directory }) {
                val path = members.singleOrNull() ?: continue
                val manga = legacy[directory] ?: continue
                if (chapterRepository.getChapterByMangaId(manga.id).all { it.url == path.relativeUrl }) {
                    put(LocalWorkDefinition(listOf(path)).url, manga.url)
                }
            }
        }
    }

    private suspend fun existingTarget(definition: LocalWorkDefinition, allowLegacySingle: Boolean): Manga? {
        val exact = mangaRepository.getMangaByUrlAndSourceId(definition.url, LocalSource.ID)
        if (exact?.favorite == true || !allowLegacySingle) return exact
        val path = definition.members.singleOrNull() ?: return exact
        val legacy = mangaRepository.getMangaByUrlAndSourceId(path.directory, LocalSource.ID)
            ?.takeIf { it.favorite } ?: return exact
        val contents = catalog.works(LocalAuthorGroup(path.directory, path.directory, listOf(path.directory)))
        if (contents != listOf(path)) return exact
        if (chapterRepository.getChapterByMangaId(legacy.id).any { it.url != path.relativeUrl }) return exact
        return legacy
    }

    suspend fun editCategories(mangaId: Long, add: List<Long>, remove: List<Long>) = withContext(Dispatchers.IO) {
        mutation.withLock {
            val manga = mangaRepository.getMangaById(mangaId)
            requireLocalIdentity(manga)
            val valid = getCategories.await().map { it.id }.toSet()
            require(add.all { it > 0 && it in valid })
            val previous = getCategories.await(manga.id).map { it.id }.filter { it > 0 }
            mangaRepository.setMangaCategories(manga.id, (previous + add).distinct().filterNot { it in remove })
        }
    }

    suspend fun open(work: LocalWorkPath, author: String): Manga = withContext(Dispatchers.IO) {
        mutation.withLock {
            catalog.requireAvailable(work)
            val definition = LocalWorkDefinition(listOf(work))
            val existing = existingTarget(definition, allowLegacySingle = true)
            existing?.takeIf { !LocalWorkDefinition.isWorkUrl(it.url) }
                ?: prepare(definition, work.displayName, author, inheritedProgress(listOf(work)))
        }
    }

    suspend fun openFolder(folder: LocalWorkFolder, author: String): Manga = withContext(Dispatchers.IO) {
        mutation.withLock {
            catalog.requireAvailableWorks(folder.members)
            val manga = prepare(
                LocalWorkDefinition(folder.members),
                folder.name,
                author,
                inheritedProgress(folder.members),
                folder.cover,
            )
            val cover = folder.cover
            if (cover != null && manga.thumbnailUrl != folder.coverUrl) updateCover(manga, cover)
            mangaRepository.getMangaById(manga.id)
        }
    }

    suspend fun setFolderCover(folder: LocalWorkFolder, cover: LocalWorkCover) = withContext(Dispatchers.IO) {
        mutation.withLock {
            require(cover.path in folder.members)
            catalog.requireAvailable(cover.path)
            mangaRepository.getMangaByUrlAndSourceId(folder.url, LocalSource.ID)?.let { updateCover(it, cover) }
            folders.setCover(folder.url, cover)
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
        cover: LocalWorkCover? = null,
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
                    thumbnailUrl = (cover ?: LocalWorkCover(definition.members.first())).toUrl(),
                    initialized = true,
                    chapterFlags = Manga.CHAPTER_SORT_ASC or Manga.CHAPTER_SORTING_SOURCE,
                ),
            ),
        ).single()
        LocalWorkDefinition.fromMemo(manga.url, manga.memo)
        val storedChapters = chapterRepository.getChapterByMangaId(manga.id)
        val stored = storedChapters.mapTo(HashSet()) { it.url }
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
        val reordered = if (definition.authorKey != null) {
            val positions = definition.members.mapIndexed { index, path -> path.relativeUrl to index }.toMap()
            storedChapters.mapNotNull { chapter ->
                val index = positions[chapter.url] ?: return@mapNotNull null
                val order = (definition.members.lastIndex - index).toLong()
                val number = (index + 1).toDouble()
                if (chapter.sourceOrder == order && chapter.chapterNumber == number) return@mapNotNull null
                ChapterRemoteUpdate(
                    chapter.id,
                    chapter.name,
                    chapter.scanlator,
                    number,
                    chapter.dateUpload,
                    order,
                    chapter.memo,
                )
            }
        } else {
            emptyList()
        }
        if (missing.isNotEmpty() || reordered.isNotEmpty()) {
            // Never delete old chapters or overwrite read/bookmark/page fields during this additive sync.
            chapterRepository.updateFromRemote(emptyList(), missing, reordered)
        }
        if (definition.authorKey != null) {
            val memo = definition.toMemo(manga.memo)
            if (memo != manga.memo || manga.title != title || manga.author != author) {
                check(
                    mangaRepository.updateRemote(
                        MangaRemoteUpdate(
                            id = manga.id,
                            title = title,
                            author = author,
                            artist = manga.artist,
                            description = manga.description,
                            genre = manga.genre,
                            status = manga.status,
                            thumbnailUrl = manga.thumbnailUrl,
                            updateStrategy = manga.updateStrategy,
                            memo = memo,
                            initialized = manga.initialized,
                            coverLastModified = manga.coverLastModified,
                        ),
                    ),
                ) { "Could not update author directory" }
                return mangaRepository.getMangaById(manga.id)
            }
        }
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
            updateCover(manga, cover)
            folders.setCover(definition.url, cover)
        }
    }

    private suspend fun updateCover(manga: Manga, cover: LocalWorkCover) {
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
