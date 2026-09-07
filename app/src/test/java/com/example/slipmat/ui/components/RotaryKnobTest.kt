package com.example.slipmat.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class RotaryKnobTest {

    @Test
    fun `the minimum gives -135 degrees, the maximum +135, the midpoint 0`() {
        assertEquals(-135f, rotationForValue(value = 0f, min = 0f, max = 100f), 0.001f)
        assertEquals(135f, rotationForValue(value = 100f, min = 0f, max = 100f), 0.001f)
        assertEquals(0f, rotationForValue(value = 50f, min = 0f, max = 100f), 0.001f)
    }

    @Test
    fun `rotation and value round-trip through each other`() {
        val range = 0f..100f
        for (value in listOf(0f, 25f, 50f, 75f, 100f)) {
            val rotation = rotationForValue(value, range.start, range.endInclusive)
            assertEquals(value, valueForRotation(rotation, range.start, range.endInclusive), 0.001f)
        }
    }

    @Test
    fun `a 150px upward drag from the minimum reaches the maximum exactly`() {
        val reached = valueForDrag(startValue = 0f, dragUpPx = 150f, min = 0f, max = 100f)
        assertEquals(100f, reached, 0.001f)
    }

    @Test
    fun `a 150px downward drag from the maximum reaches the minimum exactly`() {
        val reached = valueForDrag(startValue = 100f, dragUpPx = -150f, min = 0f, max = 100f)
        assertEquals(0f, reached, 0.001f)
    }

    @Test
    fun `a drag past either end clamps rather than overshoots`() {
        assertEquals(100f, valueForDrag(startValue = 0f, dragUpPx = 300f, min = 0f, max = 100f), 0.001f)
        assertEquals(0f, valueForDrag(startValue = 100f, dragUpPx = -300f, min = 0f, max = 100f), 0.001f)
    }
}
