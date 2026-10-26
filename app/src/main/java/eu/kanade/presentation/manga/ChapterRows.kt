package eu.kanade.presentation.manga

import eu.kanade.tachiyomi.ui.manga.ChapterList

/** Missing-count markers stay full-width, ordering and chapter identities are never changed. */
internal fun chapterRows(chapters: List<ChapterList>, width: Int): List<List<ChapterList>> = buildList {
    require(width in 1..10)
    var pending = mutableListOf<ChapterList>()
    for (chapter in chapters) {
        if (chapter is ChapterList.MissingCount) {
            if (pending.isNotEmpty()) add(pending)
            add(listOf(chapter))
            pending = mutableListOf()
        } else {
            pending.add(chapter)
            if (pending.size == width) {
                add(pending)
                pending = mutableListOf()
            }
        }
    }
    if (pending.isNotEmpty()) add(pending)
}


/** Same 128dp adaptive cell minimum, 8dp outer padding and 4dp gaps as LazyLibraryGrid. */
internal fun chapterGridColumns(availableWidthDp: Float, requested: Int): Int =
    if (requested in 1..10) requested else ((availableWidthDp - 16f + 4f) / 132f).toInt().coerceIn(1, 10)
