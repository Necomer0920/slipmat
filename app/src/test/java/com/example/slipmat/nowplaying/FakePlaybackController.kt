package com.example.slipmat.nowplaying

import com.example.slipmat.core.media.PlaybackController
import com.example.slipmat.core.media.PlayerState
import com.example.slipmat.core.media.QueueItem
import com.example.slipmat.core.media.RepeatMode
import com.example.slipmat.core.media.PitchRange
import com.example.slipmat.core.media.speedPitchFor
import com.example.slipmat.core.media.dsp.FilterMode
import com.example.slipmat.core.media.dsp.FilterState
import com.example.slipmat.core.media.SleepTimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Records what the view model asked the player to do.
 *
 * The point of these tests is the decisions the view model makes — play versus pause, which repeat
 * mode comes next — not that Media3 honours them.
 */
class FakePlaybackController : PlaybackController {

    private val _state = MutableStateFlow(PlayerState.EMPTY)
    override val state: StateFlow<PlayerState> = _state

    private val _sleepTimerState = MutableStateFlow<SleepTimerState>(SleepTimerState.Idle)
    override val sleepTimerState: StateFlow<SleepTimerState> = _sleepTimerState

    private val _filterState = MutableStateFlow(FilterState())
    override val filterState: StateFlow<FilterState> = _filterState

    private val _tempoSlider = MutableStateFlow(0f)
    override val tempoSlider: StateFlow<Float> = _tempoSlider

    val calls = mutableListOf<String>()

    fun setState(state: PlayerState) { _state.value = state }

    override fun connect() { calls += "connect" }
    override fun release() { calls += "release" }
    override fun playQueue(items: List<QueueItem>, startIndex: Int) { calls += "playQueue(${items.size},$startIndex)" }
    override fun play() { calls += "play" }
    override fun pause() { calls += "pause" }
    override fun next() { calls += "next" }
    override fun previous() { calls += "previous" }
    override fun seekTo(positionMs: Long) { calls += "seekTo($positionMs)" }
    override fun skipForward() { calls += "skipForward" }
    override fun skipBack() { calls += "skipBack" }
    override fun setShuffle(enabled: Boolean) { calls += "setShuffle($enabled)" }
    override fun setRepeatMode(mode: RepeatMode) { calls += "setRepeatMode($mode)" }
    override fun moveQueueItem(fromIndex: Int, toIndex: Int) { calls += "move($fromIndex,$toIndex)" }
    override fun skipToQueueIndex(index: Int) { calls += "skipTo($index)" }
    override fun startSleepTimer(durationMs: Long) { calls += "startSleepTimer($durationMs)" }
    override fun cancelSleepTimer() { calls += "cancelSleepTimer" }
    override fun moveTempoFader(sliderValue: Float) { _tempoSlider.value = sliderValue.coerceIn(-1f, 1f) }
    override fun applyTempo(range: PitchRange, keyLock: Boolean) {
        val speedPitch = speedPitchFor(_tempoSlider.value, range, keyLock)
        calls += "speed=${speedPitch.speed},pitch=${speedPitch.pitch}"
    }
    override fun setFilterEnabled(enabled: Boolean) { calls += "filterEnabled($enabled)"; _filterState.value = _filterState.value.copy(enabled = enabled) }
    override fun setFilterCutoff(hz: Float) { calls += "cutoff($hz)"; _filterState.value = _filterState.value.copy(cutoffHz = hz) }
    override fun setFilterMode(mode: FilterMode) { calls += "filterMode($mode)"; _filterState.value = _filterState.value.copy(mode = mode) }
}

/** In-memory settings, so the view model can be tested without a Context or DataStore. */
class FakePlaybackSettings(
    keyLockInitial: Boolean = true,
    rangeInitial: String? = null,
) : com.example.slipmat.core.data.settings.PlaybackSettings {

    private val _keyLock = MutableStateFlow(keyLockInitial)
    private val _range = MutableStateFlow(rangeInitial)

    override val keyLock: kotlinx.coroutines.flow.Flow<Boolean> = _keyLock
    override val pitchRangeName: kotlinx.coroutines.flow.Flow<String?> = _range

    override suspend fun setKeyLock(enabled: Boolean) { _keyLock.value = enabled }
    override suspend fun setPitchRangeName(name: String) { _range.value = name }
}

/** Returns a fixed waveform without decoding anything. */
class FakeWaveformSource(
    private val peaks: FloatArray? = null,
) : com.example.slipmat.core.media.waveform.WaveformSource {
    override suspend fun peaksFor(mediaUri: String): FloatArray? = peaks
}
