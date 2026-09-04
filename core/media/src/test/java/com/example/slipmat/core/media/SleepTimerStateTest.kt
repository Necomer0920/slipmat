package com.example.slipmat.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The fade curve is what the service multiplies the player volume by, so an error here is either
 * an abrupt cut or a track that never actually goes quiet.
 */
class SleepTimerStateTest {

    @Test
    fun `well before the end the volume is untouched`() {
        val state = SleepTimerState.Running(remainingMs = 60_000)

        assertEquals(1f, state.fadeFraction, 0f)
        assertFalse(state.isFading)
    }

    @Test
    fun `the fade begins exactly at the fade window`() {
        val state = SleepTimerState.Running(remainingMs = SLEEP_FADE_MS)

        assertEquals(1f, state.fadeFraction, 0f)
        assertTrue(state.isFading)
    }

    @Test
    fun `halfway through the fade the volume is halved`() {
        val state = SleepTimerState.Running(remainingMs = SLEEP_FADE_MS / 2)

        assertEquals(0.5f, state.fadeFraction, 0.001f)
    }

    @Test
    fun `at zero the volume is silent`() {
        assertEquals(0f, SleepTimerState.Running(remainingMs = 0).fadeFraction, 0f)
    }

    @Test
    fun `an overrun past zero never produces a negative volume`() {
        // Player.volume rejects negatives, so clamping matters even if the timer should not get here.
        assertEquals(0f, SleepTimerState.Running(remainingMs = -5_000).fadeFraction, 0f)
    }

    @Test
    fun `a timer shorter than the fade window starts already fading`() {
        val state = SleepTimerState.Running(remainingMs = 3_000)

        assertTrue(state.isFading)
        assertEquals(0.3f, state.fadeFraction, 0.001f)
    }
}
