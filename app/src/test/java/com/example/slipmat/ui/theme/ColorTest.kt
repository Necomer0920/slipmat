package com.example.slipmat.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test

/** WCAG AA for normal text — the bar §0's tie-break rule holds every fixed pairing to. */
private const val MIN_CONTRAST = 4.5f

class ColorTest {

    @Test
    fun `onSurface clears WCAG AA on surface, in both ramps`() {
        val dark = contrastRatio(OnSurfaceDark, SurfaceDark)
        val light = contrastRatio(OnSurfaceLight, SurfaceLight)

        assertTrue("dark onSurface/surface: ${"%.2f".format(dark)}:1", dark >= MIN_CONTRAST)
        assertTrue("light onSurface/surface: ${"%.2f".format(light)}:1", light >= MIN_CONTRAST)
    }

    @Test
    fun `onSurfaceVariant clears WCAG AA on surfaceContainerHighest, in both ramps`() {
        // The pairing WaveformSeekBar's unplayed bars rely on (§3.1's surfaceVariant alias).
        val dark = contrastRatio(OnSurfaceVariantDark, SurfaceContainerHighestDark)
        val light = contrastRatio(OnSurfaceVariantLight, SurfaceContainerHighestLight)

        assertTrue("dark onSurfaceVariant/surfaceContainerHighest: ${"%.2f".format(dark)}:1", dark >= MIN_CONTRAST)
        assertTrue("light onSurfaceVariant/surfaceContainerHighest: ${"%.2f".format(light)}:1", light >= MIN_CONTRAST)
    }

    @Test
    fun `surfaceVariant is pinned to surfaceContainerHighest, in both ramps`() {
        assertTrue(SurfaceVariantDark == SurfaceContainerHighestDark)
        assertTrue(SurfaceVariantLight == SurfaceContainerHighestLight)
    }
}
