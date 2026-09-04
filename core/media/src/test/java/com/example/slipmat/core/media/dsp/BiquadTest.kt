package com.example.slipmat.core.media.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.ln

private const val SAMPLE_RATE = 44_100

/** Amplitude ratio expressed in decibels, which is how filter behaviour is actually specified. */
private fun Float.toDb(): Float = (20.0 * ln(this.toDouble()) / ln(10.0)).toFloat()

/**
 * These assert what the filter *does*, not that five numbers equal five other numbers.
 *
 * A transposed sign in the cookbook formulas produces perfectly plausible-looking coefficients and
 * a filter that boosts what it should cut. Only evaluating the response catches that.
 */
class BiquadResponseTest {

    @Test
    fun `a low-pass leaves frequencies well below the cutoff alone`() {
        val filter = biquadCoefficients(FilterMode.LowPass, cutoffHz = 1000f, sampleRate = SAMPLE_RATE)

        assertEquals(0f, filter.magnitudeAt(50f, SAMPLE_RATE).toDb(), 0.5f)
    }

    @Test
    fun `a low-pass strongly attenuates well above the cutoff`() {
        val filter = biquadCoefficients(FilterMode.LowPass, cutoffHz = 1000f, sampleRate = SAMPLE_RATE)

        // Two poles roll off at 12 dB per octave; four octaves up should be far down.
        assertTrue(filter.magnitudeAt(16_000f, SAMPLE_RATE).toDb() < -40f)
    }

    @Test
    fun `a high-pass is the mirror image`() {
        val filter = biquadCoefficients(FilterMode.HighPass, cutoffHz = 1000f, sampleRate = SAMPLE_RATE)

        assertTrue("bass should be removed", filter.magnitudeAt(50f, SAMPLE_RATE).toDb() < -40f)
        assertEquals("treble should pass", 0f, filter.magnitudeAt(16_000f, SAMPLE_RATE).toDb(), 0.5f)
    }

    @Test
    fun `the cutoff is where the response is down three decibels`() {
        // This is what makes the cutoff label mean something. Butterworth Q puts -3 dB exactly here.
        val low = biquadCoefficients(FilterMode.LowPass, cutoffHz = 2000f, sampleRate = SAMPLE_RATE)
        val high = biquadCoefficients(FilterMode.HighPass, cutoffHz = 2000f, sampleRate = SAMPLE_RATE)

        assertEquals(-3.0f, low.magnitudeAt(2000f, SAMPLE_RATE).toDb(), 0.3f)
        assertEquals(-3.0f, high.magnitudeAt(2000f, SAMPLE_RATE).toDb(), 0.3f)
    }

    @Test
    fun `the rolloff is twelve decibels per octave`() {
        val filter = biquadCoefficients(FilterMode.LowPass, cutoffHz = 500f, sampleRate = SAMPLE_RATE)

        val atTwoK = filter.magnitudeAt(2000f, SAMPLE_RATE).toDb()
        val atFourK = filter.magnitudeAt(4000f, SAMPLE_RATE).toDb()

        assertEquals(-12f, atFourK - atTwoK, 1.5f)
    }

    @Test
    fun `a cutoff at or above Nyquist is clamped rather than going unstable`() {
        // An unclamped design here produces a filter that screams instead of one that is wrong.
        val filter = biquadCoefficients(FilterMode.LowPass, cutoffHz = 40_000f, sampleRate = SAMPLE_RATE)

        assertTrue(filter.b0.isFinite() && filter.a1.isFinite() && filter.a2.isFinite())
        assertTrue("passband should stay near unity", abs(filter.magnitudeAt(1000f, SAMPLE_RATE).toDb()) < 1f)
    }

    @Test
    fun `a cutoff below the audible floor is clamped`() {
        val filter = biquadCoefficients(FilterMode.HighPass, cutoffHz = 0f, sampleRate = SAMPLE_RATE)

        assertTrue(filter.b0.isFinite())
    }

    @Test
    fun `an invalid sample rate bypasses instead of dividing by zero`() {
        assertEquals(BiquadCoefficients.BYPASS, biquadCoefficients(FilterMode.LowPass, 1000f, 0))
    }

    @Test
    fun `bypass really does pass everything`() {
        assertEquals(0f, BiquadCoefficients.BYPASS.magnitudeAt(100f, SAMPLE_RATE).toDb(), 0.001f)
        assertEquals(0f, BiquadCoefficients.BYPASS.magnitudeAt(10_000f, SAMPLE_RATE).toDb(), 0.001f)
    }

    @Test
    fun `the filter is stable at every cutoff across the sweep`() {
        // Poles must stay inside the unit circle, or the filter rings away into a burst of noise.
        for (cutoff in listOf(20f, 100f, 500f, 2_000f, 8_000f, 20_000f)) {
            val f = biquadCoefficients(FilterMode.LowPass, cutoff, SAMPLE_RATE)
            assertTrue("|a2| >= 1 at $cutoff Hz means an unstable pole", abs(f.a2) < 1f)
            assertTrue("unstable pole pair at $cutoff Hz", abs(f.a1) < 1f + f.a2)
        }
    }
}
