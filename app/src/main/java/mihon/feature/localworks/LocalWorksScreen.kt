package mihon.feature.localworks

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import mihon.icons.materialsymbols.rounded.Refresh
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.manga.model.Manga
import tachiyomi.presentation.core.components.material.Scaffold
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
        var importCombined by remember { mutableStateOf<Boolean?>(null) }
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
        BackHandler(enabled = state.author != null || state.saving || state.selected.isNotEmpty(), onBack = back)
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
                    if (state.author != null && !state.loading && !state.saving) {
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
                    label = { Text(stringResource(R.string.local_works_search)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    singleLine = true,
                    enabled = !state.saving,
                )
                if (state.author == null) {
                    Text(
                        stringResource(R.string.local_works_group_help),
                        Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else if (state.selected.isNotEmpty()) {
                    SelectionBar(
                        state.selected.size,
                        { importCombined = false },
                        { importCombined = true },
                        viewModel::clearSelection,
                        state.saving,
                    )
                } else {
                    Text(
                        stringResource(
                            R.string.local_works_author_summary,
                            state.author!!.directories.size,
                        ),
                        Modifier.padding(12.dp),
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
                        LazyColumn(Modifier.weight(1f)) {
                            items(visibleAuthors, key = { it.key }) { author ->
                                ListItem(
                                    headlineContent = { Text(author.name) },
                                    supportingContent = {
                                        Text(
                                            stringResource(
                                                R.string.local_works_directories,
                                                author.directories.size,
                                            ),
                                        )
                                    },
                                    modifier = Modifier.combinedClickable(
                                        enabled = !state.saving,
                                        onClick = { viewModel.enter(author) },
                                    ),
                                )
                                HorizontalDivider()
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
                            columns = GridCells.Adaptive(112.dp),
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                        ) {
                            items(visibleWorks, key = { it.path.relativeUrl }) { item ->
                                val ordinal = state.selected.indexOf(item.path) + 1
                                WorkCard(
                                    item = item,
                                    manga = state.favorites[item.preview.url] ?: item.preview,
                                    ordinal = ordinal,
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
            PreviewDialog(
                item,
                state.favorites[item.preview.url] ?: item.preview,
                { viewModel.preview(null) },
                { viewModel.open(item) },
                {
                    viewModel.preview(null)
                    viewModel.clearSelection()
                    viewModel.toggle(item.path)
                    importCombined = false
                },
            )
        }
        importCombined?.let { combined ->
            ImportDialog(
                works = state.selected,
                combined = combined,
                categories = state.categories,
                saving = state.saving,
                onDismiss = { if (!state.saving) importCombined = null },
                onSubmit = { ordered, title, categories ->
                    importCombined = null
                    viewModel.add(ordered, combined, title, categories)
                },
            )
        }
    }
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
    ) {
        Box {
            MangaCover.Book(manga, Modifier.fillMaxWidth())
            Surface(Modifier.align(Alignment.TopEnd), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (ordinal > 0) Text(ordinal.toString(), Modifier.padding(start = 6.dp))
                    val selectionLabel = stringResource(R.string.local_works_select) + " " + item.path.relativeUrl
                    Checkbox(
                        checked = ordinal > 0,
                        onCheckedChange = { onSelect() },
                        enabled = enabled,
                        modifier = Modifier.semantics { contentDescription = selectionLabel },
                    )
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
        dismissButton = { TextButton(onClick = onAdd) { Text(stringResource(R.string.local_works_add)) } },
    )
}

@Composable
private fun ImportDialog(
    works: List<LocalWorkPath>,
    combined: Boolean,
    categories: List<Category>,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (List<LocalWorkPath>, String, List<Long>) -> Unit,
) {
    var ordered by remember(works) { mutableStateOf(works) }
    var customTitle by remember { mutableStateOf<String?>(null) }
    var chosenCategories by remember { mutableStateOf(emptySet<Long>()) }
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
            val resource = if (combined) R.string.local_works_add_combined else R.string.local_works_add_separately
            Text(stringResource(resource))
        },
        text = {
            Column(
                Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (combined) {
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
                Text(stringResource(R.string.local_works_category_help), style = MaterialTheme.typography.bodySmall)
                if (categories.isNotEmpty()) {
                    LazyColumn(Modifier.height(140.dp)) {
                        items(categories, key = { it.id }) { category ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {
                                    chosenCategories = if (category.id in chosenCategories) {
                                        chosenCategories - category.id
                                    } else {
                                        chosenCategories + category.id
                                    }
                                }),
                            ) {
                                Checkbox(checked = category.id in chosenCategories, onCheckedChange = {
                                    chosenCategories = if (it) {
                                        chosenCategories + category.id
                                    } else {
                                        chosenCategories - category.id
                                    }
                                })
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
                        chosenCategories.toList(),
                    )
                },
                enabled = !saving && ordered.isNotEmpty() && title.isNotBlank() && title.length <= 1024,
            ) {
                Text(stringResource(R.string.local_works_confirm))
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
