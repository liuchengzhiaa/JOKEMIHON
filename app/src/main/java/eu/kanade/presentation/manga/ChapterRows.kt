package eu.kanade.presentation.manga

import eu.kanade.tachiyomi.ui.manga.ChapterList

/** Missing-count markers stay full-width, ordering and chapter identities are never changed. */
internal fun chapterRows(chapters: List<ChapterList>, width: Int): List<List<ChapterList>> = buildList {
    require(width in 1..6)
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

