package eu.kanade.tachiyomi.data.coil

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.request.Options
import okio.buffer
import okio.source
import tachiyomi.source.local.image.LocalChapterCoverManager

/** Root and revision prevent stale thumbnails across SAF changes and explicit cover edits. */
data class ChapterCoverRequest(val chapterUrl: String, val root: String, val revision: Long)

class ChapterCoverFetcher(
    private val data: ChapterCoverRequest,
    private val options: Options,
    private val covers: LocalChapterCoverManager,
) : Fetcher {
    override suspend fun fetch() = SourceFetchResult(
        source = ImageSource(covers.open(data.chapterUrl).source().buffer(), options.fileSystem),
        mimeType = null,
        dataSource = DataSource.DISK,
    )

    class Factory(private val covers: LocalChapterCoverManager) : Fetcher.Factory<ChapterCoverRequest> {
        override fun create(data: ChapterCoverRequest, options: Options, imageLoader: ImageLoader) =
            ChapterCoverFetcher(data, options, covers)
    }

    class RequestKeyer : Keyer<ChapterCoverRequest> {
        override fun key(data: ChapterCoverRequest, options: Options) =
            "jokemihon-chapter:${data.root}\u0000${data.chapterUrl}\u0000${data.revision}"
    }
}
