package tachiyomi.source.local.work

import eu.kanade.tachiyomi.util.lang.compareToCaseInsensitiveNaturalOrder
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Presentation-only overrides keyed by original physical names, never a chain of filename guesses. */
@Serializable
data class LocalAuthorRule(val id: String, val name: String, val directories: List<String>) {
    init {
        require(id.matches(Regex("[A-Za-z0-9_-]{1,64}")))
        require(name.isNotBlank() && name.length <= 1024)
        require(directories.isNotEmpty() && directories.distinct().size == directories.size)
        directories.forEach { LocalWorkPath(it, "validation") }
    }
}

@Serializable
data class LocalAuthorRules(val rules: List<LocalAuthorRule> = emptyList(), val version: Int = 1) {
    init {
        require(version == 1 && rules.size <= 10000)
        require(rules.map { it.id }.distinct().size == rules.size)
        val paths = rules.flatMap { it.directories }
        require(paths.size <= 10000 && paths.distinct().size == paths.size)
    }

    fun applyTo(automatic: List<LocalAuthorGroup>): List<LocalAuthorGroup> {
        val available = automatic.flatMap { it.directories }.toSet()
        val assigned = rules.flatMap { it.directories }.toSet()
        val manual = rules.mapNotNull { rule ->
            val present = rule.directories.filter { it in available }
            if (present.isEmpty()) null else LocalAuthorGroup("manual:${rule.id}", rule.name, present)
        }
        val remaining = automatic.mapNotNull { group ->
            val unassigned = group.directories.filterNot { it in assigned }
            if (unassigned.isEmpty()) null else group.copy(directories = unassigned)
        }
        return (manual + remaining).sortedWith { a, b ->
            a.name.compareToCaseInsensitiveNaturalOrder(b.name)
        }
    }

    fun save(selected: List<LocalAuthorGroup>, name: String, id: String): LocalAuthorRules {
        require(selected.isNotEmpty() && selected.map { it.key }.distinct().size == selected.size)
        val selectedIds = selected.mapNotNull {
            it.key.takeIf { key -> key.startsWith("manual:") }?.removePrefix("manual:")
        }.toSet()
        // Retain temporarily missing folders when a manual group is merged/renamed.
        val byKey = rules.associateBy { "manual:${it.id}" }
        val paths = selected.flatMap { group ->
            byKey[group.key]?.directories ?: group.directories
        }.distinct()
        val directories = paths.toSet()
        val retained = rules.filterNot { it.id in selectedIds || it.directories.any(directories::contains) }
        return LocalAuthorRules(retained + LocalAuthorRule(id, name.trim(), paths))
    }

    fun dissolve(keys: Set<String>): LocalAuthorRules = copy(rules = rules.filterNot { "manual:${it.id}" in keys })
    fun encode(): String = Json.encodeToString(serializer(), this).also {
        require(it.length <= MAX_LENGTH) { "Local directory rules are too large" }
    }

    companion object {
        private const val MAX_LENGTH = 4 * 1024 * 1024
        fun decode(value: String): LocalAuthorRules {
            require(value.length <= MAX_LENGTH) { "Local directory rules are too large" }
            return if (value.isBlank()) LocalAuthorRules() else Json.decodeFromString(serializer(), value)
        }
    }
}
