package tachiyomi.source.local.image

import com.hippo.unifile.UniFile
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.source.local.io.LocalSourceFileSystem
import tachiyomi.source.local.work.LocalWorkCatalog
import tachiyomi.source.local.work.LocalWorkCover
import tachiyomi.source.local.work.LocalWorkCoverReader
import tachiyomi.source.local.work.LocalWorkPath
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID

/** Chapter artwork only. Never changes manga artwork, source pages or archive contents. */
@Inject
@SingleIn(AppScope::class)
class LocalChapterCoverManager(
    private val catalog: LocalWorkCatalog,
    private val fileSystem: LocalSourceFileSystem,
    private val reader: LocalWorkCoverReader,
) {
    private val writes = Mutex()
    private val mutableRevision = MutableStateFlow(0L)
    val revision = mutableRevision.asStateFlow()

    fun invalidate() = mutableRevision.update { it + 1 }

    suspend fun open(chapterUrl: String): InputStream = withContext(Dispatchers.IO) {
        val path = LocalWorkPath.fromRelativeUrl(chapterUrl)
        val file = catalog.resolveFile(path) ?: error("Chapter is unavailable")
        val directory = if (file.isDirectory) file else fileSystem.getMangaDirectory(path.directory)
        val stem = if (file.isDirectory) "cover" else "${path.entry.substringBeforeLast('.')}.cover"
        findCovers(directory?.listFiles().orEmpty().toList(), stem).firstOrNull()?.openInputStream()
            ?: reader.open(LocalWorkCover(path).toUrl(), preferNamedCover = true)
    }

    /** Copy to a sibling, validate bytes/type, then rename. Existing artwork is kept as a hidden backup. */
    suspend fun set(chapterUrl: String, openStream: () -> InputStream) = withContext(Dispatchers.IO) {
        writes.withLock {
            val path = LocalWorkPath.fromRelativeUrl(chapterUrl)
            catalog.requireAvailable(path)
            val file = catalog.resolveFile(path) ?: error("Chapter is unavailable")
            val directory = if (file.isDirectory) file else fileSystem.getMangaDirectory(path.directory)
            check(directory != null && directory.isDirectory && directory.canWrite()) { "Chapter folder is not writable" }
            val stem = if (file.isDirectory) "cover" else "${path.entry.substringBeforeLast('.')}.cover"
            val covers = findCovers(directory.listFiles().orEmpty().toList(), stem)
            check(covers.size <= 1) { "Multiple cover files exist; choose one cover file first" }
            val old = covers.singleOrNull()
            val temporaryName = ".jokemihon-cover-${UUID.randomUUID()}.tmp"
            val temporary = directory.createFile(temporaryName) ?: error("Cannot create cover temporary file")
            check(temporary.name == temporaryName) { "Storage provider changed temporary filename" }
            val expected = MessageDigest.getInstance("SHA-256")
            var size = 0L
            openStream().use { input ->
                temporary.openOutputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        size += count
                        check(size <= MAX_COVER_BYTES) { "Chapter cover exceeds 64 MiB" }
                        expected.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                }
            }
            check(size > 0) { "Chapter cover is empty" }
            val actual = MessageDigest.getInstance("SHA-256")
            temporary.openInputStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                var checked = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    checked += count
                    check(checked <= MAX_COVER_BYTES) { "Chapter cover exceeds 64 MiB" }
                    actual.update(buffer, 0, count)
                }
                check(checked == size) { "Incomplete cover copy" }
            }
            check(expected.digest().contentEquals(actual.digest())) { "Cover copy validation failed" }
            val type = ImageUtil.findImageType { temporary.openInputStream() } ?: error("Invalid cover image")
            val name = old?.name ?: "$stem.${type.extension}"
            check(name != null && name.length <= 240) { "Cover filename is too long" }
            check(directory.findFile(name)?.uri == old?.uri) { "Cover destination changed" }
            if (file.isDirectory && old == null) {
                // This is an added copy, not a reading page. Create its marker before publishing it.
                val markerName = markerName(name)
                val marker = directory.findFile(markerName) ?: directory.createFile(markerName)
                check(marker != null && marker.name == markerName && marker.isFile) { "Cannot record generated cover" }
                marker.openOutputStream().use { it.write("JOKEMIHON chapter cover v1\n".toByteArray()) }
            }
            var backupName: String? = null
            if (old != null) {
                backupName = ".jokemihon-cover-backup-${UUID.randomUUID()}.bin"
                check(old.renameTo(backupName)) { "Provider cannot safely replace the cover" }
            }
            try {
                check(temporary.renameTo(name)) { "Provider cannot publish the cover" }
                check(directory.findFile(name)?.isFile == true) { "Published cover is unavailable" }
            } catch (e: Throwable) {
                if (backupName != null && directory.findFile(name) == null) {
                    check(directory.findFile(backupName)?.renameTo(name) == true) { "Cover rollback failed; hidden backup retained" }
                }
                throw e
            }
            invalidate()
        }
    }

    private fun findCovers(files: List<UniFile>, stem: String): List<UniFile> = files
        .filter { it.isFile && it.name?.substringBeforeLast('.', it.name.orEmpty()).equals(stem, true) }
        .filter { ImageUtil.findImageType { it.openInputStream() } != null }
        .sortedBy { it.name }

    companion object {
        private const val MAX_COVER_BYTES = 64L * 1024 * 1024
        private fun markerName(name: String) = ".jokemihon-chapter-cover.$name.marker"

        /** Only newly generated copies are excluded; pre-existing cover pages keep their old indices. */
        fun readingFiles(files: List<UniFile>): List<UniFile> {
            val names = files.mapNotNull { it.name }.toHashSet()
            return files.filter { file ->
                val name = file.name.orEmpty()
                !name.startsWith('.') && !(name.substringBeforeLast('.').equals("cover", true) && markerName(name) in names)
            }
        }
    }
}
