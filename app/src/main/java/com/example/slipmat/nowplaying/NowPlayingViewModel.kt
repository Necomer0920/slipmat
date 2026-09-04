package com.example.slipmat.nowplaying

import androidx.lifecycle.ViewModel
import com.example.slipmat.core.data.settings.PlaybackSettings
import com.example.slipmat.core.media.PitchRange
import com.example.slipmat.core.media.PlaybackController
import com.example.slipmat.core.media.speedPitchFor
import com.example.slipmat.core.media.PlayerState
import com.example.slipmat.core.media.RepeatMode
import com.example.slipmat.core.media.SleepTimerState
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Mirrors playback state for the UI and forwards commands to the service.
 *
 * Holds no state of its own: [state] is the controller's flow, unchanged. Anything cached here
 * would be a second source of truth that a process death could silently desynchronise.
 */
@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val playback: PlaybackController,
    private val settings: PlaybackSettings,
) : ViewModel() {

    /**
     * The slider position, owned here rather than read back from the player.
     *
     * A speed multiplier does not uniquely determine a slider position — 1.08x is full travel at
     * ±8% and a fifth of it at ±50% — so deriving the slider from the player would make it jump
     * whenever the range changed.
     */
    private val _sliderValue = MutableStateFlow(0f)
    val sliderValue: StateFlow<Float> = _sliderValue.asStateFlow()

    val keyLock: StateFlow<Boolean> = settings.keyLock
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), true)

    val pitchRange: StateFlow<PitchRange> = settings.pitchRangeName
        .map { name -> PitchRange.entries.firstOrNull { it.name == name } ?: PitchRange.Narrow }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PitchRange.Narrow)

    val state: StateFlow<PlayerState> = playback.state

    val sleepTimer: StateFlow<SleepTimerState> = playback.sleepTimerState

    fun togglePlayPause() {
        if (state.value.isPlaying) playback.pause() else playback.play()
    }

    fun next() = playback.next()

    fun previous() = playback.previous()

    fun seekTo(positionMs: Long) = playback.seekTo(positionMs)

    fun skipForward() = playback.skipForward()

    fun skipBack() = playback.skipBack()

    fun toggleShuffle() = playback.setShuffle(!state.value.shuffleEnabled)

    /** One button, three states: off → all → one → off. */
    fun cycleRepeatMode() = playback.setRepeatMode(state.value.repeatMode.next())

    fun moveQueueItem(fromIndex: Int, toIndex: Int) = playback.moveQueueItem(fromIndex, toIndex)

    fun skipToQueueIndex(index: Int) = playback.skipToQueueIndex(index)

    fun startSleepTimer(minutes: Int) = playback.startSleepTimer(minutes * 60_000L)

    fun cancelSleepTimer() = playback.cancelSleepTimer()

    fun onSliderChange(value: Float) {
        _sliderValue.value = value
        applySpeedPitch(value, keyLock.value, pitchRange.value)
    }

    fun onKeyLockChange(enabled: Boolean) {
        // Applied immediately so the switch takes effect mid-track, then persisted.
        applySpeedPitch(_sliderValue.value, enabled, pitchRange.value)
        viewModelScope.launch { settings.setKeyLock(enabled) }
    }

    fun onRangeChange(range: PitchRange) {
        // The slider stays where it is, so the same position now means a different percentage.
        applySpeedPitch(_sliderValue.value, keyLock.value, range)
        viewModelScope.launch { settings.setPitchRangeName(range.name) }
    }

    private fun applySpeedPitch(slider: Float, keyLock: Boolean, range: PitchRange) {
        playback.setSpeedPitch(speedPitchFor(slider, range, keyLock))
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
