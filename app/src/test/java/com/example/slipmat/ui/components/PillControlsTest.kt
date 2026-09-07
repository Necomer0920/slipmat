package com.example.slipmat.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

private val Primary = Color(0xFF9C4234)
private val OnPrimary = Color(0xFFFFFFFF)
private val OnSurfaceVariant = Color(0xFF48464A)
private val OnSurface = Color(0xFF1B1A1D)
private val SurfaceContainerHigh = Color(0xFFE8E6E7)

class PillControlsTest {

    @Test
    fun `pill active and inactive states differ in fill, not only content colour`() {
        val activeContainer = pillContainerColor(selected = true, primary = Primary)
        val inactiveContainer = pillContainerColor(selected = false, primary = Primary)
        assertNotEquals(activeContainer, inactiveContainer)
        assertEquals(Primary, activeContainer)
        assertEquals(Color.Transparent, inactiveContainer)

        val activeContent = pillContentColor(true, OnPrimary, OnSurfaceVariant)
        val inactiveContent = pillContentColor(false, OnPrimary, OnSurfaceVariant)
        assertNotEquals(activeContent, inactiveContent)
    }

    @Test
    fun `a plain chip goes transparent when inactive`() {
        val inactive = chipContainerColor(
            selected = false,
            alwaysFilled = false,
            primary = Primary,
            surfaceContainerHigh = SurfaceContainerHigh,
        )
        assertEquals(Color.Transparent, inactive)
    }

    @Test
    fun `the EQ-chip variant never falls back to transparent, even when inactive`() {
        val inactive = chipContainerColor(
            selected = false,
            alwaysFilled = true,
            primary = Primary,
            surfaceContainerHigh = SurfaceContainerHigh,
        )
        assertNotEquals(Color.Transparent, inactive)
        assertEquals(SurfaceContainerHigh, inactive)

        // Selecting it still swaps the fill, rather than layering a tint over the same container.
        val active = chipContainerColor(
            selected = true,
            alwaysFilled = true,
            primary = Primary,
            surfaceContainerHigh = SurfaceContainerHigh,
        )
        assertNotEquals(inactive, active)
        assertEquals(Primary, active)
    }

    @Test
    fun `segmented toggle active and inactive states differ in fill`() {
        val active = segmentContainerColor(selected = true, surfaceContainerHigh = SurfaceContainerHigh)
        val inactive = segmentContainerColor(selected = false, surfaceContainerHigh = SurfaceContainerHigh)
        assertNotEquals(active, inactive)
        assertEquals(Color.Transparent, inactive)

        val activeContent = segmentContentColor(true, OnSurface, OnSurfaceVariant)
        val inactiveContent = segmentContentColor(false, OnSurface, OnSurfaceVariant)
        assertNotEquals(activeContent, inactiveContent)
    }

    @Test
    fun `the effect dot's filled state tracks enabled, for all three effects alike`() {
        // The dot is a pure function of `enabled` - the same rule for filter, delay and EQ alike,
        // since none of the three is special-cased in the composable that reads this.
        val filterOn = effectDotColor(enabled = true, primary = Primary, onSurfaceVariant = OnSurfaceVariant)
        val filterOff = effectDotColor(enabled = false, primary = Primary, onSurfaceVariant = OnSurfaceVariant)
        val delayOn = effectDotColor(enabled = true, primary = Primary, onSurfaceVariant = OnSurfaceVariant)
        val delayOff = effectDotColor(enabled = false, primary = Primary, onSurfaceVariant = OnSurfaceVariant)
        val eqOn = effectDotColor(enabled = true, primary = Primary, onSurfaceVariant = OnSurfaceVariant)
        val eqOff = effectDotColor(enabled = false, primary = Primary, onSurfaceVariant = OnSurfaceVariant)

        for (on in listOf(filterOn, delayOn, eqOn)) assertEquals(Primary, on)
        for (off in listOf(filterOff, delayOff, eqOff)) assertEquals(OnSurfaceVariant, off)
        assertNotEquals(filterOn, filterOff)
    }
}
