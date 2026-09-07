package com.example.slipmat.nowplaying

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchTempoControlsTest {

    @Test
    fun `plus 8 percent formats as a signed percentage and a 1_08x multiplier`() {
        assertEquals("+8.0%", formatTempoPercent(8f))
        assertEquals("1.08×", formatSpeedMultiplier(8f))
    }

    @Test
    fun `zero formats as a signed zero percentage and a 1_00x multiplier`() {
        assertEquals("+0.0%", formatTempoPercent(0f))
        assertEquals("1.00×", formatSpeedMultiplier(0f))
    }

    @Test
    fun `negative values carry their own sign rather than a duplicated one`() {
        assertEquals("-8.0%", formatTempoPercent(-8f))
        assertEquals("0.70×", formatSpeedMultiplier(-30f))
    }

    @Test
    fun `positive values are always signed, never bare`() {
        assertTrue(formatTempoPercent(8f).startsWith("+"))
        assertTrue(formatTempoPercent(0f).startsWith("+"))
    }

    @Test
    fun `the key status line's semitone figure is signed like everything else here`() {
        assertEquals("+1.3 st", formatSemitones(1.3f))
        assertEquals("-1.4 st", formatSemitones(-1.4f))
        assertEquals("+0.0 st", formatSemitones(0f))
    }
}
