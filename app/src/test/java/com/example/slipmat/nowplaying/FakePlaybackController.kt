package com.example.slipmat.nowplaying

import com.example.slipmat.core.media.PlaybackController
import com.example.slipmat.core.media.PlayerState
import com.example.slipmat.core.media.QueueItem
import com.example.slipmat.core.media.RepeatMode
import com.example.slipmat.core.media.PitchRange
import com.example.slipmat.core.media.speedPitchFor
import com.example.slipmat.core.media.dsp.DelayState
import com.example.slipmat.core.media.dsp.EqState
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

    private val _eqState = MutableStateFlow(EqState())
    override val eqState: StateFlow<EqState> = _eqState

    private val _delayState = MutableStateFlow(DelayState())
    override val delayState: StateFlow<DelayState> = _delayState

    private val _tempoSlider = MutableStateFlow(0f)
    override val tempoSlider: StateFlow<Float> = _tempoSlider

    private val _keyLock = MutableStateFlow(true)
    override val keyLock: StateFlow<Boolean> = _keyLock

    val calls = mutableListOf<String>()

    fun setState(state: PlayerState) { _state.value = state }

    override fun connect() { calls += "connect" }
    override fun release() { calls += "release" }
    override fun playQueue(items: List<QueueItem>, startIndex: Int) {
        calls += "playQueue(${items.size},$startIndex)"
        _tempoSlider.value = 0f
        _keyLock.value = true
    }
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
    override fun setKeyLock(enabled: Boolean) { _keyLock.value = enabled }
    override fun applyTempo(range: PitchRange) {
        val speedPitch = speedPitchFor(_tempoSlider.value, range, _keyLock.value)
        calls += "speed=${speedPitch.speed},pitch=${speedPitch.pitch}"
    }
    override fun setFilterEnabled(enabled: Boolean) { calls += "filterEnabled($enabled)"; _filterState.value = _filterState.value.copy(enabled = enabled) }
    override fun setFilterCutoff(hz: Float) { calls += "cutoff($hz)"; _filterState.value = _filterState.value.copy(cutoffHz = hz) }
    override fun setFilterMode(mode: FilterMode) { calls += "filterMode($mode)"; _filterState.value = _filterState.value.copy(mode = mode) }
    override fun setDelayEnabled(enabled: Boolean) { calls += "delayEnabled($enabled)"; _delayState.value = _delayState.value.copy(enabled = enabled) }
    override fun setDelayTime(ms: Float) { calls += "delayTime($ms)"; _delayState.value = _delayState.value.copy(timeMs = ms) }
    override fun setDelayFeedback(value: Float) { calls += "delayFeedback($value)"; _delayState.value = _delayState.value.copy(feedback = value) }
    override fun setDelayMix(value: Float) { calls += "delayMix($value)"; _delayState.value = _delayState.value.copy(mix = value) }
    override fun setEqGains(gainsDb: List<Float>) {
        calls += "eqGains(${gainsDb.joinToString(",")})"
        _eqState.value = _eqState.value.copy(gainsDb = gainsDb)
    }
    override fun setEqEnabled(enabled: Boolean) { calls += "eqEnabled($enabled)"; _eqState.value = _eqState.value.copy(enabled = enabled) }
    override fun setEqGain(band: Int, gainDb: Float) {
        calls += "eqGain($band,$gainDb)"
        _eqState.value = _eqState.value.copy(
            gainsDb = _eqState.value.gainsDb.mapIndexed { index, existing -> if (index == band) gainDb else existing },
        )
    }
}

/** In-memory settings, so the view model can be tested without a Context or DataStore. */
class FakePlaybackSettings(
    rangeInitial: String? = null,
) : com.example.slipmat.core.data.settings.PlaybackSettings {

    private val _range = MutableStateFlow(rangeInitial)

    override val pitchRangeName: kotlinx.coroutines.flow.Flow<String?> = _range

    override suspend fun setPitchRangeName(name: String) { _range.value = name }
}

/** In-memory EQ presets, so preset behaviour can be tested without a database. */
class FakeEqPresetStore : com.example.slipmat.core.data.eq.EqPresetStore {

    private val stored = MutableStateFlow<List<com.example.slipmat.core.data.eq.EqPreset>>(emptyList())

    /**
     * What is stored, without suspending.
     *
     * So tests can assert on it without `runTest`, whose scheduler is not the one
     * `MainDispatcherRule` gives `viewModelScope` — launches queue on the rule's scheduler and
     * never run, and every assertion then describes a view model that did nothing.
     */
    val saved: List<com.example.slipmat.core.data.eq.EqPreset> get() = stored.value

    fun put(name: String, gainsDb: List<Float>) {
        stored.value = stored.value.filterNot { it.name == name } +
            com.example.slipmat.core.data.eq.EqPreset(name, gainsDb)
    }

    override val presets: kotlinx.coroutines.flow.Flow<List<com.example.slipmat.core.data.eq.EqPreset>> = stored

    override suspend fun save(name: String, gainsDb: List<Float>) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        stored.value = stored.value.filterNot { it.name == trimmed } +
            com.example.slipmat.core.data.eq.EqPreset(trimmed, gainsDb)
    }

    override suspend fun load(name: String) = stored.value.firstOrNull { it.name == name }

    override suspend fun delete(name: String) {
        stored.value = stored.value.filterNot { it.name == name }
    }
}

/** Returns a fixed waveform without decoding anything. */
class FakeWaveformSource(
    private val peaks: FloatArray? = null,
) : com.example.slipmat.core.media.waveform.WaveformSource {
    override suspend fun peaksFor(mediaUri: String): FloatArray? = peaks
}
