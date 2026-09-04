package com.example.slipmat.nowplaying

import com.example.slipmat.core.media.PlaybackController
import com.example.slipmat.core.media.PlayerState
import com.example.slipmat.core.media.QueueItem
import com.example.slipmat.core.media.RepeatMode
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
}
