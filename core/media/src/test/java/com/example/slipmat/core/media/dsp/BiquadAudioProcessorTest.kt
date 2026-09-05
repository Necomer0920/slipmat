package com.example.slipmat.core.media.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val RATE = 44_100
private const val CHANNELS = 2
private const val FRAMES = 8192

/**
 * These cover the *enablement* path, which is where the filter actually broke.
 *
 * Media3 asks a processor `isActive` when it configures the chain — once per track, not per buffer.
 * The first version of this class answered that question with the on/off switch, so chain membership
 * froze at whatever the switch read at track start: on then meant it could never be turned off, off
 * then meant it could never be turned on. Both misbehaviours were silent, and the response tests in
 * [BiquadResponseTest] passed throughout, because the coefficient math was never wrong.
 */
@UnstableApi
class BiquadAudioProcessorTest {

    @Test
    fun `stays in the chain while switched off`() {
        val processor = configured(enabled = false)

        // If this is ever false, the processor is dropped at configure time and no later call to
        // setEnabled can bring it back until the next track.
        assertTrue(processor.isActive)
    }

    @Test
    fun `switched off, it passes audio through untouched`() {
        val processor = configured(enabled = false)
        val input = tone(hz = 10_000f)

        val output = processor.process(input)

        assertArrayEquals(tone(hz = 10_000f).array(), output)
    }

    @Test
    fun `switching on takes effect without reconfiguring`() {
        // Configured while off, exactly as it would be if a track started with the filter down.
        val processor = configured(enabled = false)
        val loud = rms(processor.process(tone(hz = 10_000f)))

        processor.setEnabled(true)
        processor.setCutoff(200f)
        val filtered = rms(processor.process(tone(hz = 10_000f)))

        // A 10 kHz tone through a 200 Hz low-pass is roughly 60 dB down; anything near the input
        // level means the toggle did not reach the audio.
        assertTrue("expected attenuation, got $filtered vs $loud", filtered < loud / 100f)
    }

    @Test
    fun `switching off takes effect without reconfiguring`() {
        val processor = configured(enabled = true)
        processor.setCutoff(200f)
        val filtered = rms(processor.process(tone(hz = 10_000f)))

        processor.setEnabled(false)
        val restored = rms(processor.process(tone(hz = 10_000f)))

        assertTrue("expected the filter to let go, got $restored vs $filtered", restored > filtered * 100f)
    }

    @Test
    fun `switching off clears filter memory`() {
        val processor = configured(enabled = true)
        processor.setCutoff(200f)
        processor.process(tone(hz = 100f))

        // Off and straight back on: the first sample must not be coloured by what came before, or
        // re-enabling clicks.
        processor.setEnabled(false)
        processor.setEnabled(true)
        val fresh = processor.process(silence())

        assertArrayEquals(silence().array(), fresh)
    }
}

@UnstableApi
private fun configured(enabled: Boolean): BiquadAudioProcessor = BiquadAudioProcessor().apply {
    configure(AudioProcessor.AudioFormat(RATE, CHANNELS, C.ENCODING_PCM_16BIT))
    flush(AudioProcessor.StreamMetadata.DEFAULT)
    setEnabled(enabled)
}

/** Runs one buffer through and returns the bytes the sink would receive. */
@UnstableApi
private fun BiquadAudioProcessor.process(input: ByteBuffer): ByteArray {
    queueInput(input)
    val output = getOutput()
    return ByteArray(output.remaining()).also { output.get(it) }
}

/** Interleaved stereo 16-bit little-endian, both channels identical. */
private fun tone(hz: Float): ByteBuffer {
    val buffer = ByteBuffer.allocate(FRAMES * CHANNELS * 2).order(ByteOrder.nativeOrder())
    for (frame in 0 until FRAMES) {
        val sample = (sin(2.0 * PI * hz * frame / RATE) * 20_000).toInt().toShort()
        repeat(CHANNELS) { buffer.putShort(sample) }
    }
    return buffer.flip() as ByteBuffer
}

private fun silence(): ByteBuffer =
    ByteBuffer.allocate(FRAMES * CHANNELS * 2).order(ByteOrder.nativeOrder())

private fun rms(bytes: ByteArray): Float {
    val samples = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder()).asShortBuffer()
    var sum = 0.0
    while (samples.hasRemaining()) {
        val value = samples.get().toDouble()
        sum += value * value
    }
    return sqrt(sum / (bytes.size / 2)).toFloat()
}
