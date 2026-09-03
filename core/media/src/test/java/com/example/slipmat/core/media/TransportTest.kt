package com.example.slipmat.core.media

import org.junit.Assert.assertEquals
import org.junit.Test

/** Skip-seek must never leave the track, whichever end it is near. */
class SkipSeekTest {

    @Test
    fun `a skip forward mid-track simply advances`() {
        assertEquals(40_000L, skipSeekTarget(currentMs = 30_000, durationMs = 200_000, deltaMs = SKIP_SEEK_MS))
    }

    @Test
    fun `a skip back mid-track simply rewinds`() {
        assertEquals(20_000L, skipSeekTarget(currentMs = 30_000, durationMs = 200_000, deltaMs = -SKIP_SEEK_MS))
    }

    @Test
    fun `skipping forward near the end stops at the end, it does not overshoot`() {
        // Overshooting ends the track, which is not what "forward 15 seconds" should do.
        assertEquals(200_000L, skipSeekTarget(currentMs = 195_000, durationMs = 200_000, deltaMs = SKIP_SEEK_MS))
    }

    @Test
    fun `skipping back near the start stops at zero, it does not go negative`() {
        assertEquals(0L, skipSeekTarget(currentMs = 5_000, durationMs = 200_000, deltaMs = -SKIP_SEEK_MS))
    }

    @Test
    fun `with an unknown duration only the lower bound can be enforced`() {
        // Media3 reports a non-positive duration until a track is prepared.
        assertEquals(40_000L, skipSeekTarget(currentMs = 30_000, durationMs = 0, deltaMs = SKIP_SEEK_MS))
        assertEquals(0L, skipSeekTarget(currentMs = 5_000, durationMs = -1, deltaMs = -SKIP_SEEK_MS))
    }

    @Test
    fun `a skip from exactly zero on a very short track lands on its end`() {
        assertEquals(4_000L, skipSeekTarget(currentMs = 0, durationMs = 4_000, deltaMs = SKIP_SEEK_MS))
    }
}

class RepeatModeTest {

    @Test
    fun `the button cycles off then all then one and back`() {
        assertEquals(RepeatMode.All, RepeatMode.Off.next())
        assertEquals(RepeatMode.One, RepeatMode.All.next())
        assertEquals(RepeatMode.Off, RepeatMode.One.next())
    }

    @Test
    fun `cycling three times returns to where it started`() {
        assertEquals(RepeatMode.Off, RepeatMode.Off.next().next().next())
    }

    @Test
    fun `player state defaults to no shuffle and no repeat`() {
        assertEquals(false, PlayerState.EMPTY.shuffleEnabled)
        assertEquals(RepeatMode.Off, PlayerState.EMPTY.repeatMode)
    }

    @Test
    fun `shuffle and repeat survive the state mapping`() {
        val state = playerStateOf(
            isPlaying = true,
            rawPositionMs = 0,
            rawDurationMs = 1_000,
            shuffleEnabled = true,
            repeatMode = RepeatMode.One,
        )

        assertEquals(true, state.shuffleEnabled)
        assertEquals(RepeatMode.One, state.repeatMode)
    }
}
