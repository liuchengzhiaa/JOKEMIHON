package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.R
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.roundedfilled.ViewModule
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SettingsChipRow
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import androidx.compose.ui.res.stringResource as androidStringResource

@Composable
fun ChapterLayoutSelector() {
    val context = LocalContext.current
    val preferences = remember { context.appGraph.libraryPreferences }
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(MaterialSymbols.RoundedFilled.ViewModule, androidStringResource(R.string.chapter_layout_settings))
    }
    if (expanded) {
        AdaptiveSheet(onDismissRequest = { expanded = false }) {
            ChapterDisplaySettings(preferences, onDismiss = { expanded = false })
        }
    }
}

@Composable
private fun ChapterDisplaySettings(preferences: LibraryPreferences, onDismiss: () -> Unit) {
    val selected by preferences.chapterLayout.collectAsState()
    val portrait by preferences.chapterPortraitColumns.collectAsState()
    val landscape by preferences.chapterLandscapeColumns.collectAsState()
    val choices = listOf(
        0 to androidStringResource(R.string.chapter_layout_list),
        1 to androidStringResource(R.string.chapter_layout_cover_list),
        3 to stringResource(MR.strings.action_display_grid),
        2 to stringResource(MR.strings.action_display_comfortable_grid),
        4 to stringResource(MR.strings.action_display_cover_only_grid),
    )
    Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
        HeadingItem(androidStringResource(R.string.chapter_layout_settings))
        SettingsChipRow(MR.strings.action_display_mode) {
            choices.forEach { (mode, label) ->
                FilterChip(
                    selected = selected == mode,
                    onClick = { preferences.chapterLayout.set(mode) },
                    label = { Text(label) },
                )
            }
        }
        if (selected in 2..4) {
            SliderItem(
                value = portrait.coerceIn(0, 10),
                valueRange = 0..10,
                label = androidStringResource(R.string.chapter_columns_portrait),
                valueString = if (portrait in 1..10) portrait.toString() else stringResource(MR.strings.label_auto),
                onChange = preferences.chapterPortraitColumns::set,
                pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            SliderItem(
                value = landscape.coerceIn(0, 10),
                valueRange = 0..10,
                label = androidStringResource(R.string.chapter_columns_landscape),
                valueString = if (landscape in 1..10) landscape.toString() else stringResource(MR.strings.label_auto),
                onChange = preferences.chapterLandscapeColumns::set,
                pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            HeadingItem(MR.strings.overlay_header)
            CheckboxItem(androidStringResource(R.string.chapter_show_read_state), preferences.chapterReadBadge)
            CheckboxItem(androidStringResource(R.string.chapter_show_progress), preferences.chapterProgressBadge)
            CheckboxItem(androidStringResource(R.string.chapter_show_bookmark), preferences.chapterBookmarkBadge)
            CheckboxItem(androidStringResource(R.string.chapter_show_download), preferences.chapterDownloadBadge)
        }
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End).padding(end = 16.dp)) {
            Text(stringResource(MR.strings.action_close))
        }
    }
}
