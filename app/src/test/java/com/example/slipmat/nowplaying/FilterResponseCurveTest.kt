package com.example.slipmat.nowplaying

import com.example.slipmat.core.media.dsp.FilterMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterResponseCurveTest {

    @Test
    fun `low-pass is flat left of the cutoff and falls monotonically right of it`() {
        val points = filterResponseCurve(cutoffPosition = 0.5f, mode = FilterMode.LowPass, pointCount = 21)
        val cutoffIndex = 10 // x = i / 20, so x = 0.5 lands exactly on index 10.

        for (i in 0..cutoffIndex) assertEquals(points[0], points[i], 0.0001f)
        for (i in cutoffIndex until points.size - 1) assertTrue(points[i] > points[i + 1])
    }

    @Test
    fun `high-pass mirrors low-pass - flat right of the cutoff, rising into it from the left`() {
        val points = filterResponseCurve(cutoffPosition = 0.5f, mode = FilterMode.HighPass, pointCount = 21)
        val cutoffIndex = 10

        for (i in cutoffIndex until points.size) assertEquals(points.last(), points[i], 0.0001f)
        for (i in 0 until cutoffIndex) assertTrue(points[i] < points[i + 1])
    }

    @Test
    fun `the corner tracks the cutoff, not a fixed midpoint`() {
        val points = filterResponseCurve(cutoffPosition = 0.25f, mode = FilterMode.LowPass, pointCount = 21)
        val cutoffIndex = 5 // x = i / 20, so x = 0.25 lands exactly on index 5.

        for (i in 0..cutoffIndex) assertEquals(points[0], points[i], 0.0001f)
        assertTrue(points[cutoffIndex] > points[cutoffIndex + 1])
    }

    @Test
    fun `the line never fully bottoms out, even swept to the far edge`() {
        val lowPass = filterResponseCurve(cutoffPosition = 0f, mode = FilterMode.LowPass, pointCount = 21)
        val highPass = filterResponseCurve(cutoffPosition = 1f, mode = FilterMode.HighPass, pointCount = 21)

        assertTrue(lowPass.last() > 0f)
        assertTrue(highPass.first() > 0f)
    }
}
