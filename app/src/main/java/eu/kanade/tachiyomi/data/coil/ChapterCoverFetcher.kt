package eu.kanade.tachiyomi.data.coil

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.request.Options
import kotlinx.coroutines.CancellationException
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/** Persistent epoch prevents old disk/memory covers returning after an app restart. */
data class ChapterCoverRequest(val chapterUrl: String, val root: String, val revision: Long) {
    val cacheKey: String get() = "jokemihon-chapter-v2:$root\u0000$chapterUrl\u0000$revision"
}

class ChapterCoverFetcher(
    private val data: ChapterCoverRequest,
    private val options: Options,
    private val cache: ChapterCoverDiskCache,
) : Fetcher {
    override suspend fun fetch(): SourceFetchResult {
        try {
            val snapshot = cache.open(data)
            try {
                return SourceFetchResult(
                    source = ImageSource(
                        file = snapshot.data,
                        fileSystem = options.fileSystem,
                        diskCacheKey = data.cacheKey,
                        closeable = snapshot,
                    ),
                    mimeType = null,
                    dataSource = DataSource.DISK,
                )
            } catch (e: Throwable) {
                snapshot.close()
                throw e
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Chapter cover cache/load failed" }
            throw e
        }
    }

    class Factory(private val cache: ChapterCoverDiskCache) : Fetcher.Factory<ChapterCoverRequest> {
        override fun create(data: ChapterCoverRequest, options: Options, imageLoader: ImageLoader) =
            ChapterCoverFetcher(data, options, cache)
    }

    class RequestKeyer : Keyer<ChapterCoverRequest> {
        override fun key(data: ChapterCoverRequest, options: Options) = data.cacheKey
    }
}
