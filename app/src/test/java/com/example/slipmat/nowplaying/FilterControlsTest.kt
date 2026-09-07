package com.example.slipmat.nowplaying

import com.example.slipmat.core.media.dsp.FILTER_MAX_HZ
import com.example.slipmat.core.media.dsp.FILTER_MIN_HZ
import com.example.slipmat.core.media.dsp.sliderToFrequency
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sqrt

class FilterControlsTest {

    @Test
    fun `the midpoint scale label is the engine's own midpoint frequency, not a typed guess`() {
        // Computed independently of sliderToFrequency: the geometric mean of the engine's own
        // floor and ceiling, which is where a log-mapped slider's true midpoint falls (§5.10).
        val expectedMidpointHz = sqrt(FILTER_MIN_HZ * FILTER_MAX_HZ)

        assertEquals(expectedMidpointHz, sliderToFrequency(0.5f), 0.5f)
        assertEquals("775 Hz", formatCutoff(sliderToFrequency(0.5f)))
    }

    @Test
    fun `the outer scale labels match the engine's own floor and ceiling`() {
        assertEquals("30 Hz", formatCutoff(FILTER_MIN_HZ))
        assertEquals("20.0 kHz", formatCutoff(FILTER_MAX_HZ))
    }

    @Test
    fun `kHz above a thousand, Hz below`() {
        assertEquals("999 Hz", formatCutoff(999f))
        assertEquals("1.0 kHz", formatCutoff(1000f))
    }
}
