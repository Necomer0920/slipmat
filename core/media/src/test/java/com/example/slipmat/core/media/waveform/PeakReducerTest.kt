package com.example.slipmat.core.media.waveform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The waveform is the thing users scrub against, so its shape has to correspond to the audio.
 * These use synthetic PCM with known peaks, so a wrong answer is unambiguous.
 */
class PeakReducerTest {

    private fun reduce(samples: ShortArray, buckets: Int): FloatArray {
        val reducer = PeakReducer(bucketCount = buckets, totalSampleCount = samples.size.toLong())
        reducer.add(samples, samples.size)
        return reducer.peaks()
    }

    @Test
    fun `silence reduces to a flat line rather than dividing by zero`() {
        val peaks = reduce(ShortArray(1000), buckets = 10)

        assertEquals(10, peaks.size)
        assertTrue(peaks.all { it == 0f })
    }

    @Test
    fun `a constant tone fills every bucket equally`() {
        val peaks = reduce(ShortArray(1000) { 8000 }, buckets = 10)

        assertTrue(peaks.all { it == 1f })
    }

    @Test
    fun `a loud burst lands in the bucket that contains it`() {
        // Quiet everywhere, one loud sample a quarter of the way in.
        val samples = ShortArray(1000) { 100 }
        samples[250] = Short.MAX_VALUE

        val peaks = reduce(samples, buckets = 10)

        assertEquals(1f, peaks[2], 0.0001f)
        assertTrue("the burst leaked into a neighbouring bucket", peaks[3] < 0.1f)
    }

    @Test
    fun `negative peaks count as loud, since amplitude is what is drawn`() {
        val samples = ShortArray(100) { 0 }
        samples[50] = (-Short.MAX_VALUE).toShort()

        val peaks = reduce(samples, buckets = 4)

        assertEquals(1f, peaks[2], 0.0001f)
    }

    @Test
    fun `a quiet recording still fills the canvas`() {
        // Un-normalised this draws as a nearly flat line, which is useless to scrub against.
        val peaks = reduce(ShortArray(400) { 300 }, buckets = 4)

        assertTrue(peaks.all { it == 1f })
    }

    @Test
    fun `relative loudness survives normalisation`() {
        val samples = ShortArray(400) { if (it < 200) 1000 else 4000 }

        val peaks = reduce(samples, buckets = 2)

        assertEquals(0.25f, peaks[0], 0.0001f)
        assertEquals(1f, peaks[1], 0.0001f)
    }

    @Test
    fun `a stream longer than predicted does not overflow the buckets`() {
        // Duration metadata is often approximate, so the real sample count can exceed the estimate.
        val reducer = PeakReducer(bucketCount = 4, totalSampleCount = 100)
        reducer.add(ShortArray(400) { 5000 }, 400)

        val peaks = reducer.peaks()

        assertEquals(4, peaks.size)
        assertEquals(1f, peaks[3], 0.0001f)
    }

    @Test
    fun `a stream shorter than predicted leaves the tail empty rather than failing`() {
        val reducer = PeakReducer(bucketCount = 4, totalSampleCount = 400)
        reducer.add(ShortArray(100) { 5000 }, 100)

        val peaks = reducer.peaks()

        assertEquals(1f, peaks[0], 0.0001f)
        assertEquals(0f, peaks[3], 0.0001f)
    }

    @Test
    fun `an unknown total sample count does not crash`() {
        val reducer = PeakReducer(bucketCount = 4, totalSampleCount = 0)
        reducer.add(ShortArray(50) { 1000 }, 50)

        assertEquals(4, reducer.peaks().size)
    }
}
