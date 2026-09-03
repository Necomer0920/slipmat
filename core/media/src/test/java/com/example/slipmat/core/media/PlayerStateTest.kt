package com.example.slipmat.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The mapping exists to sanitise what Media3 actually reports. Each case below is a value the
 * player really does emit, and which would produce a visibly broken UI if passed straight through.
 */
class PlayerStateTest {

    @Test
    fun `unset duration becomes zero rather than a negative`() {
        // Media3 reports C.TIME_UNSET (a large negative) until a track is prepared.
        val state = playerStateOf(isPlaying = false, rawPositionMs = 0, rawDurationMs = Long.MIN_VALUE + 1)

        assertEquals(0L, state.durationMs)
    }

    @Test
    fun `negative position is clamped to zero`() {
        val state = playerStateOf(isPlaying = true, rawPositionMs = -1_000, rawDurationMs = 60_000)

        assertEquals(0L, state.positionMs)
    }

    @Test
    fun `position beyond duration is clamped to duration`() {
        val state = playerStateOf(isPlaying = true, rawPositionMs = 61_000, rawDurationMs = 60_000)

        assertEquals(60_000L, state.positionMs)
    }

    @Test
    fun `progress is zero while duration is unknown`() {
        val state = playerStateOf(isPlaying = true, rawPositionMs = 5_000, rawDurationMs = -1)

        assertEquals(0f, state.progress, 0f)
    }

    @Test
    fun `progress is the played fraction`() {
        val state = playerStateOf(isPlaying = true, rawPositionMs = 30_000, rawDurationMs = 120_000)

        assertEquals(0.25f, state.progress, 0.0001f)
    }

    @Test
    fun `blank metadata is treated as absent`() {
        val state = playerStateOf(
            isPlaying = false,
            rawPositionMs = 0,
            rawDurationMs = 0,
            title = "   ",
            artist = "",
        )

        assertNull(state.title)
        assertNull(state.artist)
    }

    @Test
    fun `hasMedia reflects whether a track is loaded`() {
        assertFalse(PlayerState.EMPTY.hasMedia)

        val loaded = playerStateOf(isPlaying = false, rawPositionMs = 0, rawDurationMs = 0, mediaId = "42")
        assertTrue(loaded.hasMedia)
    }
}
