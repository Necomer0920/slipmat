package com.example.slipmat.nowplaying

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BucketPeaksTest {

    /** Spans, computed the same way [bucketPeaks] does, to check against independently. */
    private fun spansFor(peakCount: Int, barCount: Int): List<IntRange> =
        (0 until barCount).map { bar ->
            val from = bar * peakCount / barCount
            val to = if (bar == barCount - 1) peakCount else (bar + 1) * peakCount / barCount
            from until to
        }

    @Test
    fun `every span is non-empty and the spans exactly tile the peak array`() {
        for ((peakCount, barCount) in listOf(1000 to 64, 65 to 64, 64 to 64, 4096 to 64, 200 to 7)) {
            val spans = spansFor(peakCount, barCount)

            for (span in spans) assertTrue("$peakCount peaks into $barCount bars: $span", !span.isEmpty())

            // Non-overlapping and contiguous: each span starts exactly where the previous ended.
            for (i in 1 until spans.size) assertEquals(spans[i - 1].last + 1, spans[i].first)

            assertEquals(0, spans.first().first)
            assertEquals(peakCount, spans.last().last + 1)
        }
    }

    @Test
    fun `each bar is the loudest sample within its own span`() {
        val peaks = FloatArray(256) { index -> index / 256f }

        val bars = bucketPeaks(peaks, 64)

        assertEquals(64, bars.size)
        val spans = spansFor(peaks.size, 64)
        for ((bar, span) in spans.withIndex()) {
            val expected = span.maxOf { peaks[it] }
            assertEquals(expected, bars[bar], 0.0001f)
        }
    }

    @Test
    fun `the last bar's span includes the very last peak`() {
        // 67 peaks into 64 bars divides unevenly - a case worth checking concretely, not just via
        // the span arithmetic above.
        val peaks = FloatArray(67) { 0.1f }
        peaks[peaks.lastIndex] = 0.99f // a unique standout at the very end

        val bars = bucketPeaks(peaks, 64)

        assertEquals(0.99f, bars.last(), 0.0001f)
    }

    @Test
    fun `an empty peak array produces no bars`() {
        assertEquals(0, bucketPeaks(FloatArray(0), 64).size)
    }
}

class ClampTooltipXTest {

    @Test
    fun `at the very start the tooltip's left edge sits at the track's own start`() {
        val left = clampTooltipX(touchX = 0f, tooltipWidth = 48f, trackWidth = 360f)

        assertEquals(0f, left, 0.001f)
    }

    @Test
    fun `at the very end the tooltip's right edge sits at the track's own end, not past it`() {
        val trackWidth = 360f
        val tooltipWidth = 48f

        val left = clampTooltipX(touchX = trackWidth, tooltipWidth = tooltipWidth, trackWidth = trackWidth)

        assertTrue(left >= 0f)
        assertEquals(trackWidth, left + tooltipWidth, 0.001f) // right edge exactly at the track's end
    }

    @Test
    fun `mid-track the tooltip centres on the touch point`() {
        val left = clampTooltipX(touchX = 180f, tooltipWidth = 48f, trackWidth = 360f)

        assertEquals(180f - 24f, left, 0.001f)
    }

    @Test
    fun `a tooltip wider than the track never goes negative`() {
        val left = clampTooltipX(touchX = 0f, tooltipWidth = 400f, trackWidth = 360f)

        assertTrue(left >= 0f)
    }
}
