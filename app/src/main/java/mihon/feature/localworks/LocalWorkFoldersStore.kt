package mihon.feature.localworks

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.manga.model.Manga
import tachiyomi.source.local.work.LocalWorkCover
import tachiyomi.source.local.work.LocalWorkDefinition
import tachiyomi.source.local.work.LocalWorkFolder
import tachiyomi.source.local.work.LocalWorkFolders
import tachiyomi.source.local.work.LocalWorkPath
import java.util.UUID

@Inject
@SingleIn(AppScope::class)
class LocalWorkFoldersStore(preferences: PreferenceStore) {
    private val preference = preferences.getString(Preference.appStateKey("jokemihon_work_folders_v1"))
    val changes get() = preference.changes()

    @Synchronized
    fun get(): LocalWorkFolders = LocalWorkFolders.decode(preference.get())

    @Synchronized
    fun create(members: List<LocalWorkPath>, name: String): LocalWorkFolder {
        val current = get()
        val folder = LocalWorkFolder(UUID.randomUUID().toString(), name.trim(), members)
        current.folders.firstOrNull { it.url == folder.url }?.let { return it }
        preference.set(LocalWorkFolders(current.folders + folder).encode())
        return folder
    }

    @Synchronized
    fun setCover(url: String, cover: LocalWorkCover) {
        val current = get()
        val folder = current.folders.firstOrNull { it.url == url } ?: return
        require(cover.path in folder.members)
        preference.set(current.copy(folders = current.folders.map {
            if (it.id == folder.id) it.copy(cover = cover) else it
        }).encode())
    }

    // Earlier releases already backed up combined favorites as manga memos. Expose them without new DB entries.
    @Synchronized
    fun adoptExisting(mangas: List<Manga>) {
        val current = get()
        val known = current.folders.mapTo(HashSet()) { it.url }
        val additions = mangas.mapNotNull { manga ->
            if (!LocalWorkDefinition.isWorkUrl(manga.url) || manga.url in known) return@mapNotNull null
            val definition = runCatching {
                LocalWorkDefinition.fromMemo(manga.url, manga.memo)
            }.getOrNull() ?: return@mapNotNull null
            if (definition.members.size < 2) return@mapNotNull null
            val cover = manga.thumbnailUrl?.let { url ->
                runCatching { LocalWorkCover.fromUrl(url) }.getOrNull()
            }?.takeIf { it.path in definition.members }
            val folder = LocalWorkFolder(UUID.randomUUID().toString(), manga.title, definition.members, cover)
            known.add(folder.url)
            folder
        }
        if (additions.isNotEmpty()) preference.set(LocalWorkFolders(current.folders + additions).encode())
    }

    @Synchronized
    fun backup(): String = get().encode()

    @Synchronized
    fun restore(value: String) {
        // Complete validation before replacing; absent fields in older backups never call this.
        preference.set(LocalWorkFolders.decode(value).encode())
    }
}
