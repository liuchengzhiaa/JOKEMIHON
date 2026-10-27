package eu.kanade.presentation.library.components

import kotlin.math.floor

/** Independently authored mosaic geometry; no media queries or Compose state. Units are 1/8 of a block. */
internal data class FoliaTile(val index: Int, val id: Long, val x: Float, val y: Float, val width: Float, val height: Float)

internal data class FoliaQuery(val tiles: List<FoliaTile>, val examined: Int)

internal class FoliaWallScene private constructor(
    val ids: List<Long>,
    val tiles: List<FoliaTile>,
    private val blocks: Map<Long, List<FoliaTile>>,
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float,
) {
    /** Only inspect intersecting 2D blocks, never scan the whole library on a drag frame. */
    fun query(left: Float, top: Float, right: Float, bottom: Float): FoliaQuery {
        if (!left.isFinite() || !top.isFinite() || !right.isFinite() || !bottom.isFinite() ||
            left > right || top > bottom || right < minX || bottom < minY || left > maxX || top > maxY
        ) return FoliaQuery(emptyList(), 0)
        val result = ArrayList<FoliaTile>()
        var examined = 0
        for (y in cell(top.coerceAtLeast(minY))..cell(bottom.coerceAtMost(maxY))) {
            for (x in cell(left.coerceAtLeast(minX))..cell(right.coerceAtMost(maxX))) {
                blocks[key(x, y)]?.forEach { tile ->
                    examined++
                    if (tile.x < right && tile.x + tile.width > left && tile.y < bottom && tile.y + tile.height > top) {
                        result += tile
                    }
                }
            }
        }
        return FoliaQuery(result, examined)
    }

    fun constrain(transform: FoliaTransform): FoliaTransform = transform.copy(
        x = transform.x.coerceIn(5.5f - maxX, 2.5f - minX),
        y = transform.y.coerceIn(5.5f - maxY, 2.5f - minY),
    )

    companion object {
        private fun cell(value: Float) = floor(value / 8.0).toInt()
        private fun key(x: Int, y: Int) = (x.toLong() shl 32) xor (y.toLong() and 0xffffffffL)

        fun create(ids: List<Long>): FoliaWallScene {
            // An exact 8x8 partition, alternating reflections. Order always comes from library sorting.
            val pattern = listOf(
                floatArrayOf(0f, 0f, 3f, 4f), floatArrayOf(3f, 0f, 2f, 3f),
                floatArrayOf(5f, 0f, 3f, 3f), floatArrayOf(3f, 3f, 3f, 3f),
                floatArrayOf(6f, 3f, 2f, 3f), floatArrayOf(0f, 4f, 3f, 4f),
                floatArrayOf(3f, 6f, 2f, 2f), floatArrayOf(5f, 6f, 3f, 2f),
            )
            val tiles = ArrayList<FoliaTile>(ids.size)
            val blocks = HashMap<Long, List<FoliaTile>>()
            var bx = 0
            var by = 0
            var dx = 0
            var dy = -1
            ids.indices.step(8).forEach { start ->
                val group = (start until minOf(start + 8, ids.size)).map { index ->
                    val slot = pattern[index - start]
                    val x = if ((start / 8) % 2 == 0) slot[0] else 8f - slot[0] - slot[2]
                    FoliaTile(index, ids[index], bx * 8f + x, by * 8f + slot[1], slot[2], slot[3])
                }
                blocks[key(bx, by)] = group
                tiles += group
                // Square spiral keeps the first groups close in both axes, without an endless vertical strip.
                if (bx == by || (bx < 0 && bx == -by) || (bx > 0 && bx == 1 - by)) {
                    val previous = dx
                    dx = -dy
                    dy = previous
                }
                bx += dx
                by += dy
            }
            return FoliaWallScene(
                ids.toList(), tiles, blocks,
                tiles.minOfOrNull { it.x } ?: 0f, tiles.minOfOrNull { it.y } ?: 0f,
                (tiles.maxOfOrNull { it.x + it.width } ?: 8f).coerceAtLeast(8f),
                (tiles.maxOfOrNull { it.y + it.height } ?: 8f).coerceAtLeast(8f),
            )
        }
    }
}

internal data class FoliaTransform(val x: Float = 0f, val y: Float = 0f, val zoom: Float = 1f) {
    /** Centroid is relative to viewport centre; preserve its world coordinate when pinching. */
    fun gesture(cx: Float, cy: Float, panX: Float, panY: Float, factor: Float, unit: Float): FoliaTransform {
        if (listOf(cx, cy, panX, panY, factor, unit).any { !it.isFinite() } || factor <= 0f || unit <= 0f) return this
        val nextZoom = (zoom * factor).coerceIn(0.6f, 1.8f)
        return FoliaTransform(
            x + cx / unit * (1f / nextZoom - 1f / zoom) + panX / (unit * nextZoom),
            y + cy / unit * (1f / nextZoom - 1f / zoom) + panY / (unit * nextZoom),
            nextZoom,
        )
    }
}
