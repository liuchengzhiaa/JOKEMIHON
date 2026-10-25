package mihon.feature.localworks

import tachiyomi.source.local.work.LocalWorkDefinition
import tachiyomi.source.local.work.LocalWorkPath

/** JOKEMIHON: selection order is explicit; reselecting an item appends it, never sorts it. */
fun toggleLocalWorkSelection(selected: List<LocalWorkPath>, work: LocalWorkPath): List<LocalWorkPath> =
    if (work in selected) {
        selected - work
    } else {
        require(selected.size < LocalWorkDefinition.MAX_MEMBERS)
        selected + work
    }

fun moveLocalWorkSelection(selected: List<LocalWorkPath>, index: Int, delta: Int): List<LocalWorkPath> {
    val destination = index + delta
    require(index in selected.indices && destination in selected.indices)
    return selected.toMutableList().apply { add(destination, removeAt(index)) }
}

fun localCollectionNumeral(count: Int): String {
    require(count in 1..LocalWorkDefinition.MAX_MEMBERS)
    val digits = "零一二三四五六七八九"
    val units = listOf(1000 to "千", 100 to "百", 10 to "十", 1 to "")
    var rest = count
    var gap = false
    return buildString {
        for ((unit, suffix) in units) {
            val digit = rest / unit
            rest %= unit
            if (digit == 0) {
                if (isNotEmpty() && rest > 0) gap = true
                continue
            }
            if (gap) append('零')
            gap = false
            if (!(unit == 10 && digit == 1 && isEmpty())) append(digits[digit])
            append(suffix)
        }
    }
}
