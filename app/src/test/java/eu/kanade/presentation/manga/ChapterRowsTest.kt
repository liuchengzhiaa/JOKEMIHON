package eu.kanade.presentation.manga

import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.manga.ChapterList
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

class ChapterRowsTest {
    private fun item(id: Long) = ChapterList.Item(Chapter.create().copy(id = id), Download.State.NOT_DOWNLOADED)

    @Test
    fun `grid preserves chapter identity order and incomplete last row`() {
        val chapters = (1L..5L).map(::item)
        val rows = chapterRows(chapters, 2)
        rows.map { it.size } shouldBe listOf(2, 2, 1)
        rows.flatten() shouldBe chapters
    }

    @Test
    fun `missing count remains its own full width row`() {
        val marker = ChapterList.MissingCount("gap", 2)
        val chapters = listOf(item(1), marker, item(2), item(3))
        chapterRows(chapters, 2) shouldBe listOf(listOf(chapters[0]), listOf(marker), chapters.drop(2))
    }

    @Test
    fun `list and empty chapter lists do not add placeholders`() {
        val chapters = listOf(item(1), item(2))
        chapterRows(chapters, 1) shouldBe chapters.map { listOf(it) }
        chapterRows(emptyList(), 2) shouldBe emptyList()
    }
}
