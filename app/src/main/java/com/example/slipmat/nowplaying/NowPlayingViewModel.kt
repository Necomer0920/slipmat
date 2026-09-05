package com.example.slipmat.nowplaying

import androidx.lifecycle.ViewModel
import com.example.slipmat.core.data.settings.PlaybackSettings
import com.example.slipmat.core.media.PitchRange
import com.example.slipmat.core.media.PlaybackController
import com.example.slipmat.core.media.dsp.DelayState
import com.example.slipmat.core.media.dsp.FilterMode
import com.example.slipmat.core.media.dsp.FilterState
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
     * The slider position, kept by the controller rather than here.
     *
     * It cannot be derived from the player's speed — 1.08x is full travel at ±8% and a fifth of it
     * at ±50%, so the slider would jump whenever the range changed. But it cannot live in this
     * ViewModel either: the player keeps its parameters across tracks and screens, and a position
     * that dies with the screen leaves the readout claiming +0.0% over pitched audio.
     */
    val sliderValue: StateFlow<Float> = playback.tempoSlider

    /** Performance state, so it sits with the fader in the controller rather than in DataStore. */
    val keyLock: StateFlow<Boolean> = playback.keyLock

    val pitchRange: StateFlow<PitchRange> = settings.pitchRangeName
        .map { name -> PitchRange.entries.firstOrNull { it.name == name } ?: PitchRange.Narrow }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PitchRange.Narrow)

    val state: StateFlow<PlayerState> = playback.state

    val sleepTimer: StateFlow<SleepTimerState> = playback.sleepTimerState

    val filter: StateFlow<FilterState> = playback.filterState

    val delay: StateFlow<DelayState> = playback.delayState

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

    fun onDelayEnabledChange(enabled: Boolean) = playback.setDelayEnabled(enabled)

    /**
     * Not rate-limited, unlike the filter cutoff.
     *
     * Each of these is one volatile write that the audio thread reads once per buffer. The
     * cutoff is throttled because every change redesigns the filter; there is nothing to
     * redesign here, so throttling would only add lag to a control meant to be swept.
     */
    fun onDelayTimeChange(ms: Float) = playback.setDelayTime(ms)

    fun onDelayFeedbackChange(value: Float) = playback.setDelayFeedback(value)

    fun onDelayMixChange(value: Float) = playback.setDelayMix(value)

    fun onFilterEnabledChange(enabled: Boolean) = playback.setFilterEnabled(enabled)

    fun onFilterModeChange(mode: FilterMode) = playback.setFilterMode(mode)

    /**
     * Rate-limited for the same reason the tempo slider is: a drag emits a value per frame, and
     * each one redesigns the filter. The audio thread reads the coefficients once per buffer, so
     * more than a few updates per buffer is wasted work at best.
     */
    fun onFilterCutoffChange(hz: Float) {
        val now = System.currentTimeMillis()
        if (now - lastCutoffAtMs >= APPLY_INTERVAL_MS) {
            lastCutoffAtMs = now
            playback.setFilterCutoff(hz)
        }
    }

    /**
     * Updates the slider immediately, but rate-limits what reaches the player.
     *
     * A drag emits a new value every frame. Pushing each one through
     * `setPlaybackParameters` reconfigures the audio pipeline dozens of times a second, and each
     * reconfiguration can splice the stream audibly. The UI stays at frame rate; the player hears
     * at most one change per [APPLY_INTERVAL_MS], plus a final exact value on release.
     */
    fun onSliderChange(value: Float) {
        playback.moveTempoFader(value)
        val now = System.currentTimeMillis()
        if (now - lastAppliedAtMs >= APPLY_INTERVAL_MS) {
            lastAppliedAtMs = now
            playback.applyTempo(pitchRange.value)
        }
    }

    /** Called when the finger lifts, so the player ends up on exactly the value shown. */
    fun onSliderChangeFinished() {
        lastAppliedAtMs = System.currentTimeMillis()
        playback.applyTempo(pitchRange.value)
    }

    fun onKeyLockChange(enabled: Boolean) {
        playback.setKeyLock(enabled)
        // Applied at once, so the switch takes effect on the track already playing.
        playback.applyTempo(pitchRange.value)
    }

    fun onRangeChange(range: PitchRange) {
        // The slider stays where it is, so the same position now means a different percentage.
        playback.applyTempo(range)
        viewModelScope.launch { settings.setPitchRangeName(range.name) }
    }

    private var lastAppliedAtMs = 0L
    private var lastCutoffAtMs = 0L

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L

        /** Fast enough to feel continuous, slow enough not to thrash the audio pipeline. */
        const val APPLY_INTERVAL_MS = 100L
    }
}
