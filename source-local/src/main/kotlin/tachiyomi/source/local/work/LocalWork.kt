package tachiyomi.source.local.work

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import java.security.MessageDigest
import java.util.Base64

/** JOKEMIHON: references only. No document is moved, copied, renamed or rewritten. */
@Serializable
data class LocalWorkPath(val directory: String, val entry: String) {
    init {
        require(validSegment(directory) && validSegment(entry)) { "Invalid local work path" }
    }

    val relativeUrl: String get() = "$directory/$entry"
    val displayName: String get() = if (entry.substringAfterLast('.', "").lowercase() in ARCHIVE_EXTENSIONS) {
        entry.substringBeforeLast('.')
    } else {
        entry
    }

    companion object {
        private val ARCHIVE_EXTENSIONS = setOf("zip", "cbz", "rar", "cbr", "7z", "cb7", "tar", "cbt", "epub")

        fun fromRelativeUrl(url: String): LocalWorkPath {
            val parts = url.split('/')
            require(parts.size == 2) { "Invalid local chapter path" }
            return LocalWorkPath(parts[0], parts[1])
        }

        private fun validSegment(value: String): Boolean = value.isNotBlank() && value.length <= 1024 &&
            value != "." && value != ".." && !value.startsWith('.') &&
            value.none { it == '/' || it == '\\' || it == '\u0000' }
    }
}

@Serializable
data class LocalWorkDefinition(
    val members: List<LocalWorkPath>,
    val version: Int = 1,
    val authorKey: String? = null,
) {
    init {
        require(version == 1 && members.size in 1..MAX_MEMBERS && members.distinct().size == members.size) {
            "Invalid local work definition"
        }
        require(authorKey == null || (authorKey.isNotBlank() && authorKey.length <= 2048 && '\u0000' !in authorKey))
    }

    // Ordered, length-independent identity; duplicate basenames in separate directories stay distinct.
    val url: String get() {
        // Author identity survives newly added files; old ordered-work identities remain byte-for-byte unchanged.
        val canonical = authorKey ?: members.joinToString("\u0000") { "${it.directory}\u0000${it.entry}" }
        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
        val prefix = if (authorKey == null) URL_PREFIX else URL_PREFIX + "author-"
        return prefix + digest.joinToString("") { "%02x".format(it) }
    }

    fun toMemo(existing: JsonObject = JsonObject(emptyMap())): JsonObject =
        JsonObject(existing + (MEMO_KEY to Json.encodeToJsonElement(this)))

    companion object {
        const val MAX_MEMBERS = 1000
        const val URL_PREFIX = "@jokemihon-work/"
        const val MEMO_KEY = "app.jokemihon.localWork.v1"

        fun isWorkUrl(url: String): Boolean = url.startsWith(URL_PREFIX)

        fun fromMemo(url: String, memo: JsonObject): LocalWorkDefinition {
            require(isWorkUrl(url))
            val value = memo[MEMO_KEY] ?: error("Local work definition is missing")
            return Json.decodeFromJsonElement<LocalWorkDefinition>(value).also {
                require(it.url == url) { "Local work identity mismatch" }
            }
        }
    }
}

/** Page index follows the same natural image ordering as the local reader. Portable in normal backups. */
@Serializable
data class LocalWorkCover(val path: LocalWorkPath, val page: Int = 0) {
    init {
        require(page in 0..100000)
    }

    fun toUrl(): String = PREFIX + Base64.getUrlEncoder().withoutPadding()
        .encodeToString(Json.encodeToString(serializer(), this).toByteArray(Charsets.UTF_8))

    companion object {
        const val PREFIX = "jokemihon-local-cover:"
        fun isCoverUrl(url: String?): Boolean = url?.startsWith(PREFIX) == true
        fun fromUrl(url: String): LocalWorkCover {
            require(isCoverUrl(url) && url.length <= 16384)
            val bytes = Base64.getUrlDecoder().decode(url.removePrefix(PREFIX))
            return Json.decodeFromString(serializer(), bytes.toString(Charsets.UTF_8))
        }
    }
}
