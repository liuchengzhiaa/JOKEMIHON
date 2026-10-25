package mihon.feature.localworks

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.CancellationException
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Check
import mihon.icons.materialsymbols.rounded.Folder
import mihon.icons.materialsymbols.rounded.Info
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.KeyboardArrowRight
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import mihon.icons.materialsymbols.rounded.Refresh
import mihon.icons.materialsymbols.rounded.Search
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.manga.model.Manga
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.source.local.work.LocalAuthorGroup
import tachiyomi.source.local.work.LocalWorkDefinition
import tachiyomi.source.local.work.LocalWorkPath

/** JOKEMIHON: separate, non-destructive view over the existing local source. */
class LocalWorksScreen : Screen() {
    @Composable
    override fun Content() {
        val viewModel = metroViewModel<LocalWorksViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val resources by rememberUpdatedState(LocalResources.current)
        var query by rememberSaveable(state.author?.key) { mutableStateOf("") }
        var importRequest by remember { mutableStateOf<LocalWorkImport?>(null) }
        var groupNameDialog by remember { mutableStateOf(false) }
        var dissolveDialog by remember { mutableStateOf(false) }
        var backupInfo by remember { mutableStateOf(false) }
        val visibleAuthors = remember(state.authors, query) {
            state.authors.filter {
                it.name.contains(
                    query,
                    true,
                ) || it.directories.any { name ->
                    name.contains(
                        query,
                        true,
                    )
                }
            }
        }
        val visibleWorks = remember(
            state.works,
            query,
        ) {
            state.works.filter {
                it.path.displayName.contains(
                    query,
                    true,
                )
            }
        }
        val back = {
            if (!viewModel.back()) navigator.pop()
            Unit
        }
        BackHandler(
            enabled = state.author != null || state.saving ||
                state.selected.isNotEmpty() || state.selectedAuthors.isNotEmpty(),
            onBack = back,
        )
        LaunchedEffect(Unit) {
            viewModel.events.collect { event ->
                when (event) {
                    is LocalWorksEvent.Open -> navigator.push(MangaScreen(event.mangaId, true))
                    is LocalWorksEvent.Message -> context.toast(resources.getString(event.resource))
                }
            }
        }
        Scaffold(topBar = { scrollBehavior ->
            AppBar(
                title = state.author?.name ?: stringResource(R.string.local_works_title),
                navigateUp = back,
                scrollBehavior = scrollBehavior,
                actions = {
                    if (state.author == null && state.selectedAuthors.isEmpty()) {
                        IconButton(onClick = { backupInfo = true }) {
                            Icon(MaterialSymbols.Rounded.Info, stringResource(R.string.local_works_backup_title))
                        }
                    }
                    if (state.author != null && state.selected.isNotEmpty() && !state.loading && !state.saving) {
                        TextButton(onClick = { viewModel.selectAll(visibleWorks) }) {
                            Text(stringResource(R.string.local_works_select_all))
                        }
                    }
                    IconButton(onClick = viewModel::refresh, enabled = !state.loading && !state.saving) {
                        Icon(MaterialSymbols.Rounded.Refresh, stringResource(R.string.local_works_refresh))
                    }
                },
            )
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (state.saving) LinearProgressIndicator(Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.local_works_search)) },
                    leadingIcon = { Icon(MaterialSymbols.Rounded.Search, contentDescription = null) },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                    enabled = !state.saving,
                )
                if (state.author == null && state.selectedAuthors.isNotEmpty()) {
                    AuthorSelectionBar(
                        state.selectedAuthors,
                        state.saving,
                        { groupNameDialog = true },
                        { dissolveDialog = true },
                        viewModel::clearSelection,
                    )
                } else if (state.author == null) {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Text(
                            stringResource(R.string.local_works_catalog_summary, visibleAuthors.size),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            stringResource(R.string.local_works_group_help),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                } else if (state.selected.isNotEmpty()) {
                    SelectionBar(
                        state.selected.size,
                        { importRequest = LocalWorkImport(state.selected, false) },
                        { importRequest = LocalWorkImport(state.selected, true) },
                        viewModel::clearSelection,
                        state.saving,
                    )
                } else {
                    Text(
                        stringResource(R.string.local_works_author_summary, state.author!!.directories.size),
                        Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                when {
                    state.loading -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                    state.error -> Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.local_works_load_failed))
                        Button(onClick = viewModel::refresh) { Text(stringResource(R.string.local_works_retry)) }
                        OutlinedButton(onClick = {
                            navigator.push(SettingsScreen(SettingsScreen.Destination.DataAndStorage))
                        }) {
                            Text(stringResource(R.string.local_works_storage_settings))
                        }
                    }
                    state.author == null -> {
                        if (visibleAuthors.isEmpty()) {
                            Text(
                                stringResource(R.string.local_works_empty_authors),
                                Modifier.padding(24.dp),
                            )
                        }
                        LazyColumn(
                            Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(visibleAuthors, key = { it.key }) { author ->
                                AuthorCard(
                                    author,
                                    state.selectedAuthors.indexOf(author) + 1,
                                    state.selectedAuthors.isNotEmpty(),
                                    !state.saving,
                                    onClick = {
                                        if (state.selectedAuthors.isEmpty()) {
                                            viewModel.enter(author)
                                        } else {
                                            viewModel.toggleAuthor(author)
                                        }
                                    },
                                    onSelect = { viewModel.toggleAuthor(author) },
                                )
                            }
                        }
                    }
                    else -> {
                        if (visibleWorks.isEmpty()) {
                            Text(
                                stringResource(R.string.local_works_empty_works),
                                Modifier.padding(24.dp),
                            )
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(132.dp),
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            items(visibleWorks, key = { it.path.relativeUrl }) { item ->
                                val ordinal = state.selected.indexOf(item.path) + 1
                                WorkCard(
                                    item = item,
                                    manga = state.favorites[item.preview.url] ?: item.preview,
                                    ordinal = ordinal,
                                    selectionMode = state.selected.isNotEmpty(),
                                    added = item.path in state.favoriteMembers,
                                    enabled = !state.saving,
                                    onSelect = { viewModel.toggle(item.path) },
                                    onClick = {
                                        if (state.selected.isEmpty()) {
                                            viewModel.preview(item)
                                        } else {
                                            viewModel.toggle(item.path)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        state.preview?.let { item ->
            val existing = state.favorites[item.preview.url] ?: state.favoriteMembers[item.path]
            PreviewDialog(
                item,
                existing ?: item.preview,
                existing != null,
                { viewModel.preview(null) },
                { viewModel.open(item) },
                {
                    viewModel.preview(null)
                    val paths = existing?.let {
                        LocalWorkDefinition.fromMemo(it.url, it.memo).members
                    } ?: listOf(item.path)
                    importRequest = LocalWorkImport(paths, paths.size > 1, existing?.title, existing?.id)
                },
            )
        }
        importRequest?.let { request ->
            ImportDialog(
                works = request.works,
                combined = request.combined,
                initialTitle = request.title,
                categoriesOnly = request.categoriesOnlyId != null,
                loadCategories = viewModel::categoryMembership,
                categories = state.categories,
                saving = state.saving,
                onDismiss = { if (!state.saving) importRequest = null },
                onSubmit = { ordered, title, add, remove, removeOriginals ->
                    importRequest = null
                    val id = request.categoriesOnlyId
                    if (id != null) {
                        viewModel.editCategories(id, add, remove)
                    } else {
                        viewModel.add(ordered, request.combined, title, add, remove, removeOriginals)
                    }
                },
            )
        }
        if (groupNameDialog && state.selectedAuthors.isNotEmpty()) {
            AuthorNameDialog(
                state.selectedAuthors,
                { groupNameDialog = false },
                { name ->
                    groupNameDialog = false
                    viewModel.saveAuthorGroup(name)
                },
            )
        }
        if (backupInfo) {
            AlertDialog(
                onDismissRequest = { backupInfo = false },
                title = { Text(stringResource(R.string.local_works_backup_title)) },
                text = {
                    Text(
                        stringResource(R.string.local_works_backup_help),
                        Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        backupInfo = false
                    }) { Text(stringResource(R.string.local_works_understood)) }
                },
            )
        }
        if (dissolveDialog) {
            AlertDialog(
                onDismissRequest = { dissolveDialog = false },
                title = { Text(stringResource(R.string.local_works_dissolve)) },
                text = { Text(stringResource(R.string.local_works_dissolve_help)) },
                confirmButton = {
                    TextButton(onClick = {
                        dissolveDialog = false
                        viewModel.dissolveAuthorGroups()
                    }) {
                        Text(stringResource(R.string.local_works_dissolve))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        dissolveDialog = false
                    }) { Text(stringResource(R.string.local_works_cancel)) }
                },
            )
        }
    }
}

private data class LocalWorkImport(
    val works: List<LocalWorkPath>,
    val combined: Boolean,
    val title: String? = null,
    val categoriesOnlyId: Long? = null,
)

@Composable
private fun AuthorCard(
    author: LocalAuthorGroup,
    ordinal: Int,
    selectionMode: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onSelect: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(
            enabled = enabled,
            onClick = onClick,
            onLongClick = onSelect,
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (ordinal > 0) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                Icon(
                    MaterialSymbols.Rounded.Folder,
                    null,
                    Modifier.padding(10.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    author.name,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    stringResource(
                        if (author.key.startsWith("manual:")) {
                            R.string.local_works_manual_directories
                        } else {
                            R.string.local_works_directories
                        },
                        author.directories.size,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (selectionMode) {
                if (ordinal > 0) Text(ordinal.toString(), color = MaterialTheme.colorScheme.primary)
                Checkbox(checked = ordinal > 0, onCheckedChange = { onSelect() }, enabled = enabled)
            } else {
                Icon(
                    MaterialSymbols.Rounded.KeyboardArrowRight,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AuthorSelectionBar(
    selected: List<LocalAuthorGroup>,
    saving: Boolean,
    onName: () -> Unit,
    onDissolve: () -> Unit,
    onClear: () -> Unit,
) {
    Surface(
        Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.local_works_selected_directories, selected.size),
                    style = MaterialTheme.typography.labelLarge,
                )
                TextButton(onClick = onClear, enabled = !saving) { Text(stringResource(R.string.local_works_clear)) }
            }
            Row {
                TextButton(onClick = onName, enabled = !saving) {
                    Text(
                        stringResource(
                            if (selected.size > 1) {
                                R.string.local_works_merge_directories
                            } else {
                                R.string.local_works_rename
                            },
                        ),
                    )
                }
                if (selected.any { it.key.startsWith("manual:") }) {
                    TextButton(onClick = onDissolve, enabled = !saving) {
                        Text(stringResource(R.string.local_works_dissolve))
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthorNameDialog(selected: List<LocalAuthorGroup>, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(selected.first().name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (selected.size > 1) {
                        R.string.local_works_merge_directories
                    } else {
                        R.string.local_works_rename
                    },
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.local_works_manual_help), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.local_works_directory_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank() && name.length <= 1024) {
                Text(stringResource(R.string.local_works_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.local_works_cancel)) } },
    )
}

@Composable
private fun SelectionBar(count: Int, separate: () -> Unit, combined: () -> Unit, clear: () -> Unit, saving: Boolean) {
    Column(Modifier.padding(horizontal = 12.dp)) {
        Text(stringResource(R.string.local_works_selected, count), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = separate,
                enabled = !saving,
            ) { Text(stringResource(R.string.local_works_add_separately)) }
            TextButton(
                onClick = combined,
                enabled = count > 1 && !saving,
            ) { Text(stringResource(R.string.local_works_add_combined)) }
            TextButton(onClick = clear, enabled = !saving) { Text(stringResource(R.string.local_works_clear)) }
        }
    }
}

@Composable
private fun WorkCard(
    item: LocalWorkItem,
    manga: Manga,
    ordinal: Int,
    selectionMode: Boolean,
    added: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        Modifier.combinedClickable(
            enabled = enabled,
            onClick = onClick,
            onLongClick = onSelect,
            onLongClickLabel = stringResource(R.string.local_works_select),
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = if (ordinal > 0) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Box {
            MangaCover.Book(manga, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            if (added && !selectionMode) {
                Surface(Modifier.align(Alignment.TopStart).padding(6.dp), shape = RoundedCornerShape(10.dp)) {
                    Icon(
                        MaterialSymbols.Rounded.Check,
                        stringResource(R.string.local_works_in_library),
                        Modifier.padding(5.dp).size(18.dp),
                    )
                }
            }
            if (selectionMode) {
                Surface(
                    Modifier.align(Alignment.TopEnd).padding(6.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (ordinal > 0) Text(ordinal.toString(), Modifier.padding(start = 6.dp))
                        val selectionLabel = stringResource(R.string.local_works_select) + " " + item.path.relativeUrl
                        Checkbox(
                            checked = ordinal > 0,
                            onCheckedChange = { onSelect() },
                            enabled = enabled,
                            modifier = Modifier.size(36.dp).semantics { contentDescription = selectionLabel },
                        )
                    }
                }
            }
        }
        Column(Modifier.padding(8.dp)) {
            Text(
                item.path.displayName,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                item.path.directory,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
            )
            if (added) {
                Text(
                    stringResource(R.string.local_works_in_library),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun PreviewDialog(
    item: LocalWorkItem,
    manga: Manga,
    inLibrary: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.path.displayName) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MangaCover.Book(manga, Modifier.width(120.dp).align(Alignment.CenterHorizontally))
                Text(stringResource(R.string.local_works_cover_help))
                Text(item.path.relativeUrl, style = MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = { TextButton(onClick = onOpen) { Text(stringResource(R.string.local_works_open)) } },
        dismissButton = {
            TextButton(onClick = onAdd) {
                Text(
                    stringResource(
                        if (inLibrary) {
                            R.string.local_works_edit_categories
                        } else {
                            R.string.local_works_add
                        },
                    ),
                )
            }
        },
    )
}

@Composable
private fun ImportDialog(
    works: List<LocalWorkPath>,
    combined: Boolean,
    initialTitle: String?,
    categoriesOnly: Boolean,
    loadCategories: suspend (List<LocalWorkPath>, Boolean) -> List<Set<Long>>,
    categories: List<Category>,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (List<LocalWorkPath>, String, List<Long>, List<Long>, Boolean) -> Unit,
) {
    var ordered by remember(works) { mutableStateOf(works) }
    var customTitle by remember { mutableStateOf(initialTitle) }
    var removeOriginals by remember(works) { mutableStateOf(false) }
    var changes by remember(ordered) { mutableStateOf(emptyMap<Long, Boolean>()) }
    var memberships by remember(ordered) { mutableStateOf<List<Set<Long>>?>(null) }
    var categoryError by remember(ordered) { mutableStateOf(false) }
    var retry by remember { mutableStateOf(0) }
    LaunchedEffect(ordered, retry) {
        categoryError = false
        memberships = null
        try {
            memberships = loadCategories(ordered, combined)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            categoryError = true
        }
    }
    val chinese = LocalLocale.current.language == "zh"
    val countLabel = if (chinese && ordered.isNotEmpty()) {
        localCollectionNumeral(ordered.size)
    } else {
        ordered.size.toString()
    }
    val suggested = if (combined) {
        stringResource(
            R.string.local_works_bundle_title,
            ordered.firstOrNull()?.displayName.orEmpty(),
            countLabel,
        )
    } else {
        ordered.firstOrNull()?.displayName.orEmpty()
    }
    val title = customTitle ?: suggested
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            val resource = when {
                categoriesOnly -> R.string.local_works_edit_categories
                combined -> R.string.local_works_add_combined
                else -> R.string.local_works_add_separately
            }
            Text(stringResource(resource))
        },
        text = {
            Column(
                Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (combined && !categoriesOnly) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { customTitle = it },
                        label = { Text(stringResource(R.string.local_works_name)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(onClick = { customTitle = null }) {
                        Text(stringResource(R.string.local_works_automatic_name))
                    }
                }
                if (categoriesOnly) Text(title, style = MaterialTheme.typography.titleSmall)
                if (!categoriesOnly) {
                    Text(stringResource(R.string.local_works_order_help), style = MaterialTheme.typography.bodySmall)
                    LazyColumn(Modifier.height(160.dp)) {
                        items(ordered.size, key = { ordered[it].relativeUrl }) { index ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${index + 1}. ${ordered[index].displayName}",
                                    Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (combined) {
                                    IconButton(
                                        onClick = {
                                            ordered = moveLocalWorkSelection(
                                                ordered,
                                                index,
                                                -1,
                                            )
                                        },
                                        enabled = index > 0,
                                    ) {
                                        Icon(
                                            imageVector = MaterialSymbols.Rounded.KeyboardArrowUp,
                                            contentDescription = stringResource(
                                                R.string.local_works_move_up,
                                                ordered[index].displayName,
                                            ),
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            ordered = moveLocalWorkSelection(
                                                ordered,
                                                index,
                                                1,
                                            )
                                        },
                                        enabled = index < ordered.lastIndex,
                                    ) {
                                        Icon(
                                            imageVector = MaterialSymbols.Rounded.KeyboardArrowDown,
                                            contentDescription = stringResource(
                                                R.string.local_works_move_down,
                                                ordered[index].displayName,
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (combined && !categoriesOnly && ordered.size > 1) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().combinedClickable(
                            enabled = !saving,
                            onClick = { removeOriginals = !removeOriginals },
                        ),
                    ) {
                        Checkbox(
                            checked = removeOriginals,
                            onCheckedChange = { removeOriginals = it },
                            enabled = !saving,
                        )
                        Text(stringResource(R.string.local_works_remove_originals))
                    }
                    Text(
                        stringResource(R.string.local_works_remove_originals_help),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(stringResource(R.string.local_works_category_help), style = MaterialTheme.typography.bodySmall)
                if (categoryError) {
                    Text(stringResource(R.string.local_works_categories_failed))
                    TextButton(onClick = { retry++ }) { Text(stringResource(R.string.local_works_retry)) }
                } else if (memberships == null) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                if (categories.isNotEmpty()) {
                    LazyColumn(Modifier.height(140.dp)) {
                        items(categories, key = { it.id }) { category ->
                            val checked = localCategoryChecked(memberships.orEmpty(), changes, category.id)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().combinedClickable(
                                    enabled = memberships != null,
                                    onClick = {
                                        changes = changes + (category.id to (checked != true))
                                    },
                                ),
                            ) {
                                TriStateCheckbox(
                                    state = when (checked) {
                                        true -> ToggleableState.On
                                        false -> ToggleableState.Off
                                        null -> ToggleableState.Indeterminate
                                    },
                                    onClick = { changes = changes + (category.id to (checked != true)) },
                                    enabled = memberships != null,
                                )
                                Text(category.name)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSubmit(
                        ordered,
                        title.trim(),
                        changes.filterValues { it }.keys.toList(),
                        changes.filterValues { !it }.keys.toList(),
                        removeOriginals && combined && !categoriesOnly && ordered.size > 1,
                    )
                },
                enabled = !saving && memberships != null && ordered.isNotEmpty() &&
                    title.isNotBlank() && title.length <= 1024,
            ) {
                Text(stringResource(if (categoriesOnly) R.string.local_works_save else R.string.local_works_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !saving,
            ) { Text(stringResource(R.string.local_works_cancel)) }
        },
    )
}
