package com.example.slipmat.core.media.waveform

import android.content.Context
import android.media.MediaCodec
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ensureActive
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext
import javax.inject.Inject

private const val DEQUEUE_TIMEOUT_US = 10_000L

/**
 * Decodes a file to PCM and reduces it to a waveform.
 *
 * Deliberately never holds the decoded audio: samples are folded into [PeakReducer] as they arrive
 * and the buffer is released immediately. A five-minute stereo track is ~50 MB of PCM, and the
 * result is 400 floats.
 */
class WaveformDecoder @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Returns peak values for [uri], or null when the file cannot be decoded.
     *
     * Cooperatively cancellable: the loop checks the coroutine on every buffer, so navigating away
     * mid-decode stops the work rather than finishing it for nobody.
     */
    suspend fun decode(uri: Uri, bucketCount: Int = WAVEFORM_BUCKETS): FloatArray? {
        val source = AudioTrackSource.open(context, uri) ?: return null

        return source.use {
            val codec = runCatching { MediaCodec.createDecoderByType(source.mime) }.getOrNull()
                ?: return@use null
            try {
                codec.configure(source.format, null, null, 0)
                codec.start()
                decodeLoop(codec, source, bucketCount)
            } catch (e: Exception) {
                // A malformed file should cost a missing waveform, not a crash.
                null
            } finally {
                runCatching { codec.stop() }
                codec.release()
            }
        }
    }

    private suspend fun decodeLoop(
        codec: MediaCodec,
        source: AudioTrackSource,
        bucketCount: Int,
    ): FloatArray {
        val reducer = PeakReducer(
            bucketCount = bucketCount,
            totalSampleCount = estimatedSampleCount(source),
        )
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        var shorts = ShortArray(0)

        while (!outputDone) {
            coroutineContext.ensureActive()

            if (!inputDone) {
                val inputIndex = codec.dequeueInputBuffer(DEQUEUE_TIMEOUT_US)
                if (inputIndex >= 0) {
                    val buffer = codec.getInputBuffer(inputIndex)!!
                    val size = source.extractor.readSampleData(buffer, 0)
                    if (size < 0) {
                        codec.queueInputBuffer(
                            inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                        )
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, size, source.extractor.sampleTime, 0)
                        source.extractor.advance()
                    }
                }
            }

            val outputIndex = codec.dequeueOutputBuffer(info, DEQUEUE_TIMEOUT_US)
            when {
                outputIndex >= 0 -> {
                    val buffer = codec.getOutputBuffer(outputIndex)
                    if (buffer != null && info.size > 0) {
                        val sampleCount = info.size / 2
                        if (shorts.size < sampleCount) shorts = ShortArray(sampleCount)
                        buffer.order(ByteOrder.nativeOrder())
                            .asShortBuffer()
                            .get(shorts, 0, sampleCount)
                        reducer.add(shorts, sampleCount)
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }

                outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                else -> Unit
            }
        }
        return reducer.peaks()
    }

    /**
     * How many samples the file should produce, from its declared duration.
     *
     * Only an estimate — containers round durations, and [PeakReducer] copes with the real stream
     * running short or long.
     */
    private fun estimatedSampleCount(source: AudioTrackSource): Long {
        val durationUs = source.durationUs
        if (durationUs <= 0L) return 0L
        return durationUs * source.sampleRate / 1_000_000L * source.channelCount
    }
}
