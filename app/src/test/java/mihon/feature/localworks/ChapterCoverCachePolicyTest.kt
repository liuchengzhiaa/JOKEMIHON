package mihon.feature.localworks

import eu.kanade.tachiyomi.data.coil.ChapterCoverRequest
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.source.local.image.LocalChapterCoverManager

class ChapterCoverCachePolicyTest {
    @Test
    fun `cache identity separates storage chapters and persisted generations`() {
        val request = ChapterCoverRequest("author/part.zip", "rootA", 9)
        val keys = listOf(request, request.copy(root = "rootB"), request.copy(chapterUrl = "author/other.zip"), request.copy(revision = 10)).map { it.cacheKey }
        keys.toSet().size shouldBe 4
    }

    @Test
    fun `recreating a request with the same stored generation keeps disk key`() {
        ChapterCoverRequest("author/part.zip", "rootA", 9).cacheKey shouldBe
            ChapterCoverRequest("author/part.zip", "rootA", 9).cacheKey
    }

    @Test
    fun `cover lookup filters exact basename including extensionless and upper case names`() {
        LocalChapterCoverManager.isCoverName("COVER.JPG", "cover") shouldBe true
        LocalChapterCoverManager.isCoverName("cover", "cover") shouldBe true
        LocalChapterCoverManager.isCoverName("part.cover.png", "part.cover") shouldBe true
        LocalChapterCoverManager.isCoverName("001.jpg", "cover") shouldBe false
        LocalChapterCoverManager.isCoverName("cover-other.jpg", "cover") shouldBe false
    }

    @Test
    fun `known page extensions do not need a header query for every image`() {
        LocalChapterCoverManager.isKnownImageName("001.JPG") shouldBe true
        LocalChapterCoverManager.isKnownImageName("001.jpeg") shouldBe true
        LocalChapterCoverManager.isKnownImageName("ComicInfo.xml") shouldBe false
    }
}
