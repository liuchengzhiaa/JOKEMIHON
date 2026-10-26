package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.R
import mihon.app.di.appGraph
import tachiyomi.presentation.core.util.collectAsState

@Composable
fun ChapterLayoutSelector() {
    val context = LocalContext.current
    val preference = remember { context.appGraph.libraryPreferences.chapterLayout }
    val selected by preference.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val labels = listOf(R.string.chapter_layout_list, R.string.chapter_layout_cover_list, R.string.chapter_layout_grid)
    Column {
        TextButton(onClick = { expanded = true }) {
            Text(stringResource(R.string.chapter_layout_button, stringResource(labels.getOrElse(selected) { labels[0] })))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            labels.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    leadingIcon = { Text(if (selected == index) "✓" else "") },
                    onClick = { preference.set(index); expanded = false },
                )
            }
        }
    }
}
