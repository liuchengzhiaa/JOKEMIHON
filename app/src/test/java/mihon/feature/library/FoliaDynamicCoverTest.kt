package mihon.feature.library

import eu.kanade.presentation.library.components.FoliaStyle
import eu.kanade.presentation.library.components.FoliaWallScene
import eu.kanade.presentation.library.components.folia.FoliaFocusShape
import eu.kanade.presentation.library.components.folia.FoliaFocusSize
import eu.kanade.presentation.library.components.folia.representativeCoverArgbs
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import kotlin.math.abs

class FoliaDynamicCoverTest {
    private val ids = (1L..48L).toList()

    @Test
    fun `obsolete large geometry work cancels before walking the whole library`() {
        var checks = 0
        assertThrows(CancellationException::class.java) {
            FoliaWallScene((1L..40_000L).toList(), null, FoliaStyle(), null, 1.5f) {
                checks++
                if (checks == 4) throw CancellationException("obsolete wall request")
            }
        }
        checks shouldBe 4
    }

    @Test
    fun `full cover is the default instead of fitting into a fixed square`() {
        FoliaStyle().shape shouldBe FoliaFocusShape.FullCover
        val portrait = FoliaWallScene.create(ids, 1L, aspect = 0.65f).byId.getValue(1L)
        val landscape = FoliaWallScene.create(ids, 1L, aspect = 1.65f).byId.getValue(1L)
        (portrait.width < portrait.height) shouldBe true
        (landscape.width > landscape.height) shouldBe true
        portrait.fitArtwork shouldBe true
        landscape.fitArtwork shouldBe true
        (abs(portrait.width / portrait.height - 0.65f) < 0.1f) shouldBe true
        (abs(landscape.width / landscape.height - 1.65f) < 0.1f) shouldBe true
    }

    @Test
    fun `focus really reflows its neighbours rather than layering a scaled original tile`() {
        val base = FoliaWallScene.create(ids)
        val focus = FoliaWallScene.create(ids, 1L, aspect = 0.65f)
        val hero = focus.byId.getValue(1L)
        (hero.width * hero.height > base.byId.getValue(1L).let { it.width * it.height }) shouldBe true
        focus.tiles.filter {
            it.blockIndex != hero.blockIndex
        }.forEach {
            it shouldBe base.byId.getValue(it.itemId.toLong())
        }
        focus.tiles.filter {
            it.blockIndex == hero.blockIndex &&
                it.itemId != "1"
        }.any { it != base.byId[it.itemId.toLong()] } shouldBe true
    }

    @Test
    fun `zero gutter covers the whole block and every focus size stays valid`() {
        FoliaFocusSize.entries.forEach { size ->
            val scene = FoliaWallScene.create(ids, 1L, size = size, aspect = 0.65f)
            val block = scene.tiles.filter { it.blockIndex == 0 }
            block.sumOf { (it.width * it.height).toDouble() } shouldBe 96.0
        }
    }

    @Test
    fun `full cover geometry changes again when the decoded artwork aspect arrives`() {
        val pending = FoliaWallScene.create(ids, 1L)
        val loaded = FoliaWallScene.create(ids, 1L, aspect = 0.65f)
        (pending.byId.getValue(1L).width / pending.byId.getValue(1L).height == 1f) shouldBe true
        (loaded.byId.getValue(1L).width / loaded.byId.getValue(1L).height < 0.8f) shouldBe true
    }

    @Test
    fun `palette ignores transparent white and black padding`() {
        val red = 0xffb44c58.toInt()
        val pixels = IntArray(400) {
            when {
                it < 100 -> 0x00ffffff
                it < 200 -> 0xffffffff.toInt()
                it < 300 -> 0xff000000.toInt()
                else -> red
            }
        }
        representativeCoverArgbs(pixels, balance = false) shouldBe listOf(red)
    }

    @Test
    fun `palette and geometry are deterministic without keeping bitmap instances`() {
        val pixels = IntArray(4096) { if (it % 3 == 0) 0xff4466aa.toInt() else 0xffb44c58.toInt() }
        representativeCoverArgbs(pixels) shouldBe representativeCoverArgbs(pixels)
        (representativeCoverArgbs(pixels).size in 1..4) shouldBe true
    }
}
