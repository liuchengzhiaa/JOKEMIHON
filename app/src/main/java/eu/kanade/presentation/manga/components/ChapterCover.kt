package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.kanade.tachiyomi.data.coil.ChapterCoverRequest
import mihon.app.di.appGraph
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
    MangaCover.Book(model, Modifier.fillMaxWidth())
}
