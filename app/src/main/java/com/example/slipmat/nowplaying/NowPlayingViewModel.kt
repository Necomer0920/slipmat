package com.example.slipmat.nowplaying

import androidx.lifecycle.ViewModel
import com.example.slipmat.core.data.settings.PlaybackSettings
import com.example.slipmat.core.media.PitchRange
import com.example.slipmat.core.media.PlaybackController
import com.example.slipmat.core.media.speedPitchFor
import com.example.slipmat.core.media.waveform.WaveformSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
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
    private val waveforms: WaveformSource,
) : ViewModel() {

    private val _waveform = MutableStateFlow<FloatArray?>(null)

    /** Peaks for the current track, or null while decoding or when the file cannot be decoded. */
    val waveform: StateFlow<FloatArray?> = _waveform.asStateFlow()

    private var waveformJob: Job? = null

    init {
        // One decode per track. Cancelling the previous job matters: skipping through a queue
        // would otherwise leave several multi-second decodes racing to write the same field.
        viewModelScope.launch {
            playback.state
                .map { it.mediaId to it.queue.getOrNull(it.queueIndex)?.uri }
                .distinctUntilChanged()
                .collect { (_, uri) ->
                    waveformJob?.cancel()
                    _waveform.value = null
                    if (uri == null) return@collect
                    waveformJob = viewModelScope.launch {
                        _waveform.value = waveforms.peaksFor(uri)
                    }
                }
        }
    }

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

    /** Seek by fraction, which is what a waveform drag produces. */
    fun seekToFraction(fraction: Float) {
        val duration = state.value.durationMs
        if (duration > 0L) playback.seekTo((fraction * duration).toLong())
    }

    fun skipForward() = playback.skipForward()

    fun skipBack() = playback.skipBack()

    fun toggleShuffle() = playback.setShuffle(!state.value.shuffleEnabled)

    /** One button, three states: off → all → one → off. */
    fun cycleRepeatMode() = playback.setRepeatMode(state.value.repeatMode.next())

    fun moveQueueItem(fromIndex: Int, toIndex: Int) = playback.moveQueueItem(fromIndex, toIndex)

    fun skipToQueueIndex(index: Int) = playback.skipToQueueIndex(index)

    fun startSleepTimer(minutes: Int) = playback.startSleepTimer(minutes * 60_000L)

    fun cancelSleepTimer() = playback.cancelSleepTimer()

    /**
     * Updates the slider immediately, but rate-limits what reaches the player.
     *
     * A drag emits a new value every frame. Pushing each one through
     * `setPlaybackParameters` reconfigures the audio pipeline dozens of times a second, and each
     * reconfiguration can splice the stream audibly. The UI stays at frame rate; the player hears
     * at most one change per [APPLY_INTERVAL_MS], plus a final exact value on release.
     */
    fun onSliderChange(value: Float) {
        _sliderValue.value = value
        val now = System.currentTimeMillis()
        if (now - lastAppliedAtMs >= APPLY_INTERVAL_MS) {
            lastAppliedAtMs = now
            applySpeedPitch(value, keyLock.value, pitchRange.value)
        }
    }

    /** Called when the finger lifts, so the player ends up on exactly the value shown. */
    fun onSliderChangeFinished() {
        lastAppliedAtMs = System.currentTimeMillis()
        applySpeedPitch(_sliderValue.value, keyLock.value, pitchRange.value)
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

    private var lastAppliedAtMs = 0L

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L

        /** Fast enough to feel continuous, slow enough not to thrash the audio pipeline. */
        const val APPLY_INTERVAL_MS = 100L
    }
}
