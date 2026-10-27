package mihon.feature.localworks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.tachiyomi.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.source.local.LocalSource
import tachiyomi.source.local.work.LocalAuthorGroup
import tachiyomi.source.local.work.LocalWorkCatalog
import tachiyomi.source.local.work.LocalWorkCover
import tachiyomi.source.local.work.LocalWorkDefinition
import tachiyomi.source.local.work.LocalWorkFolder
import tachiyomi.source.local.work.LocalWorkPath

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class LocalWorksViewModel(
    private val catalog: LocalWorkCatalog,
    private val library: LocalWorkLibrary,
    private val authorRules: LocalAuthorRulesStore,
    private val workFolders: LocalWorkFoldersStore,
    private val authorSync: LocalAuthorSync,
    mangaRepository: MangaRepository,
    getCategories: GetCategories,
) : ViewModel() {
    private val mutableState = MutableStateFlow(LocalWorksState())
    val state = mutableState.asStateFlow()
    private val eventChannel = Channel<LocalWorksEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()
    private var loadingJob: Job? = null
    private var aliasJob: Job? = null
    private var syncJob: Job? = null
    private var loadVersion = 0
    private var automaticAuthors: List<LocalAuthorGroup> = emptyList()

    init {
        viewModelScope.launch {
            authorRules.changes.collect {
                try {
                    val automatic = automaticAuthors
                    val groups = withContext(Dispatchers.Default) { authorRules.get().applyTo(automatic) }
                    // Do not let an older initial preference emission overwrite a completed root scan.
                    if (automatic === automaticAuthors) mutableState.update { it.copy(authors = groups) }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    logcat(LogPriority.ERROR, e)
                    mutableState.update { it.copy(error = true) }
                }
            }
        }
        viewModelScope.launch {
            getCategories.subscribe().collect { categories ->
                mutableState.update { it.copy(categories = categories.filterNot(Category::isSystemCategory)) }
            }
        }
        viewModelScope.launch {
            workFolders.changes.collect {
                try {
                    val folders = withContext(Dispatchers.IO) { workFolders.get().folders }
                    mutableState.update { current ->
                        current.copy(
                            folders = folders,
                            folderId = current.folderId?.takeIf { id -> folders.any { it.id == id } },
                        )
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    logcat(LogPriority.ERROR, e)
                    mutableState.update { it.copy(error = true) }
                }
            }
        }
        viewModelScope.launch {
            mangaRepository.getLibraryMangaAsFlow().conflate().collect { entries ->
                val local = entries.filter { it.manga.source == LocalSource.ID }
                try {
                    withContext(Dispatchers.IO) { workFolders.adoptExisting(local.map { it.manga }) }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    logcat(LogPriority.ERROR, e)
                    eventChannel.send(LocalWorksEvent.Message(R.string.local_works_operation_failed))
                }
                val indexed = withContext(Dispatchers.Default) {
                    val members = mutableMapOf<LocalWorkPath, MutableList<Manga>>()
                    val taggedDirectories = mutableSetOf<String>()
                    for (entry in local) {
                        val manga = entry.manga
                        val named = entry.categories.any { it > 0 }
                        if (LocalWorkDefinition.isWorkUrl(manga.url)) {
                            val definition = runCatching {
                                LocalWorkDefinition.fromMemo(manga.url, manga.memo)
                            }.getOrNull() ?: continue
                            for (path in definition.members) {
                                members.getOrPut(path) { mutableListOf() }.add(manga)
                                if (named) taggedDirectories.add(path.directory)
                            }
                        } else if (named) {
                            taggedDirectories.add(manga.url)
                        }
                    }
                    members.mapValues { it.value.toList() } to taggedDirectories.toSet()
                }
                mutableState.update {
                    it.copy(
                        memberFavorites = indexed.first,
                        taggedDirectories = indexed.second,
                        favorites = local.associate { entry -> entry.manga.url to entry.manga },
                        taggedUrls = local.filter { entry -> entry.categories.any { id -> id > 0 } }
                            .mapTo(HashSet()) { entry -> entry.manga.url },
                        tagsByUrl = local.associate { entry ->
                            entry.manga.url to entry.categories.filter { id -> id > 0 }
                        },
                    )
                }
                refreshAliases()
            }
        }
        refresh()
    }

    fun syncAuthors() {
        val snapshot = state.value
        if (snapshot.saving || snapshot.loading || snapshot.author != null || snapshot.error) return
        mutableState.update { it.copy(saving = true, syncProgress = LocalAuthorSyncProgress(0, 0)) }
        syncJob = viewModelScope.launch {
            try {
                val result = authorSync.sync { progress -> mutableState.update { it.copy(syncProgress = progress) } }
                eventChannel.send(LocalWorksEvent.SyncFinished(result))
            } catch (e: Exception) {
                if (e is CancellationException) {
                    eventChannel.trySend(LocalWorksEvent.Message(R.string.local_works_sync_cancelled))
                    throw e
                }
                logcat(LogPriority.ERROR, e)
                eventChannel.send(LocalWorksEvent.Message(R.string.local_works_operation_failed))
            } finally {
                mutableState.update { it.copy(saving = false, syncProgress = null) }
            }
        }
    }

    fun cancelSync() {
        syncJob?.cancel()
    }

    fun refresh() {
        if (state.value.saving) return
        loadVersion++
        val version = loadVersion
        loadingJob?.cancel()
        val author = state.value.author
        mutableState.update {
            it.copy(
                loading = true,
                error = false,
                selected = emptyList(),
                selectedAuthors = emptyList(),
                preview = null,
            )
        }
        loadingJob = viewModelScope.launch {
            try {
                if (author == null) {
                    val automatic = catalog.authors()
                    val authors = withContext(Dispatchers.Default) { authorRules.get().applyTo(automatic) }
                    if (version == loadVersion) {
                        automaticAuthors = automatic
                        mutableState.update { it.copy(authors = authors) }
                    }
                } else {
                    val works = withContext(Dispatchers.Default) {
                        catalog.works(author).map { path ->
                            val definition = LocalWorkDefinition(listOf(path))
                            LocalWorkItem(
                                path,
                                Manga.create().copy(
                                    url = definition.url,
                                    source = LocalSource.ID,
                                    title = path.displayName,
                                    thumbnailUrl = LocalWorkCover(path).toUrl(),
                                    author = author.name,
                                ),
                            )
                        }
                    }
                    if (version == loadVersion) {
                        mutableState.update { it.copy(works = works) }
                        refreshAliases()
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logcat(LogPriority.ERROR, e)
                if (version == loadVersion) mutableState.update { it.copy(error = true) }
            } finally {
                if (version == loadVersion) mutableState.update { it.copy(loading = false) }
            }
        }
    }

    private fun refreshAliases() {
        aliasJob?.cancel()
        val snapshot = state.value
        if (snapshot.works.isEmpty()) {
            mutableState.update { it.copy(legacySingleAliases = emptyMap()) }
            return
        }
        aliasJob = viewModelScope.launch {
            try {
                val aliases = library.legacySingleAliases(snapshot.works.map { it.path }, snapshot.favorites.values)
                mutableState.update {
                    if (it.works === snapshot.works && it.favorites === snapshot.favorites) {
                        it.copy(legacySingleAliases = aliases)
                    } else {
                        it
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logcat(LogPriority.ERROR, e)
                eventChannel.send(LocalWorksEvent.Message(R.string.local_works_operation_failed))
            }
        }
    }

    fun enter(author: LocalAuthorGroup) {
        if (state.value.loading || state.value.saving) return
        mutableState.update { it.copy(author = author, folderId = null, works = emptyList(), selected = emptyList()) }
        refresh()
    }

    fun back(): Boolean {
        if (state.value.saving) return true
        if (state.value.selected.isNotEmpty() || state.value.selectedAuthors.isNotEmpty()) {
            clearSelection()
            return true
        }
        if (state.value.folderId != null) {
            mutableState.update { it.copy(folderId = null, preview = null) }
            return true
        }
        if (state.value.author != null) {
            loadVersion++
            loadingJob?.cancel()
            mutableState.update {
                it.copy(
                    author = null,
                    folderId = null,
                    works = emptyList(),
                    selected = emptyList(),
                    preview = null,
                    loading = false,
                    error = false,
                )
            }
            return true
        }
        return false
    }

    fun enterFolder(folder: LocalWorkFolder) {
        if (state.value.saving || state.value.selected.isNotEmpty()) return
        mutableState.update { it.copy(folderId = folder.id, preview = null, selected = emptyList()) }
    }

    fun createFolder(ordered: List<LocalWorkPath>, title: String) {
        if (state.value.saving || state.value.folderId != null) return
        mutableState.update { it.copy(saving = true, preview = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { workFolders.create(ordered, title) }
                clearSelection()
                eventChannel.send(LocalWorksEvent.Message(R.string.local_works_folder_created))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logcat(LogPriority.ERROR, e)
                eventChannel.send(LocalWorksEvent.Message(R.string.local_works_operation_failed))
            } finally {
                mutableState.update { it.copy(saving = false) }
            }
        }
    }

    fun setFolderCover(path: LocalWorkPath) {
        val folder = state.value.folder ?: return
        if (state.value.saving) return
        mutableState.update { it.copy(saving = true, preview = null) }
        viewModelScope.launch {
            try {
                library.setFolderCover(folder, LocalWorkCover(path))
                eventChannel.send(LocalWorksEvent.Message(R.string.local_works_folder_cover_saved))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logcat(LogPriority.ERROR, e)
                eventChannel.send(LocalWorksEvent.Message(R.string.local_works_operation_failed))
            } finally {
                mutableState.update { it.copy(saving = false) }
            }
        }
    }

    fun toggleAuthor(author: LocalAuthorGroup) {
        if (state.value.saving || state.value.loading) return
        mutableState.update { current ->
            val selected = current.selectedAuthors
            current.copy(selectedAuthors = if (author in selected) selected - author else selected + author)
        }
    }

    fun saveAuthorGroup(name: String) {
        val selected = state.value.selectedAuthors
        if (selected.isEmpty() || state.value.saving) return
        updateRules { authorRules.save(selected, name) }
    }

    fun dissolveAuthorGroups() {
        val keys = state.value.selectedAuthors.map { it.key }.toSet()
        if (keys.isEmpty() || state.value.saving) return
        updateRules { authorRules.dissolve(keys) }
    }

    private fun updateRules(action: () -> Unit) {
        mutableState.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { action() }
                clearSelection()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logcat(LogPriority.ERROR, e)
                eventChannel.send(LocalWorksEvent.Message(R.string.local_works_operation_failed))
            } finally {
                mutableState.update { it.copy(saving = false) }
            }
        }
    }

    suspend fun categoryMembershipForId(mangaId: Long): List<Set<Long>> = library.categoryMembershipForId(mangaId)

    suspend fun categoryMembership(works: List<LocalWorkPath>, combined: Boolean): List<Set<Long>> =
        library.categoryMembership(works, combined)

    fun editCategories(mangaId: Long, add: List<Long>, remove: List<Long>) {
        if (state.value.saving) return
        mutableState.update { it.copy(saving = true, preview = null) }
        viewModelScope.launch {
            try {
                library.editCategories(mangaId, add, remove)
                eventChannel.send(LocalWorksEvent.Message(R.string.local_works_categories_saved))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logcat(LogPriority.ERROR, e)
                eventChannel.send(LocalWorksEvent.Message(R.string.local_works_operation_failed))
            } finally {
                mutableState.update { it.copy(saving = false) }
            }
        }
    }

    fun toggle(path: LocalWorkPath) {
        if (state.value.saving) return
        if (path !in state.value.selected && state.value.selected.size >= LocalWorkDefinition.MAX_MEMBERS) {
            viewModelScope.launch { eventChannel.send(LocalWorksEvent.Message(R.string.local_works_selection_limit)) }
            return
        }
        mutableState.update { it.copy(selected = toggleLocalWorkSelection(it.selected, path)) }
    }

    fun selectAll(visible: List<LocalWorkItem>) {
        val works = visible.filter { it.folder == null }
        if (works.size > LocalWorkDefinition.MAX_MEMBERS) {
            viewModelScope.launch { eventChannel.send(LocalWorksEvent.Message(R.string.local_works_selection_limit)) }
        } else {
            mutableState.update { it.copy(selected = works.map(LocalWorkItem::path)) }
        }
    }

    fun clearSelection() {
        mutableState.update { it.copy(selected = emptyList(), selectedAuthors = emptyList()) }
    }
    fun preview(item: LocalWorkItem?) {
        mutableState.update { it.copy(preview = item) }
    }

    fun open(item: LocalWorkItem) {
        val snapshot = state.value
        if (snapshot.saving) return
        mutableState.update { it.copy(saving = true, preview = null) }
        viewModelScope.launch {
            try {
                val existing = snapshot.favoriteFor(item)
                val author = snapshot.author?.name.orEmpty()
                val manga = existing ?: item.folder?.let { library.openFolder(it, author) }
                    ?: library.open(item.path, author)
                eventChannel.send(LocalWorksEvent.Open(manga.id))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logcat(LogPriority.ERROR, e)
                eventChannel.send(LocalWorksEvent.Message(R.string.local_works_operation_failed))
            } finally {
                mutableState.update { it.copy(saving = false) }
            }
        }
    }

    fun add(
        ordered: List<LocalWorkPath>,
        combined: Boolean,
        title: String,
        categories: List<Long>,
        removeCategories: List<Long>,
        removeOriginals: Boolean,
        folder: LocalWorkFolder? = null,
    ) {
        if (state.value.saving) return
        val author = state.value.author?.name.orEmpty()
        mutableState.update { it.copy(saving = true, preview = null) }
        viewModelScope.launch {
            try {
                library.add(
                    ordered,
                    combined,
                    title,
                    author,
                    categories,
                    removeCategories,
                    removeOriginals,
                    folder?.cover,
                )
                mutableState.update { it.copy(selected = emptyList()) }
                eventChannel.send(LocalWorksEvent.Message(R.string.local_works_added))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logcat(LogPriority.ERROR, e)
                eventChannel.send(LocalWorksEvent.Message(R.string.local_works_operation_failed))
            } finally {
                mutableState.update { it.copy(saving = false) }
            }
        }
    }
}

data class LocalWorkItem(val path: LocalWorkPath, val preview: Manga, val folder: LocalWorkFolder? = null) {
    val title get() = folder?.name ?: path.displayName
    val key get() = folder?.let { "folder:${it.id}" } ?: path.relativeUrl
}

fun LocalWorkFolder.toItem(author: String): LocalWorkItem = LocalWorkItem(
    members.first(),
    Manga.create().copy(
        url = url,
        source = LocalSource.ID,
        title = name,
        thumbnailUrl = coverUrl,
        author = author,
    ),
    this,
)

data class LocalWorkTagContext(val manga: Manga, val names: String)

fun LocalWorksState.favoriteFor(item: LocalWorkItem): Manga? = favorites[item.preview.url]
    ?: if (item.folder == null) legacySingleAliases[item.preview.url]?.let { favorites[it] } else null

fun LocalWorksState.tagContexts(item: LocalWorkItem): List<LocalWorkTagContext> {
    val own = favoriteFor(item)
    val paths = item.folder?.members ?: listOf(item.path)
    return tagContextsForPaths(paths, setOfNotNull(own?.id))
}

fun LocalWorksState.tagContextsForPaths(
    paths: List<LocalWorkPath>,
    excludedIds: Set<Long> = emptySet(),
): List<LocalWorkTagContext> {
    val related = paths.flatMap { path ->
        listOfNotNull(favorites[path.directory]) + memberFavorites[path].orEmpty()
    }.distinctBy { it.id }.filterNot { it.id in excludedIds }
    val names = categories.associate { it.id to it.name }
    return related.map { manga ->
        LocalWorkTagContext(manga, tagsByUrl[manga.url].orEmpty().mapNotNull { names[it] }.joinToString("、"))
    }
}

fun LocalWorksState.managerItems(): List<LocalWorkItem> {
    val author = author ?: return emptyList()
    val folder = folder
    if (folder != null) {
        val byPath = works.associateBy { it.path }
        return folder.members.map { path ->
            byPath[path] ?: LocalWorkItem(
                path,
                Manga.create().copy(
                    url = LocalWorkDefinition(listOf(path)).url,
                    source = LocalSource.ID,
                    title = path.displayName,
                    thumbnailUrl = LocalWorkCover(path).toUrl(),
                    author = author.name,
                ),
            )
        }
    }
    // Anchor by original physical directory, not mutable logical author name/key.
    val directories = author.directories.toHashSet()
    val visibleFolders = folders.filter { it.members.first().directory in directories }
    val assigned = visibleFolders.flatMapTo(HashSet()) { it.members }
    return visibleFolders.map { it.toItem(author.name) } + works.filterNot { it.path in assigned }
}
data class LocalWorksState(
    val authors: List<LocalAuthorGroup> = emptyList(),
    val selectedAuthors: List<LocalAuthorGroup> = emptyList(),
    val author: LocalAuthorGroup? = null,
    val works: List<LocalWorkItem> = emptyList(),
    val folders: List<LocalWorkFolder> = emptyList(),
    val folderId: String? = null,
    val selected: List<LocalWorkPath> = emptyList(),
    val preview: LocalWorkItem? = null,
    val categories: List<Category> = emptyList(),
    val favorites: Map<String, Manga> = emptyMap(),
    val taggedUrls: Set<String> = emptySet(),
    val taggedDirectories: Set<String> = emptySet(),
    val memberFavorites: Map<LocalWorkPath, List<Manga>> = emptyMap(),
    val legacySingleAliases: Map<String, String> = emptyMap(),
    val tagsByUrl: Map<String, List<Long>> = emptyMap(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val syncProgress: LocalAuthorSyncProgress? = null,
    val error: Boolean = false,
) {
    val folder get() = folders.firstOrNull { it.id == folderId }
}
sealed interface LocalWorksEvent {
    data class Open(val mangaId: Long) : LocalWorksEvent
    data class SyncFinished(val result: LocalAuthorSyncResult) : LocalWorksEvent
    data class Message(val resource: Int) : LocalWorksEvent
}
