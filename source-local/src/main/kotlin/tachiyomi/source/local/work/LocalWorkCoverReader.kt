package tachiyomi.source.local.work

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.util.lang.compareToCaseInsensitiveNaturalOrder
import kotlinx.coroutines.sync.Semaphore
import mihon.core.archive.archiveReader
import mihon.core.archive.epubReader
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.source.local.io.Format
import java.io.Closeable
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

/** JOKEMIHON: Coil samples the visible thumbnail; at most two archive cover streams are live. */
@Inject
@SingleIn(AppScope::class)
class LocalWorkCoverReader(private val context: Context, private val catalog: LocalWorkCatalog) {
    private val permits = Semaphore(2)

    suspend fun open(url: String): InputStream {
        val cover = LocalWorkCover.fromUrl(url)
        permits.acquire()
        var owner: Closeable? = null
        try {
            val file = catalog.resolveFile(cover.path)
                ?: error("Local cover file is unavailable")
            val stream = when (val format = Format.valueOf(file)) {
                is Format.Directory -> {
                    val images = format.file.listFiles().orEmpty()
                        .filter { !it.isDirectory && ImageUtil.isImage(it.name) { it.openInputStream() } }
                        .sortedWith { a, b -> a.name.orEmpty().compareToCaseInsensitiveNaturalOrder(b.name.orEmpty()) }
                    images.getOrNull(cover.page)?.openInputStream() ?: error("Cover page is unavailable")
                }
                is Format.Archive -> {
                    val reader = format.file.archiveReader(context)
                    owner = reader
                    val image = reader.useEntries { entries ->
                        entries.filter { it.isFile && ImageUtil.isImage(it.name) { reader.getInputStream(it.name)!! } }
                            .sortedWith { a, b -> a.name.compareToCaseInsensitiveNaturalOrder(b.name) }
                            .elementAtOrNull(cover.page)?.name
                    } ?: error("Cover page is unavailable")
                    reader.getInputStream(image) ?: error("Cover page is unavailable")
                }
                is Format.Epub -> {
                    val reader = format.file.epubReader(context)
                    owner = reader
                    val image = reader.getImagesFromPages().getOrNull(cover.page) ?: error("Cover page is unavailable")
                    reader.getInputStream(image) ?: error("Cover page is unavailable")
                }
            }
            val resource = owner
            return object : FilterInputStream(stream) {
                private var closed = false
                private var consumed = 0L
                private fun count(bytes: Int): Int {
                    if (bytes > 0) consumed += bytes
                    if (consumed > 64L * 1024 * 1024) throw IOException("Cover image exceeds 64 MiB")
                    return bytes
                }
                override fun read(): Int = super.read().also { if (it >= 0) count(1) }
                override fun read(
                    buffer: ByteArray,
                    offset: Int,
                    length: Int,
                ): Int = count(`in`.read(buffer, offset, length))
                override fun close() {
                    if (closed) return
                    closed = true
                    try {
                        super.close()
                    } finally {
                        try {
                            resource?.close()
                        } finally {
                            permits.release()
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            try {
                owner?.close()
            } finally {
                permits.release()
            }
            throw e
        }
    }
}
