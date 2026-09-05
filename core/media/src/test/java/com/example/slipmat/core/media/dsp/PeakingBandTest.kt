package com.example.slipmat.core.media.dsp

import kotlin.math.ln
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val SAMPLE_RATE = 44_100

private fun Float.toDb(): Float = (20.0 * ln(this.toDouble()) / ln(10.0)).toFloat()

/**
 * A peaking band's whole contract is "give me exactly this many dB, here".
 *
 * The formula for one is `A = 10^(gainDb/40)`, and writing 20 there is the classic transcription of
 * it. The result is a filter wrong by a factor of two in dB, which draws a perfectly plausible
 * curve, boosts audibly, and passes every test that only asks whether the sound changed. Asking for
 * the gain *by number* is the only check that catches it.
 */
class PeakingBandTest {

    @Test
    fun `a band delivers exactly the gain it was asked for`() {
        for (gainDb in listOf(-12f, -6f, -3f, 3f, 6f, 12f)) {
            val band = peakingCoefficients(centerHz = 1000f, gainDb = gainDb, sampleRate = SAMPLE_RATE)

            assertEquals(
                "asked for $gainDb dB at the centre",
                gainDb,
                band.magnitudeAt(1000f, SAMPLE_RATE).toDb(),
                0.01f,
            )
        }
    }

    @Test
    fun `a band leaves distant frequencies alone`() {
        val band = peakingCoefficients(centerHz = 1000f, gainDb = 12f, sampleRate = SAMPLE_RATE)

        // Five octaves down and four up: a band that reaches this far is not a band.
        assertEquals(0f, band.magnitudeAt(31f, SAMPLE_RATE).toDb(), 0.5f)
        assertEquals(0f, band.magnitudeAt(16_000f, SAMPLE_RATE).toDb(), 0.5f)
    }

    @Test
    fun `zero gain is a straight wire`() {
        val band = peakingCoefficients(centerHz = 1000f, gainDb = 0f, sampleRate = SAMPLE_RATE)

        for (hz in listOf(50f, 500f, 1000f, 5000f, 15_000f)) {
            assertEquals("$hz Hz", 0f, band.magnitudeAt(hz, SAMPLE_RATE).toDb(), 0.001f)
        }
    }

    @Test
    fun `a cut is the mirror image of a boost`() {
        val boost = peakingCoefficients(centerHz = 500f, gainDb = 9f, sampleRate = SAMPLE_RATE)
        val cut = peakingCoefficients(centerHz = 500f, gainDb = -9f, sampleRate = SAMPLE_RATE)

        for (hz in listOf(200f, 500f, 1200f)) {
            assertEquals(
                "$hz Hz",
                boost.magnitudeAt(hz, SAMPLE_RATE).toDb(),
                -cut.magnitudeAt(hz, SAMPLE_RATE).toDb(),
                0.01f,
            )
        }
    }

    @Test
    fun `the band is roughly as wide as its spacing`() {
        val band = peakingCoefficients(centerHz = 1000f, gainDb = 12f, sampleRate = SAMPLE_RATE)

        // Q is derived from the band spacing, so a neighbouring centre should still see a fair
        // share of this band. Too narrow and the curve the user drags does not match what is heard.
        val atNeighbour = band.magnitudeAt(1000f * EQ_BAND_SPACING, SAMPLE_RATE).toDb()
        assertTrue("neighbour sees $atNeighbour dB", atNeighbour > 2f && atNeighbour < 7f)
    }

    @Test
    fun `every band centre sits inside the audible range and below Nyquist`() {
        assertTrue(EQ_BANDS.first() >= 20f)
        assertTrue(EQ_BANDS.last() < SAMPLE_RATE / 2f)
        // Ascending, because the UI draws them left to right.
        assertEquals(EQ_BANDS.sorted(), EQ_BANDS)
    }
}
