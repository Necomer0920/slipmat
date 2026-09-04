package com.example.slipmat.core.media.waveform

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri

/**
 * An opened audio track, ready to decode.
 *
 * [extractor] has exactly one track selected, so `readSampleData` returns only that track's
 * samples. The caller owns it and must [close] when finished.
 */
class AudioTrackSource private constructor(
    val extractor: MediaExtractor,
    val format: MediaFormat,
    val mime: String,
) : AutoCloseable {

    val sampleRate: Int get() = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)

    val channelCount: Int get() = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

    /** Duration in microseconds, or `0` when the container does not declare one. */
    val durationUs: Long
        get() = if (format.containsKey(MediaFormat.KEY_DURATION)) {
            format.getLong(MediaFormat.KEY_DURATION)
        } else {
            0L
        }

    override fun close() {
        extractor.release()
    }

    companion object {
        /**
         * Opens [uri] and selects its first audio track.
         *
         * Returns null rather than throwing for the ordinary failures — an unreadable file, a
         * container with no audio track, a codec the device cannot parse. A waveform is a nicety;
         * failing to draw one must never take the app down.
         */
        fun open(context: Context, uri: Uri): AudioTrackSource? {
            val extractor = MediaExtractor()
            return try {
                extractor.setDataSource(context, uri, null)
                val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                    extractor.getTrackFormat(index)
                        .getString(MediaFormat.KEY_MIME)
                        ?.startsWith("audio/") == true
                }
                if (trackIndex == null) {
                    extractor.release()
                    return null
                }
                val format = extractor.getTrackFormat(trackIndex)
                extractor.selectTrack(trackIndex)
                AudioTrackSource(
                    extractor = extractor,
                    format = format,
                    mime = requireNotNull(format.getString(MediaFormat.KEY_MIME)),
                )
            } catch (e: Exception) {
                extractor.release()
                null
            }
        }
    }
}
