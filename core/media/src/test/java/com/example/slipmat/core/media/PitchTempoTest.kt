package com.example.slipmat.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The difference between the two key-lock modes is the whole feature, and it is one boolean away
 * from being silently wrong: with pitch left at 1.0 in vinyl mode, slowing a track sounds like
 * time-stretching instead of a record slowing down, and nothing crashes to tell you.
 */
class KeyLockTest {

    @Test
    fun `key lock on shifts tempo and leaves the key alone`() {
        val result = speedPitchFor(sliderValue = 1f, range = PitchRange.Narrow, keyLock = true)

        assertEquals(1.08f, result.speed, 0.0001f)
        assertEquals(1f, result.pitch, 0.0001f)
    }

    @Test
    fun `key lock off moves pitch with tempo, like a turntable`() {
        val result = speedPitchFor(sliderValue = 1f, range = PitchRange.Narrow, keyLock = false)

        assertEquals(1.08f, result.speed, 0.0001f)
        assertEquals(1.08f, result.pitch, 0.0001f)
    }

    @Test
    fun `slowing down with key lock off drops the pitch below normal`() {
        // A slowed record goes deeper. If this ever returns pitch >= 1 the vinyl mode is broken.
        val result = speedPitchFor(sliderValue = -1f, range = PitchRange.Wide, keyLock = false)

        assertEquals(0.5f, result.speed, 0.0001f)
        assertEquals(0.5f, result.pitch, 0.0001f)
        assertTrue(result.pitch < 1f)
    }

    @Test
    fun `centre is exactly normal in both modes`() {
        assertEquals(SpeedPitch.NORMAL, speedPitchFor(0f, PitchRange.Wide, keyLock = true))
        assertEquals(SpeedPitch.NORMAL, speedPitchFor(0f, PitchRange.Wide, keyLock = false))
    }

    @Test
    fun `each range scales the same slider travel differently`() {
        assertEquals(1.08f, speedPitchFor(1f, PitchRange.Narrow, true).speed, 0.0001f)
        assertEquals(1.16f, speedPitchFor(1f, PitchRange.Medium, true).speed, 0.0001f)
        assertEquals(1.50f, speedPitchFor(1f, PitchRange.Wide, true).speed, 0.0001f)
    }

    @Test
    fun `half travel gives half the range`() {
        assertEquals(1.25f, speedPitchFor(0.5f, PitchRange.Wide, true).speed, 0.0001f)
    }

    @Test
    fun `slider values beyond the ends are clamped`() {
        assertEquals(1.5f, speedPitchFor(2f, PitchRange.Wide, true).speed, 0.0001f)
        assertEquals(0.5f, speedPitchFor(-2f, PitchRange.Wide, true).speed, 0.0001f)
    }
}

class DetentTest {

    @Test
    fun `a value just off centre snaps to exactly normal`() {
        // Otherwise the track runs at 1.003x and drifts against the original imperceptibly slowly.
        assertEquals(0f, snapToDetent(0.01f), 0f)
        assertEquals(0f, snapToDetent(-0.01f), 0f)
        assertTrue(isAtDetent(0.015f))
    }

    @Test
    fun `a value outside the detent is left alone`() {
        assertEquals(0.5f, snapToDetent(0.5f), 0.0001f)
        assertFalse(isAtDetent(0.5f))
    }

    @Test
    fun `the detent produces exactly normal playback, not merely close to it`() {
        val result = speedPitchFor(0.015f, PitchRange.Wide, keyLock = false)

        assertEquals(1f, result.speed, 0f)
        assertEquals(1f, result.pitch, 0f)
    }

    @Test
    fun `the percentage readout reads zero inside the detent`() {
        assertEquals(0f, tempoPercent(0.015f, PitchRange.Wide), 0f)
        assertEquals(-8f, tempoPercent(-1f, PitchRange.Narrow), 0.0001f)
        assertEquals(16f, tempoPercent(1f, PitchRange.Medium), 0.0001f)
    }

    @Test
    fun `the redesign's fixed range reads plus-or-minus 30 percent at full travel`() {
        // §5.2 - the README's own worked number for the ±30% range this screen is fixed to.
        assertEquals(30f, tempoPercent(1f, PitchRange.Standard), 0.0001f)
        assertEquals(-30f, tempoPercent(-1f, PitchRange.Standard), 0.0001f)
    }

    @Test
    fun `position and percentage round-trip through each other at the fixed range`() {
        for (percent in listOf(-30f, -12.5f, -1.05f, 0f, 4f, 17.3f, 30f)) {
            val sliderValue = sliderValueForPercent(percent, PitchRange.Standard)
            assertEquals(percent, tempoPercent(sliderValue, PitchRange.Standard), 0.01f)
        }
    }

    @Test
    fun `at range 30 the detent is exactly 1_05 points wide, not the old 8-percent-range width`() {
        // range x 0.035 = 30 x 0.035 = 1.05 - the README's own worked number (§4.2).
        assertEquals(0f, tempoPercent(sliderValueForPercent(1.049f, PitchRange.Standard), PitchRange.Standard), 0f)
        assertEquals(0f, tempoPercent(sliderValueForPercent(-1.049f, PitchRange.Standard), PitchRange.Standard), 0f)

        // Just outside the detent, the value survives untouched.
        val justOutside = tempoPercent(sliderValueForPercent(1.06f, PitchRange.Standard), PitchRange.Standard)
        assertTrue("expected just outside the detent, got $justOutside", justOutside != 0f)
    }
}

class BpmReadoutTest {

    @Test
    fun `an unknown source tempo yields no readout at all`() {
        // Nothing in this app detects BPM, so a guess would be worse than saying nothing.
        assertNull(playingBpm(sourceBpm = null, sliderValue = 0.5f, range = PitchRange.Wide))
        assertNull(playingBpm(sourceBpm = 0f, sliderValue = 0f, range = PitchRange.Wide))
    }

    @Test
    fun `a known tempo scales with the slider`() {
        assertEquals(140f, playingBpm(140f, 0f, PitchRange.Wide)!!, 0.05f)
        assertEquals(151.2f, playingBpm(140f, 1f, PitchRange.Narrow)!!, 0.05f)
        assertEquals(70f, playingBpm(140f, -1f, PitchRange.Wide)!!, 0.05f)
    }

    @Test
    fun `the readout is rounded to one decimal, not left at full float precision`() {
        // 128 * (1 + 0.05 * 0.08) = 128.512, which must present as 128.5 rather than 128.512.
        assertEquals(128.5f, playingBpm(128f, 0.05f, PitchRange.Narrow)!!, 0.001f)
    }
}
