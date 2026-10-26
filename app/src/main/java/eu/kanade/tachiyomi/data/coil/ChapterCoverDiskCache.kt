package eu.kanade.tachiyomi.data.coil

import android.content.Context
import coil3.disk.DiskCache
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okio.Path.Companion.toOkioPath
import okio.buffer
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.source.local.image.LocalChapterCoverManager
import java.io.File
import kotlin.coroutines.coroutineContext

/** Cache encoded covers, like the library: Coil decodes only the requested thumbnail size. */
@Inject
@SingleIn(AppScope::class)
class ChapterCoverDiskCache(context: Context, private val covers: LocalChapterCoverManager) {
    private val cache by lazy {
        DiskCache.Builder()
            .directory(File(context.cacheDir, "chapter-covers-v2").toOkioPath())
            .maxSizeBytes(256L * 1024 * 1024)
            .build()
    }
    private val fills = Semaphore(2)
    private val requests = DeDupeConcurrentRequests<Unit>()

    suspend fun open(data: ChapterCoverRequest): DiskCache.Snapshot {
        val key = data.cacheKey
        cache.openSnapshot(key)?.let { return it }
        requests.apply(key) {
            fills.withPermit {
                // Do not retain snapshots across a dispatcher switch or any suspension.
                val exists = cache.openSnapshot(key)?.use { true } ?: false
                if (!exists) withContext(Dispatchers.IO) {
                    val editor = cache.openEditor(key) ?: error("Chapter cover cache is busy")
                    try {
                        cache.fileSystem.sink(editor.data).buffer().use { sink ->
                            covers.copyTo(data.chapterUrl, data.revision, data.root, sink.outputStream())
                        }
                        check(ImageUtil.findImageType { cache.fileSystem.source(editor.data).buffer().inputStream() } != null) {
                            "Chapter cover image is invalid"
                        }
                        coroutineContext.ensureActive()
                        editor.commit()
                    } catch (e: Throwable) {
                        runCatching { editor.abort() }
                        throw e
                    }
                }
            }
        }
        return cache.openSnapshot(key) ?: error("Chapter cover cache entry is unavailable")
    }
}
