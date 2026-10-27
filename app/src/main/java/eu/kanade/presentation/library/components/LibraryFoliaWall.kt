package eu.kanade.presentation.library.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Precision
import eu.kanade.presentation.library.components.folia.FocusWallShape
import eu.kanade.presentation.library.components.folia.FoliaCoverFacts
import eu.kanade.presentation.library.components.folia.FoliaCoverFactsCache
import eu.kanade.presentation.library.components.folia.artworkDecodeSize
import eu.kanade.presentation.library.components.folia.focusWallDepthScale
import eu.kanade.presentation.util.rememberResourceBitmapPainter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.library.LibraryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.OpenInNew
import mihon.icons.materialsymbols.rounded.Check
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.Folder
import mihon.icons.materialsymbols.roundedfilled.PlayArrow
import mihon.icons.materialsymbols.roundedfilled.ViewModule
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

private class FoliaViewport {
    var transform by mutableStateOf(FoliaTransform())
    var focused by mutableStateOf<Long?>(null)
    var signature: Long = 0
    var restored = false
    var gesturing = false
    var motion: Job? = null

    fun clearFocus() {
        motion?.cancel()
        focused = null
    }

    fun reset() {
        clearFocus()
        transform = FoliaTransform()
    }

    companion object {
        val Saver = listSaver<FoliaViewport, Any>(
            save = { listOf(it.transform.x, it.transform.y, it.focused ?: Long.MIN_VALUE, it.signature) },
            restore = { values ->
                FoliaViewport().apply {
                    val x = values[0] as Float
                    val y = values[1] as Float
                    if (x.isFinite() && y.isFinite()) transform = FoliaTransform(x, y)
                    focused = (values[2] as Long).takeUnless { it == Long.MIN_VALUE }
                    signature = values[3] as Long
                    restored = true
                }
            },
        )
    }
}

/** Music Folia's block reflow and poster rendering adapted to manga identities/actions. */
@Composable
internal fun LibraryFoliaWall(
    appearance: FoliaAppearance,
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
    var settingsOpen by remember { mutableStateOf(false) }
    val ids = remember(items) { items.map { it.id } }
    val itemsById = remember(items) { items.associateBy { it.id } }
    val focusedBook = viewport.focused?.let(itemsById::get)?.libraryManga?.manga
    val cover = focusedBook?.let { MangaCover(it.id, it.source, it.favorite, it.thumbnailUrl, it.coverLastModified) }
    val context = LocalContext.current.applicationContext
    val facts by key(cover) {
        produceState(FoliaCoverFactsCache.peekOrNull(cover), cover) {
            if (cover != null) value = FoliaCoverFactsCache.load(context, cover)
        }
    }
    LaunchedEffect(ids) {
        val signature = ids.fold(1125899906842597L) { hash, id -> hash * 31 + id }
        if (viewport.signature != 0L && viewport.signature != signature) viewport.reset()
        viewport.signature = signature
        if (viewport.focused !in ids) viewport.focused = null
    }
    BackHandler(isActive && selection.isEmpty() && viewport.focused != null) { viewport.clearFocus() }

    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        if (!searchQuery.isNullOrEmpty()) GlobalSearchItem(searchQuery = searchQuery, onClick = onGlobalSearchClicked)
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).clipToBounds().background(Color(0xFF080B10))) {
            val ratio = (maxHeight.value / maxWidth.value.coerceAtLeast(1f)).coerceAtLeast(0.1f)
            val style = FoliaStyle(appearance.shape, appearance.size, appearance.density)
            val focus = viewport.focused.takeIf { selection.isEmpty() }
            val aspect = facts?.takeIf { cover?.mangaId == focus }?.aspect
            val producedScene by produceState<FoliaWallScene?>(null, ids, focus, style, aspect, ratio) {
                value = withContext(Dispatchers.Default) {
                    val layoutContext = coroutineContext
                    FoliaWallScene(ids, focus, style, aspect, ratio) { layoutContext.ensureActive() }
                }
            }
            val scene = producedScene?.takeIf { it.ids == ids }
            val palette = focus?.let { foliaBookPalette(facts, it) }.orEmpty()
            FoliaAmbientLayer(palette, appearance, wash = false, modifier = Modifier.fillMaxSize())
            if (scene == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                FoliaCanvas(
                    scene,
                    items,
                    viewport,
                    appearance,
                    selection,
                    facts,
                    onClick,
                    onLongClick,
                    onClickContinueReading,
                )
            }
            FoliaAmbientLayer(palette, appearance, wash = true, modifier = Modifier.fillMaxSize())
            IconButton(
                onClick = {
                    viewport.motion?.cancel()
                    settingsOpen = true
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).size(48.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.24f), CircleShape)
                    .clip(CircleShape).background(Color(0xDC10141B)),
            ) {
                Icon(
                    MaterialSymbols.RoundedFilled.ViewModule,
                    stringResource(MR.strings.jokemihon_folia_settings),
                    tint = Color(0xFFB8DFFF),
                )
            }
        }
    }
    if (settingsOpen) {
        FoliaAppearanceSheet(
            appearance,
            viewport::reset,
            { viewport.clearFocus() },
            { settingsOpen = false },
        )
    }
}

@Composable
private fun FoliaCanvas(
    scene: FoliaWallScene,
    items: List<LibraryItem>,
    viewport: FoliaViewport,
    appearance: FoliaAppearance,
    selection: Set<Long>,
    facts: FoliaCoverFacts?,
    onClick: (LibraryManga) -> Unit,
    onLongClick: (LibraryManga) -> Unit,
    onContinue: ((LibraryManga) -> Unit)?,
) {
    val scope = rememberCoroutineScope()
    DisposableEffect(viewport) {
        onDispose {
            viewport.motion?.cancel()
            viewport.motion = null
        }
    }
    val latestScene by rememberUpdatedState(scene)
    val palette = foliaBookPalette(facts, viewport.focused ?: 0L)
    val accent = palette.first()
    LaunchedEffect(appearance.motion) { if (appearance.motion == 0) viewport.motion?.cancel() }
    LaunchedEffect(scene) {
        if (scene.focusedId != viewport.focused) return@LaunchedEffect
        if (viewport.restored) {
            viewport.restored = false
            return@LaunchedEffect
        }
        if (viewport.gesturing) return@LaunchedEffect
        val hero = scene.focusedId?.let(scene.byId::get) ?: return@LaunchedEffect
        viewport.motion?.cancel()
        viewport.motion = scope.launch {
            Animatable(Offset(viewport.transform.x, viewport.transform.y), Offset.VectorConverter).animateTo(
                Offset(-hero.centerX, -hero.centerY),
                tween(motionDuration(280, appearance.motion)),
            ) { viewport.transform = viewport.transform.copy(x = value.x, y = value.y) }
        }
    }
    SubcomposeLayout(
        Modifier.fillMaxSize().pointerInput(viewport) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                viewport.motion?.cancel()
                viewport.gesturing = true
                val tracker = VelocityTracker().apply { addPosition(down.uptimeMillis, down.position) }
                var pastSlop = false
                var panTotal = Offset.Zero
                var zoomTotal = 1f
                var multiTouch = false
                val unit = size.width.coerceAtLeast(1) / 12f
                try {
                    do {
                        val event = awaitPointerEvent()
                        multiTouch = multiTouch || event.changes.count { it.pressed } > 1
                        if (event.changes.any { it.isConsumed }) continue
                        val pan = event.calculatePan()
                        val zoom = event.calculateZoom()
                        val center = event.calculateCentroid()
                        if (center.isSpecified) {
                            event.changes.firstOrNull()?.let {
                                tracker.addPosition(
                                    it.uptimeMillis,
                                    center,
                                )
                            }
                        }
                        panTotal += pan
                        zoomTotal *= zoom
                        val justPassed = !pastSlop &&
                            (
                                panTotal.getDistance() > viewConfiguration.touchSlop ||
                                    abs(zoomTotal - 1f) * size.width / 2 > viewConfiguration.touchSlop
                                )
                        pastSlop = pastSlop || justPassed
                        if (pastSlop) {
                            val relative = if (center.isSpecified) {
                                center - Offset(
                                    size.width / 2f,
                                    size.height / 2f,
                                )
                            } else {
                                Offset.Zero
                            }
                            val delta = if (justPassed) panTotal else pan
                            val factor = if (justPassed) zoomTotal else zoom
                            val next = viewport.transform.copy(zoom = appearance.zoom).gesture(
                                relative.x,
                                relative.y,
                                delta.x,
                                delta.y,
                                factor,
                                unit,
                            )
                            viewport.transform = latestScene.constrain(next)
                            appearance.zoom = next.zoom
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                } finally {
                    viewport.gesturing = false
                }
                if (pastSlop && !multiTouch && appearance.motion > 0) {
                    val velocity = tracker.calculateVelocity()
                    val speed = Offset(velocity.x, velocity.y) * (appearance.motion / 100f) / (unit * appearance.zoom)
                    if (speed.getDistance() > 0.1f) {
                        var coast: Job? = null
                        coast = scope.launch {
                            Animatable(Offset(viewport.transform.x, viewport.transform.y), Offset.VectorConverter)
                                .animateDecay(speed, exponentialDecay(frictionMultiplier = 1.35f)) {
                                    val next = latestScene.constrain(viewport.transform.copy(x = value.x, y = value.y))
                                    viewport.transform = next
                                    if (next.x != value.x || next.y != value.y) coast?.cancel()
                                }
                        }
                        viewport.motion = coast
                    }
                }
            }
        },
    ) { constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val unit = width.coerceAtLeast(1) / 12f
        val zoom = appearance.zoom
        val pan = viewport.transform
        val scale = unit * zoom
        val margin = 0.35f
        val left = -width / (2f * scale) - pan.x
        val top = -height / (2f * scale) - pan.y
        val nearby = scene.query(
            left - 12 * margin,
            top - 12 * margin,
            left + width / scale + 12 * margin,
            top + height / scale + 12 * margin,
        )
        val measured = nearby.tiles.map { tile ->
            val item = items[tile.index]
            val focused = tile.itemId.toLong() == viewport.focused && selection.isEmpty()
            val baseW = (tile.width * unit).roundToInt().coerceAtLeast(1)
            val baseH = (tile.height * unit).roundToInt().coerceAtLeast(1)
            val placeable = subcompose(tile.itemId) {
                val duration = motionDuration(320, appearance.motion)
                val cx by animateFloatAsState(tile.centerX, tween(duration), label = "folia-card-x")
                val cy by animateFloatAsState(tile.centerY, tween(duration), label = "folia-card-y")
                val w by animateFloatAsState(tile.width, tween(duration), label = "folia-card-width")
                val h by animateFloatAsState(tile.height, tween(duration), label = "folia-card-height")
                val dark by animateFloatAsState(
                    if (appearance.lightsOut &&
                        !focused &&
                        viewport.focused != null &&
                        selection.isEmpty()
                    ) {
                        1f
                    } else {
                        0f
                    },
                    tween(
                        motionDuration(
                            260,
                            appearance.motion,
                        ),
                    ),
                    label = "folia-lights-out",
                )
                FoliaPoster(
                    item = item, focused = focused, selected = item.id in selection,
                    fitArtwork = tile.fitArtwork, silhouette = tile.shape,
                    decodeSize = artworkDecodeSize(tile.width * unit, tile.height * unit, zoom),
                    accent = accent,
                    tint = if (viewport.focused != null &&
                        !focused
                    ) {
                        accent.copy(alpha = appearance.tint / 100f)
                    } else {
                        Color.Transparent
                    },

                    motion = appearance.motion,
                    onClick = {
                        if (selection.isNotEmpty() ||
                            viewport.focused == item.id
                        ) {
                            onClick(item.libraryManga)
                        } else {
                            viewport.focused = item.id
                        }
                    },

                    onLongClick = {
                        viewport.clearFocus()
                        onLongClick(item.libraryManga)
                    },
                    onClear = { viewport.clearFocus() },
                    onContinue = onContinue?.takeIf { item.unreadCount > 0 }?.let { { it(item.libraryManga) } },
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        val liveZoom = appearance.zoom
                        val livePan = viewport.transform
                        val depth = focusWallDepthScale(
                            (tile.centerX + livePan.x) * unit * liveZoom,
                            (tile.centerY + livePan.y) * unit * liveZoom,
                            width.toFloat(),
                            height.toFloat(),
                        )
                        val normal = when {
                            focused -> 1f
                            viewport.focused != null &&
                                selection.isEmpty() -> (0.48f + depth * 0.34f).coerceAtMost(0.78f)
                            else -> 0.72f + depth * 0.28f
                        }
                        alpha = normal + (normal.coerceAtMost(0.22f) - normal) * dark
                        scaleX = liveZoom * w / tile.width
                        scaleY = liveZoom * h / tile.height
                        translationX = (cx - tile.centerX) * unit * liveZoom
                        translationY = (cy - tile.centerY) * unit * liveZoom
                        shadowElevation = if (focused) 12.dp.toPx() else 0f
                    },
                )
            }.single().measure(Constraints.fixed(baseW, baseH))
            Triple(tile, placeable, focused)
        }
        layout(width, height) {
            measured.forEach { (tile, placeable, focused) ->
                val sx = width / 2f + (tile.centerX + pan.x) * scale
                val sy = height / 2f + (tile.centerY + pan.y) * scale
                placeable.place(
                    (sx - placeable.width / 2f).roundToInt(),
                    (sy - placeable.height / 2f).roundToInt(),
                    if (focused) 10f else 0f,
                )
            }
        }
    }
}

@Composable
private fun FoliaPoster(
    item: LibraryItem,
    focused: Boolean,
    selected: Boolean,
    fitArtwork: Boolean,
    silhouette: FocusWallShape?,
    decodeSize: androidx.compose.ui.unit.IntSize,
    accent: Color,
    tint: Color,
    motion: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClear: () -> Unit,
    onContinue: (() -> Unit)?,
    modifier: Modifier,
) {
    val manga = item.libraryManga.manga
    val context = LocalContext.current
    val cover = remember(
        manga.id,
        manga.source,
        manga.favorite,
        manga.thumbnailUrl,
        manga.coverLastModified,
    ) {
        MangaCover(
            manga.id,
            manga.source,
            manga.favorite,
            manga.thumbnailUrl,
            manga.coverLastModified,
        )
    }
    val request = remember(
        cover,
        decodeSize,
    ) {
        ImageRequest.Builder(context).data(cover).size(
            decodeSize.width,
            decodeSize.height,
        ).precision(Precision.INEXACT).build()
    }
    val shape: Shape = remember(silhouette) { silhouette?.let(::FoliaCellShape) ?: RectangleShape }
    val border by animateColorAsState(
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else if (focused) {
            accent.copy(alpha = 0.76f)
        } else {
            Color.Transparent
        },
        tween(
            motionDuration(
                260,
                motion,
            ),
        ),
        label = "folia-border",
    )
    val stroke by animateFloatAsState(
        if (selected) {
            2f
        } else if (focused) {
            1.1f
        } else {
            0f
        },
        tween(
            motionDuration(
                260,
                motion,
            ),
        ),
        label = "folia-stroke",
    )
    val wash by animateColorAsState(tint, tween(motionDuration(260, motion)), label = "folia-tint")
    BoxWithConstraints(
        modifier.border(stroke.dp, border, shape).clip(shape).background(Color(0xFF10141B))
            .semantics { this.selected = selected }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onClickLabel = stringResource(
                    if (focused) MR.strings.jokemihon_folia_open else MR.strings.jokemihon_folia_focus,
                ),
            ),

    ) {
        AsyncImage(
            model = request,
            contentDescription = manga.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = if (fitArtwork) ContentScale.Fit else ContentScale.Crop,
            placeholder = ColorPainter(Color(0xFF10141B)),
            error = rememberResourceBitmapPainter(R.drawable.cover_error),

            onSuccess = { FoliaCoverFactsCache.observe(cover, it.result.image.width, it.result.image.height) },
        )
        Box(
            Modifier.fillMaxSize().drawWithCache {
                val shade = if (focused) {
                    Brush.verticalGradient(
                        0f to Color(0x42000000),
                        0.38f to Color(0x30000000),
                        0.72f to Color(0x62000000),
                        1f to Color(0xD605070B),
                    )
                } else {
                    Brush.verticalGradient(
                        0f to Color(0x18000000),
                        0.48f to Color(0x26000000),
                        1f to Color(0xDA05070B),
                    )
                }
                onDrawBehind {
                    if (wash.alpha > 0f) drawRect(wash)
                    drawRect(shade)
                }
            },
        )
        val inset = maxWidth * if (focused) 0.043f else 0.052f
        val titleSize = if (focused) {
            (maxWidth.value * 0.035f).coerceIn(10f, 17f).sp
        } else {
            (maxWidth.value * 0.072f).coerceIn(9f, 18f).sp
        }
        val metadataSize = (maxWidth.value * if (focused) 0.018f else 0.033f).coerceIn(7f, 10f).sp
        val touchSize = (maxWidth * 0.066f).coerceIn(16.dp, 30.dp)
        val iconSize = (maxWidth * 0.034f).coerceIn(9.dp, 17.dp)
        if (focused) {
            Text(
                text = stringResource(MR.strings.jokemihon_folia_work),
                modifier = Modifier.align(Alignment.TopStart)
                    .padding(start = inset, top = maxHeight * 0.024f)
                    .background(Color(0x7810141B))
                    .padding(horizontal = 3.dp, vertical = 1.dp),
                color = lerp(accent, Color.White, 0.5f).copy(alpha = 0.88f),
                fontSize = metadataSize,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
        Column(
            modifier = if (focused) {
                Modifier.align(Alignment.TopStart)
                    .padding(start = inset, top = maxHeight * 0.096f)
                    .width(maxWidth * 0.56f)
            } else {
                Modifier.align(Alignment.BottomStart)
                    .padding(start = inset, end = inset, bottom = maxHeight * 0.055f)
                    .fillMaxWidth()
            },
        ) {
            Text(
                text = manga.title,
                color = Color.White.copy(alpha = if (focused) 0.94f else 0.90f),
                fontSize = titleSize,
                fontWeight = FontWeight.Bold,
                maxLines = if (focused) 1 else 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall.copy(
                    shadow = Shadow(Color.Black.copy(alpha = 0.94f), Offset(0f, 1f), 6f),
                ),
            )
            if (!manga.author.isNullOrBlank()) {
                Text(
                    manga.author.orEmpty(),
                    color = Color.White.copy(alpha = 0.58f),
                    fontSize = metadataSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (selected) {
            Icon(
                MaterialSymbols.Rounded.Check,
                null,
                Modifier.align(Alignment.TopStart).padding(4.dp).size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        } else if (!focused) {
            if (item.badges.isLocal ||
                item.badges.sourceLanguage.isNotBlank()
            ) {
                Box(
                    Modifier.align(Alignment.TopStart).padding(inset).size(
                        (
                            maxWidth *
                                0.105f
                            ).coerceIn(
                            7.dp,
                            18.dp,
                        ),
                    ).background(Color(0x7810141B)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (item.badges.isLocal) {
                        Icon(
                            MaterialSymbols.Rounded.Folder,
                            null,
                            Modifier.fillMaxSize().padding(2.dp),
                            tint = Color.White.copy(alpha = 0.76f),
                        )
                    } else {
                        Text(
                            item.badges.sourceLanguage.uppercase(),
                            color = Color.White.copy(alpha = 0.76f),
                            fontSize = 7.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        if (!focused &&
            (
                item.badges.unreadCount > 0 ||
                    item.badges.downloadCount > 0
                )
        ) {
            Row(Modifier.align(Alignment.TopEnd).padding(inset)) {
                if (item.badges.downloadCount > 0) {
                    Text(
                        item.badges.downloadCount.toString(),
                        Modifier.background(MaterialTheme.colorScheme.tertiary).padding(horizontal = 2.dp),
                        color = MaterialTheme.colorScheme.onTertiary,
                        fontSize = metadataSize,
                    )
                }
                if (item.badges.unreadCount > 0) {
                    Text(
                        item.badges.unreadCount.toString(),
                        Modifier.background(MaterialTheme.colorScheme.primary).padding(horizontal = 2.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = metadataSize,
                    )
                }
            }
        }
        if (focused) {
            IconButton(onClick = onClear, modifier = Modifier.align(Alignment.BottomStart).size(touchSize)) {
                Icon(
                    MaterialSymbols.Rounded.Close,
                    stringResource(MR.strings.jokemihon_folia_clear_focus),
                    Modifier.size(iconSize),
                    tint = Color.White.copy(alpha = 0.85f),
                )
            }
            IconButton(onClick = onClick, modifier = Modifier.align(Alignment.BottomEnd).size(touchSize)) {
                Icon(
                    MaterialSymbols.AutoMirroredRounded.OpenInNew,
                    stringResource(MR.strings.jokemihon_folia_open),
                    Modifier.size(iconSize),
                    tint = Color.White.copy(alpha = 0.85f),
                )
            }
        } else if (onContinue != null) {
            IconButton(
                onClick = onContinue,
                modifier = Modifier.align(Alignment.BottomEnd).size(28.dp),
            ) {
                Icon(
                    MaterialSymbols.RoundedFilled.PlayArrow,
                    stringResource(MR.strings.action_start),
                    Modifier.size(14.dp),
                    tint = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}

private class FoliaCellShape(private val cells: FocusWallShape) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        cells.rects.forEach {
            path.addRect(
                Rect(
                    it.left * size.width,
                    it.top * size.height,
                    it.right * size.width,
                    it.bottom * size.height,
                ),
            )
        }
        return Outline.Generic(Path().apply { op(path, path, PathOperation.Union) })
    }
}

private fun motionDuration(base: Int, percent: Int) = (base * percent.coerceIn(0, 100) / 100f).toInt()
