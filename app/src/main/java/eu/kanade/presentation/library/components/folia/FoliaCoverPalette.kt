package eu.kanade.presentation.library.components.folia

import kotlin.math.max

private const val MAXIMUM_SAMPLED_PIXELS = 4096

private const val MAXIMUM_PALETTE_COLORS = 4
private const val MINIMUM_PALETTE_DISTANCE_SQUARED = 42 * 42

private data class CoverColorBucket(
    var count: Int = 0,
    var red: Long = 0,
    var green: Long = 0,
    var blue: Long = 0,
) {
    fun averageArgb(): Int {
        val averageRed = (red / count).toInt()
        val averageGreen = (green / count).toInt()
        val averageBlue = (blue / count).toInt()
        return opaqueArgb(averageRed, averageGreen, averageBlue)
    }

    fun score(): Long {
        val averageRed = (red / count).toInt()
        val averageGreen = (green / count).toInt()
        val averageBlue = (blue / count).toInt()
        val saturation = maxOf(averageRed, averageGreen, averageBlue) -
            minOf(averageRed, averageGreen, averageBlue)
        return count.toLong() * (96 + saturation)
    }
}

/**
 * Finds a colour family rather than averaging the entire image into grey. RGB
 * values are quantised into a small histogram and saturation slightly increases
 * a bucket's score. Very transparent, nearly black and nearly white pixels are
 * ignored because they usually describe padding or typography rather than art.
 */
internal fun dominantCoverArgb(pixels: IntArray): Int? {
    return representativeCoverArgbs(pixels, 1).firstOrNull()
}

/**
 * Builds a small, deterministic palette from a bounded pixel sample.
 *
 * Population still matters most, so a tiny saturated logo cannot replace the
 * colour that visually owns the cover. Once the strongest family is selected,
 * nearby buckets are skipped so the remaining entries can drive visibly
 * different ambient lights. Transparent, near-black and near-white pixels are
 * ignored for the same reason as [dominantCoverArgb].
 */
internal fun representativeCoverArgbs(
    pixels: IntArray,
    maxColors: Int = MAXIMUM_PALETTE_COLORS,
    balance: Boolean = true,
): List<Int> {
    if (pixels.isEmpty() || maxColors <= 0) return emptyList()

    val buckets = HashMap<Int, CoverColorBucket>(128)
    val step = max(1, pixels.size / MAXIMUM_SAMPLED_PIXELS)
    var index = 0
    while (index < pixels.size) {
        val pixel = pixels[index]
        val alpha = pixel ushr 24 and 0xFF
        val red = pixel ushr 16 and 0xFF
        val green = pixel ushr 8 and 0xFF
        val blue = pixel and 0xFF
        val luminance = (red * 299 + green * 587 + blue * 114) / 1_000
        if (alpha >= 160 && luminance in 22..242) {
            val key = (red shr 4 shl 8) or (green shr 4 shl 4) or (blue shr 4)
            val bucket = buckets.getOrPut(key) { CoverColorBucket() }
            bucket.count++
            bucket.red += red
            bucket.green += green
            bucket.blue += blue
        }
        index += step
    }

    val candidates = buckets.values
        .sortedWith(
            compareByDescending<CoverColorBucket>(CoverColorBucket::score)
                .thenByDescending(CoverColorBucket::count)
                .thenBy(CoverColorBucket::averageArgb),
        )
        .map(CoverColorBucket::averageArgb)

    val selected = ArrayList<Int>(maxColors.coerceAtMost(candidates.size))
    candidates.forEach { candidate ->
        if (selected.size >= maxColors) return@forEach
        if (selected.all { selectedColor ->
                colorDistanceSquared(
                    candidate,
                    selectedColor,
                ) >= MINIMUM_PALETTE_DISTANCE_SQUARED
            }
        ) {
            selected += candidate
        }
    }
    return if (balance) selected.map(::balanceCoverColor) else selected
}

/** Keeps very dark/bright swatches useful as UI light without erasing their hue. */
private fun balanceCoverColor(color: Int): Int {
    var red = color ushr 16 and 0xFF
    var green = color ushr 8 and 0xFF
    var blue = color and 0xFF
    val luminance = (red * 299 + green * 587 + blue * 114) / 1_000
    if (luminance < 80) {
        red = mixChannel(red, 255, 0.28f)
        green = mixChannel(green, 255, 0.28f)
        blue = mixChannel(blue, 255, 0.28f)
    } else if (luminance > 218) {
        red = mixChannel(red, 0, 0.16f)
        green = mixChannel(green, 0, 0.16f)
        blue = mixChannel(blue, 0, 0.16f)
    }
    return opaqueArgb(red, green, blue)
}

private fun colorDistanceSquared(first: Int, second: Int): Int {
    val red = (first ushr 16 and 0xFF) - (second ushr 16 and 0xFF)
    val green = (first ushr 8 and 0xFF) - (second ushr 8 and 0xFF)
    val blue = (first and 0xFF) - (second and 0xFF)
    return red * red + green * green + blue * blue
}

private fun opaqueArgb(red: Int, green: Int, blue: Int): Int =
    (0xFF shl 24) or (red shl 16) or (green shl 8) or blue

private fun mixChannel(source: Int, target: Int, fraction: Float): Int =
    (source + (target - source) * fraction).toInt().coerceIn(0, 255)
