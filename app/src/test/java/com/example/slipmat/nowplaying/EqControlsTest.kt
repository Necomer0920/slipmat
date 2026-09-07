package com.example.slipmat.nowplaying

import com.example.slipmat.core.media.dsp.EQ_BANDS
import com.example.slipmat.core.media.dsp.EQ_MAX_GAIN_DB
import com.example.slipmat.core.media.dsp.curveFractionFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqControlsTest {

    private val height = 160f

    @Test
    fun `plus 12dB sits at the top edge, minus 12 at the bottom, 0 on the zero line`() {
        assertEquals(0f, yForDb(EQ_MAX_GAIN_DB, height), 0.001f)
        assertEquals(height, yForDb(-EQ_MAX_GAIN_DB, height), 0.001f)
        assertEquals(height / 2f, yForDb(0f, height), 0.001f)
    }

    @Test
    fun `y and dB round-trip through each other`() {
        for (db in listOf(-12f, -6f, 0f, 6f, 12f)) {
            val y = yForDb(db, height)
            assertEquals(db, dbForY(y, height), 0.001f)
        }
    }

    @Test
    fun `gain is clamped to the pm12dB range rather than drawn off-canvas`() {
        assertEquals(0f, yForDb(20f, height), 0.001f)
        assertEquals(height, yForDb(-20f, height), 0.001f)
    }

    @Test
    fun `the outermost handles land inside the drawable width, not on its edges`() {
        val firstBandFraction = curveFractionFor(EQ_BANDS.first())
        val lastBandFraction = curveFractionFor(EQ_BANDS.last())

        assertTrue(firstBandFraction > 0f)
        assertTrue(firstBandFraction < 1f)
        assertTrue(lastBandFraction > 0f)
        assertTrue(lastBandFraction < 1f)
    }

    @Test
    fun `a drag snaps to the nearest whole dB`() {
        // y values chosen to land between two whole-dB rungs, so rounding is the only thing that
        // can be under test - an unsnapped dbForY would return a fractional value here.
        val nearSevenDb = yForDb(7.4f, height)
        assertEquals(7f, snappedDbForY(nearSevenDb, height), 0.001f)

        val nearMinusThreeDb = yForDb(-2.6f, height)
        assertEquals(-3f, snappedDbForY(nearMinusThreeDb, height), 0.001f)
    }

    @Test
    fun `a drag past either edge clamps to plus or minus 12, not beyond`() {
        assertEquals(12f, snappedDbForY(-50f, height), 0.001f)
        assertEquals(-12f, snappedDbForY(height + 50f, height), 0.001f)
    }

    @Test
    fun `band labels read the spec's own text, not a typed guess`() {
        assertEquals("60", formatEqBandLabel(60f))
        assertEquals("130", formatEqBandLabel(130f))
        assertEquals("300", formatEqBandLabel(300f))
        assertEquals("650", formatEqBandLabel(650f))
        assertEquals("1.5k", formatEqBandLabel(1500f))
        assertEquals("3.2k", formatEqBandLabel(3200f))
        assertEquals("7k", formatEqBandLabel(7000f))
        assertEquals("16k", formatEqBandLabel(16000f))
    }
}
