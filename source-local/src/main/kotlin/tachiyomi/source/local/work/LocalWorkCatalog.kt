package tachiyomi.source.local.work

import android.util.LruCache
import com.hippo.unifile.UniFile
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.util.lang.compareToCaseInsensitiveNaturalOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import tachiyomi.core.common.storage.extension
import tachiyomi.source.local.io.Archive
import tachiyomi.source.local.io.LocalSourceFileSystem
import kotlin.coroutines.coroutineContext

@Inject
@SingleIn(AppScope::class)
class LocalWorkCatalog(private val fileSystem: LocalSourceFileSystem) {
    // Bound cached SAF handles, not images. Root URI is part of each key; refresh evicts stale handles.
    private val directories = LruCache<String, UniFile>(32)
    private val files = LruCache<String, UniFile>(512)

    suspend fun authors(): List<LocalAuthorGroup> = withContext(Dispatchers.IO) {
        directories.evictAll()
        files.evictAll()
        val base = fileSystem.getBaseDirectory() ?: error("Local storage is not configured")
        check(base.isDirectory && base.canRead()) { "Local storage is not readable" }
        groupLocalAuthors(base.listFiles().orEmpty().filter { it.isDirectory }.mapNotNull { it.name })
    }

    // Scan one author on demand. Never recursively decode covers or list the image files here.
    suspend fun works(author: LocalAuthorGroup): List<LocalWorkPath> = withContext(Dispatchers.IO) {
        files.evictAll()
        val base = fileSystem.getBaseDirectory() ?: error("Local storage is not configured")
        buildList {
            for (directory in author.directories) {
                coroutineContext.ensureActive()
                val folder =
                    base.findFile(directory)?.takeIf { it.isDirectory } ?: error("Author directory is unavailable")
                directories.put("${base.uri}\u0000$directory", folder)
                val entries = folder.listFiles().orEmpty()
                    .filter { !it.name.orEmpty().startsWith('.') }
                    .filter(::supported)
                    .sortedWith { a, b -> a.name.orEmpty().compareToCaseInsensitiveNaturalOrder(b.name.orEmpty()) }
                for (entry in entries) {
                    val path = LocalWorkPath(directory, entry.name ?: continue)
                    files.put("${base.uri}\u0000${path.relativeUrl}", entry)
                    add(path)
                }
            }
        }
    }

    fun resolveFile(path: LocalWorkPath): UniFile? {
        val base = fileSystem.getBaseDirectory() ?: return null
        val fileKey = "${base.uri}\u0000${path.relativeUrl}"
        files.get(fileKey)?.takeIf { it.exists() }?.let { return it }
        val directoryKey = "${base.uri}\u0000${path.directory}"
        val folder = directories.get(directoryKey)?.takeIf { it.exists() }
            ?: base.findFile(path.directory)?.takeIf { it.isDirectory }?.also { directories.put(directoryKey, it) }
        return folder?.findFile(path.entry)?.also { files.put(fileKey, it) }
    }

    fun requireAvailable(path: LocalWorkPath) {
        require(resolveFile(path)?.let(::supported) == true) { "Local work is unavailable: ${path.entry}" }
    }

    // Validate a batch in one listing per physical parent, not N repeated SAF full-directory searches.
    fun requireAvailableWorks(paths: List<LocalWorkPath>) {
        for ((directory, members) in paths.groupBy { it.directory }) {
            val available = fileSystem.getMangaDirectory(directory)?.listFiles().orEmpty().associateBy { it.name }
            for (path in members) {
                require(available[path.entry]?.let(::supported) == true) {
                    "Local work is unavailable: ${path.entry}"
                }
            }
        }
    }

    private fun supported(file: UniFile): Boolean =
        file.isDirectory || Archive.isSupported(file) || file.extension.equals("epub", true)
}
