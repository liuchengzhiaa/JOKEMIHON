package eu.kanade.presentation.library.components

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CategoryWallRowsTest {
    @Test
    fun `empty categories produce no rows`() {
        categoryWallRows(emptyList(), 300, 8) shouldBe emptyList()
    }

    @Test
    fun `chips wrap in original order and include spacing`() {
        categoryWallRows(listOf(100, 100, 100, 50), 220, 8) shouldBe listOf(0..1, 2..3)
    }

    @Test
    fun `exact fit stays on the same row`() {
        categoryWallRows(listOf(100, 100, 50), 208, 8) shouldBe listOf(0..1, 2..2)
    }

    @Test
    fun `long names are capped to a row without losing adjacent categories`() {
        categoryWallRows(listOf(80, 10000, 80), 200, 8) shouldBe listOf(0..0, 1..1, 2..2)
    }

    @Test
    fun `zero widths and large arithmetic do not overflow`() {
        categoryWallRows(listOf(0, 0, 0), 3, 0) shouldBe listOf(0..2)
        categoryWallRows(listOf(Int.MAX_VALUE, Int.MAX_VALUE), Int.MAX_VALUE, 8) shouldBe listOf(0..0, 1..1)
    }

    @Test
    fun `large category lists have no missing duplicated or reordered indices`() {
        val widths = List(10000) { 40 + it % 200 }
        val rows = categoryWallRows(widths, 360, 8)
        rows.flatMap { it.toList() } shouldBe widths.indices.toList()
        rows.forEach { row ->
            (row.sumOf { widths[it] } + (row.count() - 1) * 8 <= 360) shouldBe true
        }
    }

    @Test
    fun `invalid container dimensions are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { categoryWallRows(listOf(1), 0, 8) }
        assertThrows(IllegalArgumentException::class.java) { categoryWallRows(listOf(1), 20, -1) }
    }
}
