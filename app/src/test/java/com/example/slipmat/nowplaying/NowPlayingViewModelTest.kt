package com.example.slipmat.nowplaying

import com.example.slipmat.core.media.PlayerState
import com.example.slipmat.core.media.RepeatMode
import org.junit.Assert.assertEquals
import com.example.slipmat.MainDispatcherRule
import org.junit.Rule
import org.junit.Test

class NowPlayingViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val playback = FakePlaybackController()
    private val settings = FakePlaybackSettings()
    private val viewModel = NowPlayingViewModel(playback, settings, FakeWaveformSource())

    @Test
    fun `a rebuilt screen shows the fader where the player left it`() {
        // Now-playing is a nav destination: walking back to the library destroys this ViewModel
        // while the player keeps its parameters. A fader position owned here would return to
        // centre over audio still running at 0.92x, and the readout would claim +0.0%.
        viewModel.onSliderChange(-1f)

        val rebuilt = NowPlayingViewModel(playback, settings, FakeWaveformSource())

        assertEquals(-1f, rebuilt.sliderValue.value, 0.0001f)
    }

    @Test
    fun `applying tempo reads the live fader position rather than a copy`() {
        viewModel.onSliderChange(-1f)
        playback.calls.clear()

        viewModel.onSliderChangeFinished()

        // Full slow travel on the default ±8% range, key lock on: tempo drops, key holds.
        assertEquals(listOf("speed=0.92,pitch=1.0"), playback.calls)
    }

    @Test
    fun `the play button pauses when something is playing`() {
        playback.setState(PlayerState(isPlaying = true, mediaId = "1"))

        viewModel.togglePlayPause()

        assertEquals(listOf("pause"), playback.calls)
    }

    @Test
    fun `the play button plays when paused`() {
        playback.setState(PlayerState(isPlaying = false, mediaId = "1"))

        viewModel.togglePlayPause()

        assertEquals(listOf("play"), playback.calls)
    }

    @Test
    fun `repeat advances from whatever the player currently reports`() {
        // The next mode is derived from live state, not from a counter the view model keeps —
        // otherwise a change made from the lock screen would desynchronise the button.
        playback.setState(PlayerState(repeatMode = RepeatMode.All))

        viewModel.cycleRepeatMode()

        assertEquals(listOf("setRepeatMode(One)"), playback.calls)
    }

    @Test
    fun `repeat wraps from one back to off`() {
        playback.setState(PlayerState(repeatMode = RepeatMode.One))

        viewModel.cycleRepeatMode()

        assertEquals(listOf("setRepeatMode(Off)"), playback.calls)
    }

    @Test
    fun `shuffle inverts whatever the player reports`() {
        playback.setState(PlayerState(shuffleEnabled = true))

        viewModel.toggleShuffle()

        assertEquals(listOf("setShuffle(false)"), playback.calls)
    }

    @Test
    fun `the sleep timer is set in minutes but sent in milliseconds`() {
        viewModel.startSleepTimer(minutes = 30)

        assertEquals(listOf("startSleepTimer(1800000)"), playback.calls)
    }

    @Test
    fun `transport commands are forwarded untouched`() {
        viewModel.next()
        viewModel.previous()
        viewModel.skipForward()
        viewModel.skipBack()
        viewModel.seekTo(1_234)

        assertEquals(
            listOf("next", "previous", "skipForward", "skipBack", "seekTo(1234)"),
            playback.calls,
        )
    }
}
