package eu.kanade.presentation.library.components.folia

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Precision
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import tachiyomi.domain.manga.model.MangaCover

internal data class FoliaCoverFacts(
    val aspect: Float,
    val palette: List<Int> = emptyList(),
    val resolved: Boolean = false,
)

/** Dimension-only visible-cover observations and small focused-cover colour facts, never retained bitmaps. */
internal object FoliaCoverFactsCache {
    private val facts = LruCache<MangaCover, FoliaCoverFacts>(128)
    private val loadGate = Semaphore(1)

    fun peekOrNull(key: MangaCover?) = key?.let(::peek)

    @Synchronized fun peek(key: MangaCover): FoliaCoverFacts? = facts.get(key)

    @Synchronized fun observe(key: MangaCover, width: Int, height: Int) {
        if (width > 0 && height > 0 && facts.get(key) == null) facts.put(key, FoliaCoverFacts(width.toFloat() / height))
    }

    suspend fun load(context: Context, key: MangaCover): FoliaCoverFacts? = loadGate.withPermit {
        peek(key)?.takeIf { it.resolved }?.let { return@withPermit it }
        withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context.applicationContext).data(key).size(128)
                    .allowHardware(false).precision(Precision.INEXACT).build()
                val image = context.imageLoader.execute(request).image ?: return@withContext peek(key)
                if (image.width <= 0 || image.height <= 0) return@withContext peek(key)
                val drawable = image.asDrawable(context.resources)
                val sample = drawable.toBitmap(96, 96)
                val colors = try {
                    val pixels = IntArray(96 * 96)
                    sample.getPixels(pixels, 0, 96, 0, 0, 96, 96)
                    representativeCoverArgbs(pixels)
                } finally {
                    // Never recycle a bitmap borrowed from Coil's cache.
                    if (drawable !is BitmapDrawable || sample !== drawable.bitmap) sample.recycle()
                }
                FoliaCoverFacts(image.width.toFloat() / image.height, colors, resolved = true).also {
                    synchronized(this@FoliaCoverFactsCache) { facts.put(key, it) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Missing/failed art does not change a book or poison future cover retries.
                peek(key)
            }
        }
    }
}
