package com.example.slipmat.core.media.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val RATE = 44_100
private const val CHANNELS = 2
private const val DELAY_MS = 20f

/** 882 frames at 44.1 kHz — short enough to feed in one buffer, long enough to be a real delay. */
private const val DELAY_FRAMES = 882

private const val AMPLITUDE = 20_000
private const val IMPULSE: Short = 20_000
private const val FRAMES = 2_000

/**
 * A delay line is easy to get almost right.
 *
 * An off-by-one lands the echo a sample early; a stride bug reads the other channel's history and
 * comes out as an L/R swap or a comb filter. All three sound roughly like a delay on music, which is
 * why these tests use an impulse in one channel and assert exactly where it reappears.
 */
@UnstableApi
class DelayAudioProcessorTest {

    @Test
    fun `an impulse returns one delay later in the channel it started in`() {
        val processor = configured(enabled = true)

        val output = processor.process(impulseInLeft())

        // Half dry, half wet, so each appearance is half amplitude.
        assertEquals("dry impulse", (IMPULSE / 2).toShort(), output[0])
        assertEquals("dry impulse must not cross channels", 0.toShort(), output[1])

        val echo = DELAY_FRAMES * CHANNELS
        assertEquals("echo lands one delay later", (IMPULSE / 2).toShort(), output[echo])
        assertEquals("echo must not cross channels", 0.toShort(), output[echo + 1])
    }

    @Test
    fun `nothing sounds anywhere else`() {
        val processor = configured(enabled = true)

        val output = processor.process(impulseInLeft())

        val loud = output.indices.filter { output[it] != 0.toShort() }
        assertEquals(listOf(0, DELAY_FRAMES * CHANNELS), loud)
    }

    @Test
    fun `the echo moves when the delay length does`() {
        val processor = configured(enabled = true)
        processor.setDelayMs(40f)

        val output = processor.process(impulseInLeft())

        val loud = output.indices.filter { output[it] != 0.toShort() }
        assertEquals(listOf(0, DELAY_FRAMES * 2 * CHANNELS), loud)
    }

    @Test
    fun `feedback repeats the echo, quieter each time`() {
        val processor = configured(enabled = true)
        processor.setFeedback(0.5f)

        val output = processor.process(impulseInLeft())

        val echoes = (1..2).map { output[DELAY_FRAMES * CHANNELS * it] }
        assertTrue("expected a second repeat, got $echoes", echoes[1] != 0.toShort())
        assertTrue("repeats must decay, got $echoes", echoes[1] < echoes[0])
    }

    @Test
    fun `sustained material at full feedback does not build up`() {
        val processor = configured(enabled = true)
        processor.setFeedback(1f) // clamped to the ceiling

        // Long enough for the line to turn over ~90 times, well past steady state.
        var peak = 0
        repeat(40) {
            peak = maxOf(peak, processor.process(loudTone()).maxOf { kotlin.math.abs(it.toInt()) })
        }

        // The naive `dry + wet * fb` saturates the line here and comes out above 26,000.
        assertTrue("output grew to $peak", peak <= AMPLITUDE + 1_000)
    }

    @Test
    fun `feedback cannot be pushed past the ceiling`() {
        val processor = configured(enabled = true)
        processor.setFeedback(5f)

        val output = processor.process(impulseInLeft())

        // Still decaying rather than holding: an echo at the ceiling is quieter than the one before.
        val first = output[DELAY_FRAMES * CHANNELS]
        val second = output[DELAY_FRAMES * CHANNELS * 2]
        assertTrue("no decay at the ceiling: $first then $second", second < first)
    }

    @Test
    fun `fully dry, the echo is not in the output at all`() {
        val processor = configured(enabled = true)
        processor.setFeedback(0.5f)
        processor.setMix(0f)

        val output = processor.process(impulseInLeft())

        assertArrayEquals(shortsOf(impulseInLeft()), output)
    }

    @Test
    fun `fully wet, the dry signal is gone`() {
        val processor = configured(enabled = true)
        processor.setMix(1f)

        val output = processor.process(impulseInLeft())

        assertEquals("dry signal survived a fully wet mix", 0.toShort(), output[0])
        assertEquals("echo should be at full level", IMPULSE, output[DELAY_FRAMES * CHANNELS])
    }

    @Test
    fun `switched off, it passes audio through untouched`() {
        val processor = configured(enabled = false)

        val output = processor.process(impulseInLeft())

        assertArrayEquals(shortsOf(impulseInLeft()), output)
    }

    @Test
    fun `switching off empties the line`() {
        val processor = configured(enabled = true)
        processor.process(impulseInLeft())

        // Straight off and back on: the echo that was still in flight must not reappear.
        processor.setEnabled(false)
        processor.setEnabled(true)
        val output = processor.process(silence())

        assertTrue("stale echo survived the switch", output.all { it == 0.toShort() })
    }

    @Test
    fun `a seek empties the line`() {
        val processor = configured(enabled = true)
        processor.process(impulseInLeft())

        processor.flush()
        val output = processor.process(silence())

        assertTrue("tail dragged across the seek", output.all { it == 0.toShort() })
    }

    @Test
    fun `delay length is measured in time, not samples`() {
        // The same 500 ms has to be a different number of frames at a different rate, or the echo
        // shifts whenever a track with another sample rate loads.
        assertEquals(22_050, framesFor(500f, 44_100))
        assertEquals(24_000, framesFor(500f, 48_000))
    }

    @Test
    fun `the control cannot ask for more line than exists`() {
        val processor = configured(enabled = true)
        processor.setDelayMs(MAX_DELAY_MS * 10)

        // Clamped rather than wrapping into whatever the modulo produces.
        val output = processor.process(impulseInLeft())

        assertEquals((IMPULSE / 2).toShort(), output[0])
    }
}

@UnstableApi
private fun configured(enabled: Boolean): DelayAudioProcessor = DelayAudioProcessor().apply {
    configure(AudioProcessor.AudioFormat(RATE, CHANNELS, C.ENCODING_PCM_16BIT))
    flush()
    setEnabled(enabled)
    setDelayMs(DELAY_MS)
}

@UnstableApi
private fun DelayAudioProcessor.process(input: ByteBuffer): ShortArray {
    queueInput(input)
    val output = getOutput()
    val shorts = ShortArray(output.remaining() / 2)
    output.order(ByteOrder.nativeOrder()).asShortBuffer().get(shorts)
    return shorts
}

/** One sample in the left channel of the first frame; silence everywhere else. */
private fun impulseInLeft(): ByteBuffer {
    val buffer = ByteBuffer.allocate(FRAMES * CHANNELS * 2).order(ByteOrder.nativeOrder())
    buffer.putShort(IMPULSE)
    buffer.putShort(0)
    repeat((FRAMES - 1) * CHANNELS) { buffer.putShort(0) }
    return buffer.flip() as ByteBuffer
}

/** Full-level low tone: at 20 ms the echoes come back in phase and add, which is the worst case. */
private fun loudTone(): ByteBuffer {
    val buffer = ByteBuffer.allocate(FRAMES * CHANNELS * 2).order(ByteOrder.nativeOrder())
    for (frame in 0 until FRAMES) {
        val sample = (kotlin.math.sin(2.0 * Math.PI * 100.0 * frame / RATE) * AMPLITUDE).toInt().toShort()
        repeat(CHANNELS) { buffer.putShort(sample) }
    }
    return buffer.flip() as ByteBuffer
}

private fun silence(): ByteBuffer =
    ByteBuffer.allocate(FRAMES * CHANNELS * 2).order(ByteOrder.nativeOrder())

private fun shortsOf(buffer: ByteBuffer): ShortArray {
    val shorts = ShortArray(buffer.remaining() / 2)
    buffer.order(ByteOrder.nativeOrder()).asShortBuffer().get(shorts)
    return shorts
}
