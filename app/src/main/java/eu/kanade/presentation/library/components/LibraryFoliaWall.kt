package eu.kanade.presentation.library.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Precision
import eu.kanade.presentation.util.rememberResourceBitmapPainter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.library.LibraryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Refresh
import mihon.icons.materialsymbols.rounded.Check
import mihon.icons.materialsymbols.automirroredrounded.OpenInNew
import mihon.icons.materialsymbols.roundedfilled.PlayArrow
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.BadgeGroup
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToInt

private class FoliaViewport {
    var transform by mutableStateOf(FoliaTransform())
    var focused by mutableStateOf<Long?>(null)
    var signature: Long = 0

    fun reset() {
        transform = FoliaTransform()
        focused = null
    }

    companion object {
        val Saver = listSaver<FoliaViewport, Any>(
            save = { listOf(it.transform.x, it.transform.y, it.transform.zoom, it.focused ?: Long.MIN_VALUE, it.signature) },
            restore = { values ->
                FoliaViewport().apply {
                    val x = values[0] as Float
                    val y = values[1] as Float
                    val zoom = values[2] as Float
                    if (x.isFinite() && y.isFinite() && zoom.isFinite()) {
                        transform = FoliaTransform(x, y, zoom.coerceIn(0.6f, 1.8f))
                    }
                    focused = (values[3] as Long).takeUnless { it == Long.MIN_VALUE }
                    signature = values[4] as Long
                }
            },
        )
    }
}

/** A category-local 2D canvas. Composition and image requests are restricted to nearby blocks. */
@Composable
internal fun LibraryFoliaWall(
    items: List<LibraryItem>,
    selection: Set<Long>,
    contentPadding: PaddingValues,
    isActive: Boolean,
    onClick: (LibraryManga) -> Unit,
    onLongClick: (LibraryManga) -> Unit,
    onClickContinueReading: ((LibraryManga) -> Unit)?,
    searchQuery: String?,
    onGlobalSearchClicked: () -> Unit,
) {
    val viewport = rememberSaveable(saver = FoliaViewport.Saver) { FoliaViewport() }
    val ids = remember(items) { items.map { it.id } }
    val scene by produceState<FoliaWallScene?>(null, ids) {
        value = null
        value = withContext(Dispatchers.Default) { FoliaWallScene.create(ids) }
    }
    LaunchedEffect(ids) {
        val signature = ids.fold(1125899906842597L) { hash, id -> hash * 31 + id }
        if (viewport.signature != 0L && viewport.signature != signature) viewport.reset()
        viewport.signature = signature
        if (viewport.focused !in ids) viewport.focused = null
    }
    BackHandler(isActive && selection.isEmpty() && viewport.focused != null) { viewport.focused = null }

    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        if (!searchQuery.isNullOrEmpty()) {
            GlobalSearchItem(searchQuery = searchQuery, onClick = onGlobalSearchClicked)
        }
        Box(Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
            val readyScene = scene?.takeIf { it.ids == ids }
            if (readyScene == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                FoliaCanvas(
                    scene = readyScene,
                    items = items,
                    viewport = viewport,
                    selection = selection,
                    onClick = onClick,
                    onLongClick = onLongClick,
                    onClickContinueReading = onClickContinueReading,
                )
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp, end = 4.dp),
                ) {
                    Text(
                        text = stringResource(MR.strings.jokemihon_folia_hint),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    IconButton(onClick = viewport::reset) {
                        Icon(MaterialSymbols.Rounded.Refresh, stringResource(MR.strings.jokemihon_folia_reset))
                    }
                }
            }
        }
    }
}

@Composable
private fun FoliaCanvas(
    scene: FoliaWallScene,
    items: List<LibraryItem>,
    viewport: FoliaViewport,
    selection: Set<Long>,
    onClick: (LibraryManga) -> Unit,
    onLongClick: (LibraryManga) -> Unit,
    onClickContinueReading: ((LibraryManga) -> Unit)?,
) {
    SubcomposeLayout(
        modifier = Modifier.fillMaxSize().pointerInput(scene) {
            detectTransformGestures { centroid, pan, zoom, _ ->
                val unit = minOf(size.width, size.height).coerceAtLeast(1) / 8f
                viewport.transform = scene.constrain(
                    viewport.transform.gesture(
                        centroid.x - size.width / 2f, centroid.y - size.height / 2f,
                        pan.x, pan.y, zoom, unit,
                    ),
                )
            }
        },
    ) { constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val unit = minOf(width, height).coerceAtLeast(1) / 8f
        val transform = viewport.transform
        val scale = unit * transform.zoom
        val ox = width / 2f + (transform.x - 4f) * scale
        val oy = height / 2f + (transform.y - 4f) * scale
        // A small guard band also covers the focused card's expansion; no full-library prefetch.
        val nearby = scene.query(-ox / scale - 0.4f, -oy / scale - 0.4f, (width - ox) / scale + 0.4f, (height - oy) / scale + 0.4f)
        val placements = nearby.tiles.map { tile ->
            val focused = tile.id == viewport.focused
            val item = items[tile.index]
            val w = (tile.width * scale - 4.dp.toPx()).roundToInt().coerceAtLeast(1)
            val h = (tile.height * scale - 4.dp.toPx()).roundToInt().coerceAtLeast(1)
            // Stable decode buckets do not churn on every pinch frame; never decode a full-size original.
            val decodeSize = ((maxOf(tile.width, tile.height) * unit * 1.8f / 128).roundToInt() * 128).coerceIn(256, 1024)
            val placeable = subcompose(tile.id) {
                FoliaPoster(
                    item = item,
                    focused = focused && selection.isEmpty(),
                    dimmed = viewport.focused != null && !focused && selection.isEmpty(),
                    selected = item.id in selection,
                    decodeSize = decodeSize,
                    onClick = {
                        if (selection.isNotEmpty() || viewport.focused == item.id) {
                            onClick(item.libraryManga)
                        } else {
                            viewport.focused = item.id
                        }
                    },
                    onLongClick = { onLongClick(item.libraryManga) },
                    onContinue = onClickContinueReading?.takeIf { item.unreadCount > 0 }?.let { { it(item.libraryManga) } },
                )
            }.single().measure(Constraints.fixed(w, h))
            Triple(tile, placeable, focused)
        }
        layout(width, height) {
            placements.forEach { (tile, placeable, focused) ->
                placeable.placeWithLayer(
                    (ox + tile.x * scale + 2.dp.toPx()).roundToInt(),
                    (oy + tile.y * scale + 2.dp.toPx()).roundToInt(),
                    zIndex = if (focused) 1f else 0f,
                )
            }
        }
    }
}

@Composable
private fun FoliaPoster(
    item: LibraryItem,
    focused: Boolean,
    dimmed: Boolean,
    selected: Boolean,
    decodeSize: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onContinue: (() -> Unit)?,
) {
    val manga = item.libraryManga.manga
    val context = LocalContext.current
    val model = remember(manga.id, manga.thumbnailUrl, manga.coverLastModified, manga.source, manga.favorite, decodeSize) {
        ImageRequest.Builder(context)
            .data(MangaCover(manga.id, manga.source, manga.favorite, manga.thumbnailUrl, manga.coverLastModified))
            .size(decodeSize)
            .precision(Precision.INEXACT)
            .build()
    }
    val emphasis by animateFloatAsState(if (focused) 1.18f else 1f, label = "folia-focus")
    val opacity by animateFloatAsState(if (dimmed) 0.58f else 1f, label = "folia-dim")
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier.fillMaxSize()
            .graphicsLayer { scaleX = emphasis; scaleY = emphasis; alpha = opacity }
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(if (selected || focused) 2.dp else 0.dp, if (selected || focused) MaterialTheme.colorScheme.primary else Color.Transparent, shape)
            .semantics { this.selected = selected }
            .combinedClickable(
                onClickLabel = stringResource(if (focused) MR.strings.jokemihon_folia_open else MR.strings.jokemihon_folia_focus),
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        AsyncImage(
            model = model,
            contentDescription = manga.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = if (focused) ContentScale.Fit else ContentScale.Crop,
            placeholder = ColorPainter(Color(0x1F888888)),
            error = rememberResourceBitmapPainter(R.drawable.cover_error),
        )
        BadgeGroup(Modifier.align(Alignment.TopStart).padding(6.dp)) {
            DownloadsBadge(item.badges.downloadCount)
            UnreadBadge(item.badges.unreadCount)
        }
        BadgeGroup(Modifier.align(Alignment.TopEnd).padding(6.dp)) {
            LanguageBadge(item.badges.isLocal, item.badges.sourceLanguage)
        }
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xDD000000))))
                .padding(8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = manga.title,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                maxLines = if (focused) 3 else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(MaterialSymbols.Rounded.Check, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            } else if (focused || onContinue != null) {
                FilledTonalIconButton(onClick = if (focused) onClick else onContinue!!, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (focused) MaterialSymbols.AutoMirroredRounded.OpenInNew else MaterialSymbols.RoundedFilled.PlayArrow,
                        stringResource(if (focused) MR.strings.jokemihon_folia_open else MR.strings.action_start),
                        Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
