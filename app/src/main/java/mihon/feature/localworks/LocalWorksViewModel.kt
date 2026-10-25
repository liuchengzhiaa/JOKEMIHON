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
import tachiyomi.source.local.work.LocalWorkPath

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class LocalWorksViewModel(
    private val catalog: LocalWorkCatalog,
    private val library: LocalWorkLibrary,
    private val authorRules: LocalAuthorRulesStore,
    mangaRepository: MangaRepository,
    getCategories: GetCategories,
) : ViewModel() {
    private val mutableState = MutableStateFlow(LocalWorksState())
    val state = mutableState.asStateFlow()
    private val eventChannel = Channel<LocalWorksEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()
    private var loadingJob: Job? = null
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
            mangaRepository.getFavoritesBySourceId(LocalSource.ID).conflate().collect { mangas ->
                val byMember = withContext(Dispatchers.Default) {
                    val index = mutableMapOf<LocalWorkPath, Manga>()
                    for (manga in mangas) {
                        if (!LocalWorkDefinition.isWorkUrl(manga.url)) continue
                        val definition = runCatching {
                            LocalWorkDefinition.fromMemo(manga.url, manga.memo)
                        }.getOrNull() ?: continue
                        for (member in definition.members) {
                            if (member !in index || definition.members.size == 1) index[member] = manga
                        }
                    }
                    index
                }
                mutableState.update { it.copy(favorites = mangas.associateBy(Manga::url), favoriteMembers = byMember) }
            }
        }
        refresh()
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
                    if (version == loadVersion) mutableState.update { it.copy(works = works) }
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

    fun enter(author: LocalAuthorGroup) {
        if (state.value.loading || state.value.saving) return
        mutableState.update { it.copy(author = author, works = emptyList(), selected = emptyList()) }
        refresh()
    }

    fun back(): Boolean {
        if (state.value.saving) return true
        if (state.value.selected.isNotEmpty() || state.value.selectedAuthors.isNotEmpty()) {
            clearSelection()
            return true
        }
        if (state.value.author != null) {
            loadVersion++
            loadingJob?.cancel()
            mutableState.update {
                it.copy(
                    author = null,
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
        if (visible.size > LocalWorkDefinition.MAX_MEMBERS) {
            viewModelScope.launch { eventChannel.send(LocalWorksEvent.Message(R.string.local_works_selection_limit)) }
        } else {
            mutableState.update { it.copy(selected = visible.map(LocalWorkItem::path)) }
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
                val existing = snapshot.favorites[item.preview.url] ?: snapshot.favoriteMembers[item.path]
                val manga = existing ?: library.open(item.path, snapshot.author?.name.orEmpty())
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
    ) {
        if (state.value.saving) return
        val author = state.value.author?.name.orEmpty()
        mutableState.update { it.copy(saving = true, preview = null) }
        viewModelScope.launch {
            try {
                library.add(ordered, combined, title, author, categories, removeCategories, removeOriginals)
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

data class LocalWorkItem(val path: LocalWorkPath, val preview: Manga)
data class LocalWorksState(
    val authors: List<LocalAuthorGroup> = emptyList(),
    val selectedAuthors: List<LocalAuthorGroup> = emptyList(),
    val author: LocalAuthorGroup? = null,
    val works: List<LocalWorkItem> = emptyList(),
    val selected: List<LocalWorkPath> = emptyList(),
    val preview: LocalWorkItem? = null,
    val categories: List<Category> = emptyList(),
    val favorites: Map<String, Manga> = emptyMap(),
    val favoriteMembers: Map<LocalWorkPath, Manga> = emptyMap(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: Boolean = false,
)
sealed interface LocalWorksEvent {
    data class Open(val mangaId: Long) : LocalWorksEvent
    data class Message(val resource: Int) : LocalWorksEvent
}
