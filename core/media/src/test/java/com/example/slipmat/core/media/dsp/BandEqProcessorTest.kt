package com.example.slipmat.core.media.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val RATE = 44_100
private const val CHANNELS = 2
private const val FRAMES = 16_384

/** Low enough that a +12 dB boost (about 4x) still has headroom before clipping. */
private const val AMPLITUDE = 4_000

private fun Float.toDb(): Float = (20.0 * ln(this.toDouble()) / ln(10.0)).toFloat()

/**
 * The EQ has two claims to check, and they fail in different ways.
 *
 * That the *cascade* is a product and not a sum — an additive curve looks right on a single band
 * and over-promises wherever two overlap. And that the processor actually delivers those numbers to
 * the audio, which is a separate question from whether the coefficients are right.
 */
@UnstableApi
class BandEqProcessorTest {

    @Test
    fun `bands in series multiply rather than add`() {
        // Halfway between two neighbours, where both contribute. Each alone gives some boost; the
        // pair must give their product, which in dB is their sum - but computed from the product.
        val between = sqrt(EQ_BANDS[3] * EQ_BANDS[4])
        val lowOnly = FloatArray(EQ_BANDS.size).also { it[3] = 6f }
        val highOnly = FloatArray(EQ_BANDS.size).also { it[4] = 6f }
        val both = FloatArray(EQ_BANDS.size).also { it[3] = 6f; it[4] = 6f }

        val product = eqMagnitudeAt(lowOnly, between, RATE) * eqMagnitudeAt(highOnly, between, RATE)

        assertEquals(product, eqMagnitudeAt(both, between, RATE), 0.001f)
    }

    @Test
    fun `the drawn curve is the same function as the per-frequency one`() {
        // eqCurveDb designs each band once and reuses it; eqMagnitudeAt designs per call. If those
        // ever diverge, the curve stops describing the audio and nothing else would notice.
        val gains = FloatArray(EQ_BANDS.size).also { it[1] = 8f; it[2] = -5f; it[6] = 11f }
        val points = 64
        val curve = eqCurveDb(gains, RATE, points)
        val ratio = CURVE_MAX_HZ / CURVE_MIN_HZ

        for (point in 0 until points) {
            val hz = CURVE_MIN_HZ *
                Math.pow(ratio.toDouble(), point.toDouble() / (points - 1)).toFloat()
            assertEquals("$hz Hz", eqMagnitudeAt(gains, hz, RATE).toDb(), curve[point], 0.01f)
        }
    }

    @Test
    fun `the drawn axis runs from one end of the range to the other`() {
        assertEquals(0f, curveFractionFor(CURVE_MIN_HZ), 0.001f)
        assertEquals(1f, curveFractionFor(CURVE_MAX_HZ), 0.001f)
        // Log axis: the geometric midpoint sits halfway across, not the arithmetic one.
        assertEquals(0.5f, curveFractionFor(sqrt(CURVE_MIN_HZ * CURVE_MAX_HZ)), 0.001f)
    }

    @Test
    fun `every band centre lands somewhere on the drawn axis`() {
        for (hz in EQ_BANDS) {
            val fraction = curveFractionFor(hz)
            assertTrue("$hz Hz sits at $fraction", fraction > 0f && fraction < 1f)
        }
    }

    @Test
    fun `a flat EQ is a straight wire at every band centre`() {
        val flat = FloatArray(EQ_BANDS.size)

        for (hz in EQ_BANDS) {
            assertEquals("$hz Hz", 0f, eqMagnitudeAt(flat, hz, RATE).toDb(), 0.001f)
        }
    }

    @Test
    fun `the processor delivers the boost it was asked for`() {
        val band = 3
        val processor = configured(enabled = true)
        processor.setGain(band, EQ_MAX_GAIN_DB)

        val plain = rms(configured(enabled = false).process(tone(EQ_BANDS[band])))
        val boosted = rms(processor.process(tone(EQ_BANDS[band])))

        // The neighbours contribute at this frequency too, so compare against what the curve says
        // rather than against the band's own gain in isolation.
        val expected = eqMagnitudeAt(processor.gains(), EQ_BANDS[band], RATE)
        assertEquals("gain at ${EQ_BANDS[band]} Hz", expected, boosted / plain, 0.15f)
    }

    @Test
    fun `the processor delivers a cut too`() {
        val band = 4
        val processor = configured(enabled = true)
        processor.setGain(band, -EQ_MAX_GAIN_DB)

        val plain = rms(configured(enabled = false).process(tone(EQ_BANDS[band])))
        val cut = rms(processor.process(tone(EQ_BANDS[band])))

        assertTrue("expected a cut, got ${cut / plain}", cut / plain < 0.4f)
    }

    @Test
    fun `a flat EQ leaves the audio alone`() {
        val processor = configured(enabled = true)

        val output = processor.process(tone(1000f))

        // Every band is BYPASS, so this must be sample-for-sample identical, not merely close.
        assertArrayEquals(shortsOf(tone(1000f)), output)
    }

    @Test
    fun `switched off, it passes audio through untouched`() {
        val processor = configured(enabled = false)
        processor.setGain(2, EQ_MAX_GAIN_DB)

        val output = processor.process(tone(EQ_BANDS[2]))

        assertArrayEquals(shortsOf(tone(EQ_BANDS[2])), output)
    }

    @Test
    fun `stays in the chain while switched off`() {
        // Same rule as the filter: answering isActive with the switch freezes the EQ in whatever
        // state it held when the track started.
        assertTrue(configured(enabled = false).isActive)
    }

    @Test
    fun `gains are clamped to the range the UI offers`() {
        val processor = configured(enabled = true)

        processor.setGain(0, 100f)
        processor.setGain(1, -100f)

        assertEquals(EQ_MAX_GAIN_DB, processor.gains()[0], 0.001f)
        assertEquals(-EQ_MAX_GAIN_DB, processor.gains()[1], 0.001f)
    }

    @Test
    fun `each band moves its own frequency`() {
        // A band-major state index that mixed up its stride would still boost, just the wrong band.
        for (band in EQ_BANDS.indices) {
            val gains = FloatArray(EQ_BANDS.size).also { it[band] = EQ_MAX_GAIN_DB }
            val peak = EQ_BANDS.indices.maxBy { eqMagnitudeAt(gains, EQ_BANDS[it], RATE) }

            assertEquals("band $band peaked at band $peak", band, peak)
        }
    }
}

@UnstableApi
private fun configured(enabled: Boolean): BandEqProcessor = BandEqProcessor().apply {
    configure(AudioProcessor.AudioFormat(RATE, CHANNELS, C.ENCODING_PCM_16BIT))
    flush(AudioProcessor.StreamMetadata.DEFAULT)
    setEnabled(enabled)
}

@UnstableApi
private fun BandEqProcessor.process(input: ByteBuffer): ShortArray {
    queueInput(input)
    val output = getOutput()
    val shorts = ShortArray(output.remaining() / 2)
    output.order(ByteOrder.nativeOrder()).asShortBuffer().get(shorts)
    return shorts
}

private fun tone(hz: Float): ByteBuffer {
    val buffer = ByteBuffer.allocate(FRAMES * CHANNELS * 2).order(ByteOrder.nativeOrder())
    for (frame in 0 until FRAMES) {
        val sample = (sin(2.0 * PI * hz * frame / RATE) * AMPLITUDE).toInt().toShort()
        repeat(CHANNELS) { buffer.putShort(sample) }
    }
    return buffer.flip() as ByteBuffer
}

private fun shortsOf(buffer: ByteBuffer): ShortArray {
    val shorts = ShortArray(buffer.remaining() / 2)
    buffer.order(ByteOrder.nativeOrder()).asShortBuffer().get(shorts)
    return shorts
}

/** Measured over the back half only, so the filters' start-up transient is not counted. */
private fun rms(samples: ShortArray): Float {
    var sum = 0.0
    var count = 0
    for (i in samples.size / 2 until samples.size) {
        val value = samples[i].toDouble()
        sum += value * value
        count++
    }
    return sqrt(sum / count).toFloat().also { require(abs(it) > 0f) }
}
