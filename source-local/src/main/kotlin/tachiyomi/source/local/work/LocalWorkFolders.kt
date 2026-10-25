package tachiyomi.source.local.work

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Manager-only directories. Members always point to original media; favorites are separate DB state. */
@Serializable
data class LocalWorkFolder(
    val id: String,
    val name: String,
    val members: List<LocalWorkPath>,
    val cover: LocalWorkCover? = null,
) {
    init {
        require(id.matches(Regex("[A-Za-z0-9_-]{1,64}")))
        require(name.isNotBlank() && name.length <= 1024)
        require(members.size >= 2)
        LocalWorkDefinition(members)
        require(cover == null || cover.path in members)
    }

    // Reuse existing ordered collection identities, preserving previous combined books and progress.
    @kotlinx.serialization.Transient
    val url = LocalWorkDefinition(members).url
    @kotlinx.serialization.Transient
    val coverUrl = (cover ?: LocalWorkCover(members.first())).toUrl()
}

@Serializable
data class LocalWorkFolders(val folders: List<LocalWorkFolder> = emptyList(), val version: Int = 1) {
    init {
        require(version == 1 && folders.size <= 10000)
        require(folders.sumOf { it.members.size } <= 100000)
        require(folders.map { it.id }.distinct().size == folders.size)
        require(folders.map { it.url }.distinct().size == folders.size)
    }

    fun encode(): String = Json.encodeToString(serializer(), this).also {
        require(it.length <= MAX_LENGTH) { "Local work directories are too large" }
    }

    companion object {
        private const val MAX_LENGTH = 8 * 1024 * 1024
        fun decode(value: String): LocalWorkFolders {
            require(value.length <= MAX_LENGTH) { "Local work directories are too large" }
            return if (value.isBlank()) LocalWorkFolders() else Json.decodeFromString(serializer(), value)
        }
    }
}
