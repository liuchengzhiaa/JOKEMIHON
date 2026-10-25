package eu.kanade.presentation.library.components

/** JOKEMIHON: pack measured chips into lazy rows without reordering or composing offscreen chips. */
internal fun categoryWallRows(widths: List<Int>, availableWidth: Int, spacing: Int): List<IntRange> {
    require(availableWidth > 0)
    require(spacing >= 0)
    if (widths.isEmpty()) return emptyList()

    val rows = mutableListOf<IntRange>()
    var start = 0
    var used = 0L
    widths.forEachIndexed { index, measuredWidth ->
        val width = measuredWidth.coerceIn(1, availableWidth).toLong()
        val nextWidth = if (index == start) width else used + spacing + width
        if (nextWidth > availableWidth && index > start) {
            rows.add(start until index)
            start = index
            used = width
        } else {
            used = nextWidth
        }
    }
    rows.add(start until widths.size)
    return rows
}
