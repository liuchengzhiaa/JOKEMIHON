package eu.kanade.presentation.library.components

import eu.kanade.presentation.library.components.folia.FocusWallDensity
import eu.kanade.presentation.library.components.folia.FocusWallLayoutItem
import eu.kanade.presentation.library.components.folia.FocusWallOrientation
import eu.kanade.presentation.library.components.folia.FocusWallPlacement
import eu.kanade.presentation.library.components.folia.FocusWallSpatialIndex
import eu.kanade.presentation.library.components.folia.FoliaFocusShape
import eu.kanade.presentation.library.components.folia.FoliaFocusSize
import eu.kanade.presentation.library.components.folia.FoliaWallLayoutEngine
import eu.kanade.presentation.library.components.folia.clampWallPan
import eu.kanade.presentation.library.components.folia.wallWorldBounds

internal data class FoliaQuery(val tiles: List<FocusWallPlacement>, val examined: Int)

/** Uses the same 12x8 exact-cover/reflow engine as the owner's music application. */
internal class FoliaWallScene(
    val ids: List<Long>,
    val focusedId: Long?,
    val settings: FoliaStyle,
    val aspect: Float?,
    val viewportRatio: Float,
    checkCancellation: () -> Unit = {},
) {
    val tiles = FoliaWallLayoutEngine.layout(
        items = ids.map { FocusWallLayoutItem(it.toString(), it.hashCode()) },
        viewportWidth = 12f,
        viewportHeight = 12f * viewportRatio,
        orientation = if (viewportRatio < 1f) FocusWallOrientation.Landscape else FocusWallOrientation.Portrait,
        density = FocusWallDensity.Medium,
        densityScale = settings.density / 100f,
        focusedItemId = focusedId?.toString(),
        gapPx = 0f,
        focusShape = settings.shape,
        focusSize = settings.size,
        focusArtworkAspect = aspect,
        checkCancellation = checkCancellation,
    )
    private val index = FocusWallSpatialIndex(tiles)
    val byId = tiles.associateBy { it.itemId.toLong() }
    val bounds = wallWorldBounds(tiles)
    private val halfWidth = tiles.maxOfOrNull { it.width / 2 } ?: 0f
    private val halfHeight = tiles.maxOfOrNull { it.height / 2 } ?: 0f
    val minX get() = bounds?.minX ?: 0f
    val minY get() = bounds?.minY ?: 0f
    val maxX get() = bounds?.maxX ?: 0f
    val maxY get() = bounds?.maxY ?: 0f

    fun query(left: Float, top: Float, right: Float, bottom: Float): FoliaQuery {
        if (left > right || top > bottom) return FoliaQuery(emptyList(), 0)
        val result = index.query(left - halfWidth, right + halfWidth, top - halfHeight, bottom + halfHeight)
        return FoliaQuery(
            result.placements.filter {
                it.centerX - it.width / 2 < right && it.centerX + it.width / 2 > left &&
                    it.centerY - it.height / 2 < bottom && it.centerY + it.height / 2 > top
            },
            result.candidatesExamined,
        )
    }

    fun constrain(transform: FoliaTransform): FoliaTransform {
        val pan = clampWallPan(transform.x, transform.y, 1f, bounds)
        return transform.copy(x = pan.x, y = pan.y)
    }

    companion object {
        fun create(
            ids: List<Long>,
            focus: Long? = null,
            shape: FoliaFocusShape = FoliaFocusShape.FullCover,
            size: FoliaFocusSize = FoliaFocusSize.Medium,
            aspect: Float? = null,
        ) = FoliaWallScene(ids, focus, FoliaStyle(shape = shape, size = size), aspect, 1.5f)
    }
}

internal data class FoliaTransform(val x: Float = 0f, val y: Float = 0f, val zoom: Float = 1f) {
    fun gesture(cx: Float, cy: Float, panX: Float, panY: Float, factor: Float, unit: Float): FoliaTransform {
        if (listOf(cx, cy, panX, panY, factor, unit).any { !it.isFinite() } || factor <= 0f || unit <= 0f) return this
        val nextZoom = (zoom * factor).coerceIn(0.72f, 1.35f)
        return FoliaTransform(
            x + cx / unit * (1f / nextZoom - 1f / zoom) + panX / (unit * nextZoom),
            y + cy / unit * (1f / nextZoom - 1f / zoom) + panY / (unit * nextZoom),
            nextZoom,
        )
    }
}

/** Global visual choices; per-category saved state contains only pan/focus. */
internal data class FoliaStyle(
    val shape: FoliaFocusShape = FoliaFocusShape.FullCover,
    val size: FoliaFocusSize = FoliaFocusSize.Medium,
    val density: Int = 100,
)
