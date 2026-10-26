package mihon.feature.localworks

import com.hippo.unifile.UniFile
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import tachiyomi.source.local.image.LocalChapterCoverManager

class ChapterCoverReadingFilesTest {
    private fun file(filename: String) = mockk<UniFile> { every { name } returns filename }

    @Test
    fun `preexisting cover pages retain their reading position`() {
        val files = listOf(file("001.jpg"), file("cover.jpg"))
        LocalChapterCoverManager.readingFiles(files) shouldBe files
    }

    @Test
    fun `new generated cover copy is not an extra reading page`() {
        val page = file("001.jpg")
        val files = listOf(page, file("cover.png"), file(".jokemihon-chapter-cover.cover.png.marker"))
        LocalChapterCoverManager.readingFiles(files) shouldBe listOf(page)
    }

    @Test
    fun `hidden backups and temporary files cannot become pages`() {
        val page = file("001.jpg")
        val files = listOf(page, file(".jokemihon-cover-backup.bin"), file(".jokemihon-cover.tmp"))
        LocalChapterCoverManager.readingFiles(files) shouldBe listOf(page)
    }
}
