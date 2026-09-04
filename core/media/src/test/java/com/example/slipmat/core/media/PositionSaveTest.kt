package com.example.slipmat.core.media

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Resume positions are worth getting right: a wrong one either restarts a track the user was
 * halfway through, or drops them back at its final second forever.
 */
class PositionSaveTest {

    @Test
    fun `a position well into the track is remembered`() {
        assertEquals(
            PositionSaveAction.Save(60_000),
            positionSaveAction(positionMs = 60_000, durationMs = 200_000),
        )
    }

    @Test
    fun `the first few seconds are not worth resuming`() {
        assertEquals(PositionSaveAction.Ignore, positionSaveAction(positionMs = 2_000, durationMs = 200_000))
    }

    @Test
    fun `a position near the end clears rather than saves`() {
        // Otherwise the track resumes at its last second every single time.
        assertEquals(PositionSaveAction.Clear, positionSaveAction(positionMs = 198_000, durationMs = 200_000))
    }

    @Test
    fun `a position exactly at the end clears`() {
        assertEquals(PositionSaveAction.Clear, positionSaveAction(positionMs = 200_000, durationMs = 200_000))
    }

    @Test
    fun `an unknown duration still saves, since the end cannot be judged`() {
        assertEquals(
            PositionSaveAction.Save(60_000),
            positionSaveAction(positionMs = 60_000, durationMs = 0),
        )
    }

    @Test
    fun `a negative position is ignored outright`() {
        assertEquals(PositionSaveAction.Ignore, positionSaveAction(positionMs = -1, durationMs = 200_000))
    }

    @Test
    fun `a very short track is never worth resuming`() {
        // A four-second clip is inside both the minimum and the finished threshold.
        assertEquals(PositionSaveAction.Clear, positionSaveAction(positionMs = 3_000, durationMs = 4_000))
    }
}
