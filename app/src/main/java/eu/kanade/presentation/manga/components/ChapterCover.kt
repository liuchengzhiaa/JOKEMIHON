package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import eu.kanade.presentation.util.rememberResourceBitmapPainter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.coil.ChapterCoverRequest
import logcat.LogPriority
import mihon.app.di.appGraph
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.model.Manga
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.source.local.isLocal

@Composable
fun ChapterCover(manga: Manga, chapterUrl: String) {
    val context = LocalContext.current
    val graph = remember { context.appGraph }
    val revision by graph.chapterCovers.revision.collectAsStateWithLifecycle()
    val root by graph.storagePreferences.baseStorageDirectory.collectAsState()
    val model = if (manga.isLocal()) ChapterCoverRequest(chapterUrl, root, revision) else manga
    AsyncImage(
        model = model,
        modifier = Modifier.fillMaxWidth().aspectRatio(MangaCover.Book.ratio).clip(MaterialTheme.shapes.extraSmall),
        placeholder = ColorPainter(Color(0x1F888888)),
        error = rememberResourceBitmapPainter(R.drawable.cover_error),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        onError = { error ->
            logcat(LogPriority.ERROR, error.result.throwable) { "Chapter cover decode/load failed" }
        },
    )
}
