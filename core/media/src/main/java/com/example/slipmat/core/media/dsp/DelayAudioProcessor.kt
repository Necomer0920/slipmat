package com.example.slipmat.core.media.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** The delay line is allocated for this much time; the control cannot ask for more. */
const val MAX_DELAY_MS = 1_000f

/** Below about this, repeats stop being echoes and start being comb filtering. */
const val MIN_DELAY_MS = 20f

/** A quarter note at 120 bpm, which is a musically useful place to start. */
const val DEFAULT_DELAY_MS = 500f

/**
 * Feedback stops here rather than at 1.0.
 *
 * At exactly 1.0 the line neither grows nor decays — an infinite hold. That is a real effect, but
 * it is not this one, and a repeat control whose top end never stops repeating is a control that
 * has no top end. 0.9 gives roughly twenty audible repeats before the tail is gone.
 */
const val MAX_FEEDBACK = 0.9f

/**
 * An echo in ExoPlayer's audio pipeline.
 *
 * **Runs on the audio thread**, under the same rules as [BiquadAudioProcessor]: no allocation in
 * [queueInput], parameters passed as `@Volatile` singles rather than behind a lock, and the delay
 * line allocated once in [onConfigure].
 *
 * Enablement is read per buffer here rather than through [isActive], for the reason spelled out on
 * [BiquadAudioProcessor.isActive] — Media3 asks that question when it configures the chain, so a
 * processor that answers it with a runtime switch freezes in whatever state the switch held when
 * the track started.
 */
@UnstableApi
class DelayAudioProcessor : BaseAudioProcessor() {

    @Volatile
    private var enabled: Boolean = false

    @Volatile
    private var delayMs: Float = DEFAULT_DELAY_MS

    @Volatile
    private var feedback: Float = 0f

    private var sampleRate: Int = C.RATE_UNSET_INT
    private var channelCount: Int = 0

    /**
     * The delay line: interleaved frames, `channelCount` floats each.
     *
     * Floats rather than shorts because feedback accumulates, and rounding to 16 bits on every lap
     * turns a decaying tail into a gritty one. Grow-only across configures — a second of stereo at
     * 192 kHz is three megabytes, and [onConfigure] runs per track.
     */
    private var line: FloatArray = FloatArray(0)

    /** Capacity of [line] in frames, which is one more than the longest delay it must hold. */
    private var lineFrames: Int = 0

    private var writeFrame: Int = 0

    fun setEnabled(enabled: Boolean) {
        if (this.enabled == enabled) return
        this.enabled = enabled
        // Otherwise switching the delay back on replays whatever was in the line when it went off.
        clearLine()
    }

    fun setDelayMs(ms: Float) {
        delayMs = ms.coerceIn(MIN_DELAY_MS, MAX_DELAY_MS)
    }

    fun setFeedback(value: Float) {
        feedback = value.coerceIn(0f, MAX_FEEDBACK)
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount

        // One spare frame so the longest delay still reads a frame written a full lap ago rather
        // than the one about to be overwritten.
        lineFrames = framesFor(MAX_DELAY_MS, sampleRate) + 1
        val required = lineFrames * channelCount
        if (line.size < required) line = FloatArray(required)
        clearLine()

        return inputAudioFormat
    }

    /** Active for any supported format; the switch is read per buffer. See [BiquadAudioProcessor]. */
    override fun isActive(): Boolean = super.isActive()

    override fun queueInput(inputBuffer: ByteBuffer) {
        val byteCount = inputBuffer.remaining()
        if (byteCount == 0) return

        val output = replaceOutputBuffer(byteCount).order(ByteOrder.nativeOrder())
        val input = inputBuffer.order(ByteOrder.nativeOrder())

        if (!enabled) {
            output.put(input)
            inputBuffer.position(inputBuffer.limit())
            output.flip()
            return
        }

        // Read once per buffer: a delay length that changed mid-buffer would splice the line.
        val delayFrames = framesFor(delayMs, sampleRate).coerceIn(1, lineFrames - 1)
        val fb = feedback
        val dryGain = 1f - fb
        val frameBytes = channelCount * 2

        while (input.remaining() >= frameBytes) {
            var readFrame = writeFrame - delayFrames
            if (readFrame < 0) readFrame += lineFrames
            val readBase = readFrame * channelCount
            val writeBase = writeFrame * channelCount

            var channel = 0
            while (channel < channelCount) {
                val dry = input.short.toFloat()
                val wet = line[readBase + channel]

                // Normalised feedback. The obvious `dry + wet * fb` converges to `dry / (1 - fb)`
                // on sustained material — twenty times the input at the top of the range — so it
                // clips solid on anything but an isolated drum hit. Scaling the input by the same
                // amount the tail is fed back holds the steady state at exactly the input level,
                // for any feedback value, without a limiter deciding how it sounds.
                line[writeBase + channel] = dryGain * dry + fb * wet
                output.putShort(mixDown(dry, wet))

                channel++
            }

            writeFrame++
            if (writeFrame == lineFrames) writeFrame = 0
        }

        inputBuffer.position(inputBuffer.limit())
        output.flip()
    }

    /** Clears the line, so a seek does not echo the previous position into the new one. */
    override fun onFlush() {
        writeFrame = 0
        clearLine()
    }

    private fun clearLine() {
        line.fill(0f)
        writeFrame = 0
    }

    override fun onReset() {
        sampleRate = C.RATE_UNSET_INT
        channelCount = 0
        lineFrames = 0
        writeFrame = 0
        line = FloatArray(0)
    }
}

/** Delay length in frames. Rounded, not truncated, so 500 ms is 500 ms rather than a sample short. */
internal fun framesFor(ms: Float, sampleRate: Int): Int =
    if (sampleRate <= 0) 0 else (ms * sampleRate / 1000f + 0.5f).toInt()

/**
 * Half dry, half wet.
 *
 * Halving rather than summing: two full-scale signals added together clip, and a delay that only
 * sounds right on quiet material is not a delay. The mix becomes adjustable in task 6.11.
 */
private fun mixDown(dry: Float, wet: Float): Short =
    ((dry + wet) * 0.5f).coerceIn(-32768f, 32767f).toInt().toShort()
