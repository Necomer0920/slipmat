package com.example.slipmat.nowplaying

import com.example.slipmat.core.media.PlayerState
import com.example.slipmat.core.media.dsp.EQ_BANDS
import org.junit.Assert.assertTrue
import com.example.slipmat.core.media.RepeatMode
import org.junit.Assert.assertEquals
import com.example.slipmat.MainDispatcherRule
import org.junit.Rule
import org.junit.Test

class NowPlayingViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val playback = FakePlaybackController()
    private val presets = FakeEqPresetStore()
    /**
     * Built lazily, so construction happens inside the test method.
     *
     * JUnit instantiates the test class *before* it applies rules, so a view model created as a
     * field captures `Dispatchers.Main` before [MainDispatcherRule] has installed one — and
     * everything it launches from then on is queued against a dispatcher that never runs. The
     * tests still pass, because a view model that does nothing breaks nothing that only reads
     * state back from a fake.
     */
    private val viewModel by lazy {
        NowPlayingViewModel(playback, FakeWaveformSource(), presets)
    }

    @Test
    fun `saving a preset captures the curve as it stands`() {
        viewModel.onEqGainChange(2, 6f)

        viewModel.onSaveEqPreset("  Club  ")

        // Trimmed: a name with stray spaces is a different preset to the map than to the eye.
        assertEquals("Club", presets.saved.single().name)
        assertEquals(6f, presets.saved.single().gainsDb[2], 0.001f)
    }

    @Test
    fun `loading a preset sets every band in one go`() {
        presets.put("Club", List(EQ_BANDS.size) { 3f })
        playback.calls.clear()

        viewModel.onLoadEqPreset("Club")

        // Two calls: EQ was off, so loading a preset enables it too (R3.13/§5.1) - then one gains
        // call, not eight, since band-at-a-time would animate the curve through seven settings
        // nobody asked for on the way to the one they did.
        assertEquals(listOf("eqEnabled(true)"), playback.calls.filter { it.startsWith("eqEnabled") })
        assertEquals(1, playback.calls.count { it.startsWith("eqGains(") })
    }

    @Test
    fun `loading a preset while EQ is already on does not re-enable it`() {
        viewModel.onEqEnabledChange(true)
        presets.put("Club", List(EQ_BANDS.size) { 3f })
        playback.calls.clear()

        viewModel.onLoadEqPreset("Club")

        assertEquals(emptyList<String>(), playback.calls.filter { it.startsWith("eqEnabled") })
    }

    @Test
    fun `loading a built-in preset applies its gains in one call and enables EQ`() {
        viewModel.onLoadEqPreset("Bass Boost")

        assertEquals(listOf("eqEnabled(true)"), playback.calls.filter { it.startsWith("eqEnabled") })
        assertEquals(1, playback.calls.count { it.startsWith("eqGains(") })
    }

    @Test
    fun `deleting a saved preset round-trips through the store`() {
        presets.put("Club", List(EQ_BANDS.size) { 3f })

        viewModel.onDeleteEqPreset("Club")

        assertEquals(emptyList<Any>(), presets.saved)
    }

    @Test
    fun `a blank name saves nothing`() {
        viewModel.onSaveEqPreset("   ")

        // A nameless preset cannot be loaded or deleted from a list that shows names.
        assertEquals(emptyList<Any>(), presets.saved)
    }

    @Test
    fun `loading a name that is gone does not touch the player`() {
        presets.put("Club", List(EQ_BANDS.size) { 3f })
        viewModel.onDeleteEqPreset("Club")
        playback.calls.clear()

        viewModel.onLoadEqPreset("Club")

        assertEquals(emptyList<Any>(), presets.saved)
        assertEquals(emptyList<String>(), playback.calls)
    }

    @Test
    fun `a rebuilt screen shows the fader where the player left it`() {
        // Now-playing is a nav destination: walking back to the library destroys this ViewModel
        // while the player keeps its parameters. A fader position owned here would return to
        // centre over audio still running at 0.92x, and the readout would claim +0.0%.
        viewModel.onSliderChange(-1f)

        val rebuilt = NowPlayingViewModel(playback, FakeWaveformSource(), presets)

        assertEquals(-1f, rebuilt.sliderValue.value, 0.0001f)
    }

    @Test
    fun `advancing within a queue leaves the fader alone`() {
        // Half the rule: auto-advance, next and previous are one continuous session, so what was
        // set for the mix stays set. The other half — loading a new queue resets — lives in the
        // controller's playQueue, which needs a real MediaController and is checked on device.
        viewModel.onSliderChange(-1f)

        playback.setState(PlayerState(mediaId = "2", isPlaying = true))

        assertEquals(-1f, viewModel.sliderValue.value, 0.0001f)
    }

    @Test
    fun `applying tempo reads the live fader position rather than a copy`() {
        viewModel.onSliderChange(-1f)
        playback.calls.clear()

        viewModel.onSliderChangeFinished()

        // Full slow travel on the fixed ±30% range (§5.2), key lock on: tempo drops, key holds.
        assertEquals(listOf("speed=0.7,pitch=1.0"), playback.calls)
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
    fun `touching the filter cutoff enables the filter, off by default`() {
        assertEquals(false, viewModel.filter.value.enabled)

        viewModel.onFilterCutoffChange(1000f)

        assertEquals(true, viewModel.filter.value.enabled)
    }

    @Test
    fun `touching a delay control enables delay, off by default`() {
        assertEquals(false, viewModel.delay.value.enabled)

        viewModel.onDelayTimeChange(200f)

        assertEquals(true, viewModel.delay.value.enabled)
    }

    @Test
    fun `touching an EQ band enables EQ, off by default`() {
        assertEquals(false, viewModel.eq.value.enabled)

        viewModel.onEqGainChange(0, 3f)

        assertEquals(true, viewModel.eq.value.enabled)
    }

    @Test
    fun `an already-enabled effect is not re-enabled on every touch`() {
        viewModel.onFilterCutoffChange(1000f)
        playback.calls.clear()

        viewModel.onFilterCutoffChange(2000f)

        // Only the cutoff call - no redundant filterEnabled(true) once it is already on.
        assertTrue(playback.calls.none { it.startsWith("filterEnabled") })
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
