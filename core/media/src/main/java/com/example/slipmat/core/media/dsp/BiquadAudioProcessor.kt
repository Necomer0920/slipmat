package com.example.slipmat.core.media.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Cutoff can travel this whole range; the ends are effectively "off". */
const val FILTER_MIN_HZ = 30f
const val FILTER_MAX_HZ = 20_000f

/**
 * A sweepable low-pass / high-pass filter in ExoPlayer's audio pipeline.
 *
 * **Runs on the audio thread.** [queueInput] must not allocate: a garbage collection during
 * playback is an audible dropout, not a profiler curiosity. Everything it needs is allocated in
 * [onConfigure] and reused, and the output buffer comes from [replaceOutputBuffer], which recycles
 * a direct buffer rather than making one.
 *
 * Parameters arrive from the UI thread and are read on the audio thread, so they are `@Volatile`
 * singles rather than a lock — a torn read of one float costs one slightly wrong sample, whereas a
 * lock on the audio thread costs a dropout.
 */
@UnstableApi
class BiquadAudioProcessor : BaseAudioProcessor() {

    @Volatile
    private var enabled: Boolean = false

    @Volatile
    private var cutoffHz: Float = FILTER_MAX_HZ

    @Volatile
    private var mode: FilterMode = FilterMode.LowPass

    /** Read once per buffer rather than per sample, so a mid-buffer change cannot split a filter. */
    @Volatile
    private var coefficients: BiquadCoefficients = BiquadCoefficients.BYPASS

    private var sampleRate: Int = C.RATE_UNSET_INT
    private var channelCount: Int = 0

    // Per-channel filter memory. Allocated on configure, never in the loop.
    private var x1: FloatArray = FloatArray(0)
    private var x2: FloatArray = FloatArray(0)
    private var y1: FloatArray = FloatArray(0)
    private var y2: FloatArray = FloatArray(0)

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    fun setCutoff(hz: Float) {
        cutoffHz = hz.coerceIn(FILTER_MIN_HZ, FILTER_MAX_HZ)
        redesign()
    }

    fun setMode(mode: FilterMode) {
        this.mode = mode
        redesign()
    }

    /** Designed off the audio thread; the loop only ever reads the finished coefficients. */
    private fun redesign() {
        if (sampleRate == C.RATE_UNSET_INT) return
        coefficients = biquadCoefficients(mode, cutoffHz, sampleRate)
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            // Refusing an unexpected encoding is better than filtering garbage: the chain will
            // simply pass audio through untouched.
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount

        x1 = FloatArray(channelCount)
        x2 = FloatArray(channelCount)
        y1 = FloatArray(channelCount)
        y2 = FloatArray(channelCount)

        redesign()
        return inputAudioFormat
    }

    override fun isActive(): Boolean = enabled && super.isActive()

    override fun queueInput(inputBuffer: ByteBuffer) {
        // Bytes, not frames — output is the same size as input, sample for sample.
        val byteCount = inputBuffer.remaining()
        if (byteCount == 0) return

        val output = replaceOutputBuffer(byteCount).order(ByteOrder.nativeOrder())
        val input = inputBuffer.order(ByteOrder.nativeOrder())

        val c = coefficients
        var channel = 0

        while (input.remaining() >= 2) {
            val sample = input.short.toFloat()

            // Direct Form I. Five multiplies, four adds, no branches, no allocation.
            val out = c.b0 * sample + c.b1 * x1[channel] + c.b2 * x2[channel] -
                c.a1 * y1[channel] - c.a2 * y2[channel]

            x2[channel] = x1[channel]
            x1[channel] = sample
            y2[channel] = y1[channel]
            y1[channel] = out

            output.putShort(out.coerceIn(SHORT_MIN, SHORT_MAX).toInt().toShort())

            channel++
            if (channel == channelCount) channel = 0
        }

        inputBuffer.position(inputBuffer.limit())
        output.flip()
    }

    /** Clears filter memory, so a seek does not smear the previous position into the new one. */
    override fun onFlush() {
        x1.fill(0f)
        x2.fill(0f)
        y1.fill(0f)
        y2.fill(0f)
    }

    override fun onReset() {
        sampleRate = C.RATE_UNSET_INT
        channelCount = 0
        x1 = FloatArray(0)
        x2 = FloatArray(0)
        y1 = FloatArray(0)
        y2 = FloatArray(0)
        coefficients = BiquadCoefficients.BYPASS
    }

    private companion object {
        const val SHORT_MIN = -32768f
        const val SHORT_MAX = 32767f
    }
}
