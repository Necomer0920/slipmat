package com.example.slipmat.core.media

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** How long the volume ramp lasts before the timer stops playback. */
const val SLEEP_FADE_MS = 10_000L

/** Ticks often enough for a seconds countdown to look alive, rarely enough to cost nothing. */
private const val TICK_MS = 500L

sealed interface SleepTimerState {

    data object Idle : SleepTimerState

    data class Running(val remainingMs: Long) : SleepTimerState {
        /** True once the volume ramp has begun, so the UI can say why the music is receding. */
        val isFading: Boolean get() = remainingMs <= SLEEP_FADE_MS

        /** 1f at full volume, 0f at silence. Outside the fade window this is always 1f. */
        val fadeFraction: Float
            get() = if (remainingMs >= SLEEP_FADE_MS) 1f else (remainingMs.toFloat() / SLEEP_FADE_MS).coerceIn(0f, 1f)
    }
}

/**
 * Counts down to the end of playback, independently of any UI.
 *
 * A singleton rather than something the now-playing screen owns: the whole point of a sleep timer
 * is that it keeps running with the screen off and the app backgrounded. It only tracks time — the
 * service watches [state] and does the fading and pausing, because the player belongs to it.
 */
@Singleton
class SleepTimer @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow<SleepTimerState>(SleepTimerState.Idle)
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    private var countdown: Job? = null

    /** Restarting replaces any timer already running rather than stacking a second one. */
    fun start(durationMs: Long) {
        countdown?.cancel()
        if (durationMs <= 0L) {
            _state.value = SleepTimerState.Idle
            return
        }
        countdown = scope.launch {
            // Wall-clock deadline, not an accumulated tick count: a delay that runs long under
            // load would otherwise make the timer drift past its duration.
            val deadline = System.currentTimeMillis() + durationMs
            while (isActive) {
                val remaining = deadline - System.currentTimeMillis()
                if (remaining <= 0L) break
                _state.value = SleepTimerState.Running(remaining)
                delay(TICK_MS)
            }
            _state.value = SleepTimerState.Running(0L)
        }
    }

    fun cancel() {
        countdown?.cancel()
        countdown = null
        _state.value = SleepTimerState.Idle
    }
}
