package com.example.slipmat.core.media.waveform

import androidx.core.net.toUri
import com.example.slipmat.core.data.db.WaveformDao
import com.example.slipmat.core.data.db.WaveformEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What a caller needs from the waveform cache.
 *
 * An interface for the same reason [com.example.slipmat.core.data.scan.AudioSource] is one: a view
 * model that shows waveforms should be testable without a decoder or a database.
 */
interface WaveformSource {
    suspend fun peaksFor(mediaUri: String): FloatArray?
}

/**
 * Waveforms, decoded once and remembered.
 *
 * Decoding a track costs seconds of CPU; reading it back costs a row lookup. Everything on screen
 * depends on that difference, which is why the cache is checked before anything else happens.
 */
@Singleton
class WaveformRepository @Inject constructor(
    private val decoder: WaveformDecoder,
    private val dao: WaveformDao,
) : WaveformSource {

    /** Cached peaks if present, otherwise decode and store. Null when the file cannot be decoded. */
    override suspend fun peaksFor(mediaUri: String): FloatArray? = withContext(Dispatchers.Default) {
        dao.get(mediaUri)?.let { cached ->
            if (cached.peaks.isNotEmpty()) return@withContext cached.peaks.toFloatArray()
        }

        val decoded = try {
            decoder.decode(mediaUri.toUri())
        } catch (e: CancellationException) {
            // Navigating away mid-decode is normal, not a failure worth caching.
            throw e
        } catch (e: Exception) {
            null
        } ?: return@withContext null

        dao.upsert(
            WaveformEntity(
                mediaUri = mediaUri,
                peaks = decoded.toList(),
                generatedAt = System.currentTimeMillis(),
            ),
        )
        decoded
    }

    suspend fun isCached(mediaUri: String): Boolean = dao.get(mediaUri) != null
}
