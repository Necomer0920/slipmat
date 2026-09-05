package com.example.slipmat.ui.theme

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Art is decoded no larger than this; a colour does not need a full-resolution sleeve. */
private const val DECODE_SIZE = 128

/** Albums whose colour is remembered. Skipping back and forth should not re-decode anything. */
private const val CACHE_ENTRIES = 128

/** Stands in for "this artwork has no usable colour", so failures are not retried forever. */
private const val NO_COLOR = 0

/**
 * Pulls one colour off a record sleeve.
 *
 * Only the *hue* of the result is used — [artworkColorScheme] sets the tones itself — so the choice
 * here is about finding the colour a person would name if asked what colour the sleeve is, which is
 * the vibrant swatch far more often than the dominant one. Dominant is usually the background.
 */
@Singleton
class ArtworkPalette @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val cache = LruCache<String, Int>(CACHE_ENTRIES)

    suspend fun seedFor(artworkUri: String?): Color? {
        if (artworkUri.isNullOrBlank()) return null

        cache.get(artworkUri)?.let { cached ->
            return if (cached == NO_COLOR) null else Color(cached)
        }

        val rgb = withContext(Dispatchers.IO) { extract(artworkUri) }
        cache.put(artworkUri, rgb ?: NO_COLOR)
        return rgb?.let(::Color)
    }

    private fun extract(artworkUri: String): Int? = try {
        val source = ImageDecoder.createSource(context.contentResolver, Uri.parse(artworkUri))
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            // Palette reads pixels back, which a hardware bitmap will not allow — it throws rather
            // than returning anything, and only on the devices that chose hardware allocation.
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
            decoder.setTargetSampleSize(sampleSizeFor(maxOf(info.size.width, info.size.height)))
        }

        val palette = Palette.from(bitmap).maximumColorCount(MAX_COLORS).generate()
        bitmap.recycle()

        // Vibrant first: the colour someone would name. Dominant is usually just the background.
        (
            palette.vibrantSwatch
                ?: palette.lightVibrantSwatch
                ?: palette.darkVibrantSwatch
                ?: palette.mutedSwatch
                ?: palette.dominantSwatch
            )?.rgb
    } catch (error: Exception) {
        // A missing, truncated or unsupported sleeve means no theming, never a crash. MediaStore
        // hands out art URIs for files that have since been deleted.
        null
    }

    private companion object {
        const val MAX_COLORS = 16
    }
}

/** Power-of-two sample size that brings [longestEdge] down to about [DECODE_SIZE]. */
internal fun sampleSizeFor(longestEdge: Int): Int {
    if (longestEdge <= DECODE_SIZE) return 1
    var sample = 1
    while (longestEdge / (sample * 2) >= DECODE_SIZE) sample *= 2
    return sample
}
