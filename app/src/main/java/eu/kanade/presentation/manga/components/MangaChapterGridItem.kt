package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.library.components.MangaComfortableGridItem
import eu.kanade.presentation.library.components.MangaCompactGridItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Check
import mihon.icons.materialsymbols.roundedfilled.Bookmark
import mihon.icons.materialsymbols.roundedfilled.Circle
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.presentation.core.components.Badge
import tachiyomi.presentation.core.util.collectAsState
import androidx.compose.ui.res.stringResource as androidStringResource

@Composable
fun MangaChapterGridItem(
    title: String,
    readProgress: String?,
    read: Boolean,
    bookmark: Boolean,
    selected: Boolean,
    layout: Int,
    coverData: MangaCover,
    cover: @Composable () -> Unit,
    downloadIndicatorEnabled: Boolean,
    downloadStateProvider: () -> Download.State,
    downloadProgressProvider: () -> Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDownloadClick: ((ChapterDownloadAction) -> Unit)?,
) {
    val context = LocalContext.current
    val preferences = remember { context.appGraph.libraryPreferences }
    val showRead by preferences.chapterReadBadge.collectAsState()
    val showProgress by preferences.chapterProgressBadge.collectAsState()
    val showBookmark by preferences.chapterBookmarkBadge.collectAsState()
    val showDownload by preferences.chapterDownloadBadge.collectAsState()
    val badges: @Composable RowScope.() -> Unit = {
        if (showRead) {
            val description = androidStringResource(if (read) R.string.chapter_state_read else R.string.chapter_state_unread)
            Badge(
                imageVector = if (read) MaterialSymbols.Rounded.Check else MaterialSymbols.RoundedFilled.Circle,
                modifier = Modifier.semantics { contentDescription = description },
                color = if (read) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.secondary,
                iconColor = if (read) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSecondary,
            )
        }
        if (showBookmark && bookmark) {
            val description = androidStringResource(R.string.chapter_show_bookmark)
            Badge(MaterialSymbols.RoundedFilled.Bookmark, Modifier.semantics { contentDescription = description })
        }
        if (showProgress && readProgress != null) Badge(text = readProgress)
    }
    val download: @Composable RowScope.() -> Unit = {
        // Local file availability is not a download button or a read-state indicator.
        if (showDownload && onDownloadClick != null) {
            ChapterDownloadIndicator(
                enabled = downloadIndicatorEnabled,
                downloadStateProvider = downloadStateProvider,
                downloadProgressProvider = downloadProgressProvider,
                onClick = onDownloadClick,
                modifier = Modifier.size(24.dp),
            )
        }
    }
    if (layout == 2) {
        MangaComfortableGridItem(
            coverData = coverData,
            title = title,
            onClick = onClick,
            onLongClick = onLongClick,
            isSelected = selected,
            coverBadgeStart = badges,
            coverBadgeEnd = download,
            coverContent = cover,
        )
    } else {
        MangaCompactGridItem(
            coverData = coverData,
            title = title.takeIf { layout != 4 },
            onClick = onClick,
            onLongClick = onLongClick,
            isSelected = selected,
            coverBadgeStart = badges,
            coverBadgeEnd = download,
            coverContent = cover,
        )
    }
}
