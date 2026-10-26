package tachiyomi.source.local.image

import android.content.Context
import android.util.LruCache
import com.hippo.unifile.UniFile
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.util.lang.compareToCaseInsensitiveNaturalOrder
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import mihon.core.archive.archiveReader
import mihon.core.archive.epubReader
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.source.local.io.LocalSourceFileSystem
import tachiyomi.source.local.work.LocalWorkCatalog
import tachiyomi.source.local.work.LocalWorkPath

/** Chapter artwork only. Never changes manga artwork, source pages or archive contents. */
@Inject
@SingleIn(AppScope::class)
class LocalChapterCoverManager(
    private val catalog: LocalWorkCatalog,
    private val fileSystem: LocalSourceFileSystem,
    private val context: Context,
    private val storagePreferences: tachiyomi.domain.storage.service.StoragePreferences,
    preferenceStore: PreferenceStore,
) {
    private val writes = Mutex()
    private val epoch = preferenceStore.getLong(Preference.appStateKey("chapter_cover_cache_epoch_v2"), 0L)
    private val mutableRevision = MutableStateFlow(epoch.get())
    val revision = mutableRevision.asStateFlow()
    private val reads = Semaphore(2)
    private val listingMutex = Mutex()
    // Bounded by total retained children, not an unbounded list for every chapter.
    private val listings = object : LruCache<String, List<UniFile>>(20_000) {
        override fun sizeOf(key: String, value: List<UniFile>) = value.size.coerceAtLeast(1)
    }

    @Synchronized
    fun invalidate() {
        val next = epoch.get() + 1
        epoch.set(next)
        mutableRevision.value = next
        listings.evictAll()
    }

    private suspend fun children(directory: UniFile, generation: Long): List<UniFile> = listingMutex.withLock {
        val key = "${directory.uri}\u0000$generation"
        listings.get(key) ?: directory.listFiles().orEmpty().toList().also { listings.put(key, it) }
    }

    /** No stream/permit crosses a coroutine boundary. Archive owners close BEFORE returning. */
    suspend fun copyTo(chapterUrl: String, generation: Long, expectedRoot: String, output: OutputStream) = withContext(Dispatchers.IO) {
        reads.withPermit {
            check(storagePreferences.baseStorageDirectory.get() == expectedRoot) { "Local storage changed" }
            val path = LocalWorkPath.fromRelativeUrl(chapterUrl)
            val root = fileSystem.getBaseDirectory() ?: error("Local storage is not configured")
            val parent = children(root, generation).firstOrNull { it.name == path.directory }
                ?: error("Chapter parent is unavailable")
            val siblings = children(parent, generation)
            val file = siblings.firstOrNull { it.name == path.entry } ?: error("Chapter is unavailable")
            if (file.isDirectory) {
                val files = children(file, generation)
                val explicit = findCovers(files, "cover").firstOrNull()
                val image = explicit ?: readingFiles(files)
                    .sortedWith { a, b -> a.name.orEmpty().compareToCaseInsensitiveNaturalOrder(b.name.orEmpty()) }
                    .firstOrNull { candidate ->
                        coroutineContext.ensureActive()
                        isImageCandidate(candidate.name) && candidate.isFile &&
                            (isKnownImageName(candidate.name) || ImageUtil.findImageType { candidate.openInputStream() } != null)
                    } ?: error("Chapter has no readable image")
                image.openInputStream().use { copyBounded(it, output) }
            } else {
                val sidecar = findCovers(siblings, "${path.entry.substringBeforeLast('.')}.cover").firstOrNull()
                if (sidecar != null) {
                    sidecar.openInputStream().use { copyBounded(it, output) }
                } else if (path.entry.substringAfterLast('.').equals("epub", true)) {
                    file.epubReader(context).use { reader ->
                        val image = reader.getImagesFromPages().firstOrNull() ?: error("Chapter has no readable image")
                        reader.getInputStream(image).use { copyBounded(it, output) }
                    }
                } else {
                    file.archiveReader(context).use { reader ->
                        val entries = reader.useEntries { sequence ->
                            sequence.filter { it.isFile && isImageCandidate(it.name) }
                                .sortedWith { a, b -> a.name.compareToCaseInsensitiveNaturalOrder(b.name) }
                                .map { it.name }.toList()
                        }
                        fun isImage(name: String): Boolean = isKnownImageName(name) ||
                            ImageUtil.findImageType { reader.getInputStream(name) ?: error("Image is unavailable") } != null
                        val image = entries.firstOrNull { isCoverName(it.substringAfterLast('/'), "cover") && isImage(it) }
                            ?: entries.firstOrNull { isImage(it) }
                            ?: error("Chapter has no readable image")
                        val stream = reader.getInputStream(image) ?: error("Image is unavailable")
                        stream.use { copyBounded(it, output) }
                    }
                }
            }
            check(storagePreferences.baseStorageDirectory.get() == expectedRoot && revision.value == generation) {
                "Chapter cover source changed during loading"
            }
        }
    }

    private suspend fun copyBounded(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            coroutineContext.ensureActive()
            val count = input.read(buffer, 0, buffer.size)
            if (count < 0) break
            if (count == 0) continue
            total += count
            check(total <= MAX_COVER_BYTES) { "Chapter cover exceeds 64 MiB" }
            output.write(buffer, 0, count)
        }
        check(total > 0) { "Chapter cover is empty" }
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
        .filter { isCoverName(it.name, stem) && it.isFile }
        .filter { ImageUtil.findImageType { it.openInputStream() } != null }
        .sortedBy { it.name }

    companion object {
        private const val MAX_COVER_BYTES = 64L * 1024 * 1024
        private val IMAGE_EXTENSIONS = ImageUtil.ImageType.entries.map { it.extension }.toSet() + setOf("jpeg", "heic")
        private val NON_IMAGE_EXTENSIONS = setOf("xml", "json", "txt", "nfo", "opf", "ncx", "zip", "cbz", "rar", "cbr", "7z", "epub", "pdf", "db", "ini", "marker", "tmp")

        fun isCoverName(name: String?, stem: String): Boolean =
            name != null && name.substringBeforeLast('.', name).equals(stem, true)

        fun isKnownImageName(name: String?): Boolean =
            name?.substringAfterLast('.', "")?.lowercase(java.util.Locale.ROOT) in IMAGE_EXTENSIONS

        private fun isImageCandidate(name: String?): Boolean = name != null &&
            !name.substringAfterLast('/').startsWith('.') &&
            name.substringAfterLast('.', "").lowercase(java.util.Locale.ROOT) !in NON_IMAGE_EXTENSIONS
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
