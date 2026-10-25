package eu.kanade.presentation.library.components

import kotlin.random.Random

/** Standalone checks for the production row packer; not a substitute for Android/JUnit/UI tests. */
fun main() {
    check(categoryWallRows(emptyList(), 300, 8).isEmpty())
    check(categoryWallRows(listOf(100, 100, 100, 50), 220, 8) == listOf(0..1, 2..3))
    check(categoryWallRows(listOf(100, 100, 50), 208, 8) == listOf(0..1, 2..2))
    check(categoryWallRows(listOf(80, 10000, 80), 200, 8) == listOf(0..0, 1..1, 2..2))
    check(categoryWallRows(listOf(0, 0, 0), 3, 0) == listOf(0..2))
    check(categoryWallRows(listOf(Int.MAX_VALUE, Int.MAX_VALUE), Int.MAX_VALUE, 8) == listOf(0..0, 1..1))
    check(runCatching { categoryWallRows(listOf(1), 0, 8) }.exceptionOrNull() is IllegalArgumentException)
    check(runCatching { categoryWallRows(listOf(1), 20, -1) }.exceptionOrNull() is IllegalArgumentException)

    fun verify(widths: List<Int>, available: Int, spacing: Int) {
        val rows = categoryWallRows(widths, available, spacing)
        check(rows.flatMap { it.toList() } == widths.indices.toList())
        rows.forEach { row ->
            val used = row.sumOf { widths[it].coerceIn(1, available).toLong() } + (row.count() - 1L) * spacing
            check(used <= available)
        }
    }
    verify(List(10000) { 40 + it % 200 }, 360, 8)
    val random = Random(20261025)
    repeat(500) {
        verify(List(random.nextInt(200)) { random.nextInt(0, 2000) }, random.nextInt(1, 1000), random.nextInt(32))
    }
    println("Category wall row smoke: 8 boundary checks, 10000-category case and 500 seeded property cases PASS")
}
