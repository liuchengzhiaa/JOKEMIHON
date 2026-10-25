package tachiyomi.source.local.work

import eu.kanade.tachiyomi.util.lang.compareToCaseInsensitiveNaturalOrder

/** JOKEMIHON: exact leading ASCII digit string + first subsequent Unicode letter, by owner decision. */
data class LocalAuthorGroup(val key: String, val name: String, val directories: List<String>)

fun localAuthorKey(name: String): String? {
    val digits = name.takeWhile { it in '0'..'9' }
    if (digits.isEmpty()) return null
    var offset = digits.length
    while (offset < name.length) {
        val cp = name.codePointAt(offset)
        if (Character.isLetter(cp)) return "author:$digits:${String(Character.toChars(cp))}"
        offset += Character.charCount(cp)
    }
    return null
}

fun groupLocalAuthors(directories: List<String>): List<LocalAuthorGroup> = directories
    .asSequence()
    .filter { it.isNotBlank() && !it.startsWith('.') }
    .distinct()
    .sortedWith { a, b -> a.compareToCaseInsensitiveNaturalOrder(b) }
    .groupBy { localAuthorKey(it) ?: "directory:$it" }
    .map { (key, names) ->
        val displayName = if (key.startsWith("author:")) {
            names.first().trimEnd { it in '0'..'9' || it.isWhitespace() }
        } else {
            names.first()
        }
        LocalAuthorGroup(key, displayName, names)
    }
