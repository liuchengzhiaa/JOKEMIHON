package mihon.feature.library

import eu.kanade.presentation.library.components.FoliaTransform
import eu.kanade.presentation.library.components.FoliaWallScene
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.library.model.LibraryDisplayMode
import kotlin.math.abs

class FoliaWallSceneTest {
    @Test
    fun `eight slots tile one block without overlap`() {
        val scene = FoliaWallScene.create((0L..7L).toList())
        scene.tiles.sumOf { (it.width * it.height).toDouble() } shouldBe 64.0
        scene.tiles.forEach { a ->
            scene.tiles.filter { it.id != a.id }.forEach { b ->
                (a.x < b.x + b.width && a.x + a.width > b.x && a.y < b.y + b.height && a.y + a.height > b.y) shouldBe false
            }
        }
    }

    @Test
    fun `forty thousand entries have stable unique two dimensional placements`() {
        val ids = (1L..40_000L).toList()
        val a = FoliaWallScene.create(ids)
        val b = FoliaWallScene.create(ids)
        a.tiles shouldBe b.tiles
        a.tiles.map { it.id } shouldBe ids
        a.tiles.map { it.x to it.y }.toSet().size shouldBe ids.size
        (a.minX < -8f && a.maxX > 8f && a.minY < -8f && a.maxY > 8f) shouldBe true
    }

    @Test
    fun `viewport query examines nearby buckets not entire library`() {
        val scene = FoliaWallScene.create((1L..40_000L).toList())
        val query = scene.query(-2f, -2f, 10f, 10f)
        (query.examined <= 72) shouldBe true
        query.tiles.toSet() shouldBe scene.tiles.filter {
            it.x < 10f && it.x + it.width > -2f && it.y < 10f && it.y + it.height > -2f
        }.toSet()
    }

    @Test
    fun `empty single partial and invalid query are safe`() {
        FoliaWallScene.create(emptyList()).query(0f, 0f, 8f, 8f).tiles shouldBe emptyList()
        for (count in 1..19) {
            val scene = FoliaWallScene.create((1L..count.toLong()).toList())
            scene.tiles.size shouldBe count
            scene.query(scene.minX - 1, scene.minY - 1, scene.maxX + 1, scene.maxY + 1).tiles.size shouldBe count
            scene.query(Float.NaN, 0f, 8f, 8f).tiles shouldBe emptyList()
            scene.query(10f, 0f, -10f, 8f).tiles shouldBe emptyList()
        }
    }

    @Test
    fun `pinch preserves the world coordinate under the fingers`() {
        val start = FoliaTransform(3f, -2f, 1f)
        val next = start.gesture(100f, -80f, 0f, 0f, 1.4f, 50f)
        val beforeX = 4 + 100f / (50f * start.zoom) - start.x
        val afterX = 4 + 100f / (50f * next.zoom) - next.x
        val beforeY = 4 - 80f / (50f * start.zoom) - start.y
        val afterY = 4 - 80f / (50f * next.zoom) - next.y
        (abs(beforeX - afterX) < 0.0001f && abs(beforeY - afterY) < 0.0001f) shouldBe true
    }

    @Test
    fun `pan zoom bounds and invalid gesture stay finite`() {
        val start = FoliaTransform()
        start.gesture(0f, 0f, 50f, -100f, 1f, 50f) shouldBe FoliaTransform(1f, -2f, 1f)
        start.gesture(0f, 0f, 0f, 0f, 100f, 50f).zoom shouldBe 1.8f
        start.gesture(0f, 0f, 0f, 0f, 0.01f, 50f).zoom shouldBe 0.6f
        start.gesture(Float.NaN, 0f, 0f, 0f, 1f, 50f) shouldBe start
        FoliaWallScene.create(listOf(1L)).constrain(FoliaTransform(1000f, -1000f)).let {
            it.x shouldBe 2.5f
            it.y shouldBe -2.5f
        }
    }

    @Test
    fun `new layout has stable serialization and keeps old layout values`() {
        val saved = listOf("COMPACT_GRID", "COMFORTABLE_GRID", "LIST", "COVER_ONLY_GRID", "FOLIA_WALL")
        saved.forEach { LibraryDisplayMode.deserialize(it).serialize() shouldBe it }
        LibraryDisplayMode.values.size shouldBe 5
        LibraryDisplayMode.deserialize("unknown") shouldBe LibraryDisplayMode.CompactGrid
        LibraryDisplayMode.default shouldBe LibraryDisplayMode.CompactGrid
    }
}
