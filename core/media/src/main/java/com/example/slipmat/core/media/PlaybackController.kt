package com.example.slipmat.core.media

import com.example.slipmat.core.media.dsp.DelayState
import com.example.slipmat.core.media.dsp.FilterMode
import com.example.slipmat.core.media.dsp.FilterState
import kotlinx.coroutines.flow.StateFlow

/**
 * What `:app` is allowed to know about playback.
 *
 * Deliberately free of every Media3 type. If a `MediaController`, `Player` or `MediaItem` ever
 * appears in this interface, Media3 lands on `:app`'s compile classpath and the boundary the whole
 * architecture rests on is gone — silently, with nothing failing to build.
 */
interface PlaybackController {

    /** Current playback state, mirrored from the service. Never held independently by the UI. */
    val state: StateFlow<PlayerState>

    /** Binds to the playback service. Idempotent, so every Activity start may call it. */
    fun connect()

    /** Drops the binding. Playback continues in the service regardless. */
    fun release()

    /** Replaces the queue and starts playing at [startIndex]. */
    fun playQueue(items: List<QueueItem>, startIndex: Int)

    fun play()

    fun pause()

    fun next()

    fun previous()

    fun seekTo(positionMs: Long)

    /** Jump [SKIP_SEEK_MS] forward, clamped to the end of the track. */
    fun skipForward()

    /** Jump [SKIP_SEEK_MS] back, clamped to the start of the track. */
    fun skipBack()

    fun setShuffle(enabled: Boolean)

    fun setRepeatMode(mode: RepeatMode)

    /** Reorder the queue. Indices are positions in [PlayerState.queue]. */
    fun moveQueueItem(fromIndex: Int, toIndex: Int)

    /** Jump straight to a queue position without rebuilding the queue. */
    fun skipToQueueIndex(index: Int)

    /** Countdown to the end of playback. Keeps running with the app backgrounded. */
    val sleepTimerState: StateFlow<SleepTimerState>

    fun startSleepTimer(durationMs: Long)

    fun cancelSleepTimer()

    /**
     * Sets tempo and key together.
     *
     * Both are supplied rather than derived here so the decision about what key lock *means* stays
     * in one tested place — see [speedPitchFor].
     */
    /**
     * The pitch fader's position: -1f at the slowest end, 0f at centre, 1f at the fastest.
     *
     * Held here because the player's parameters live this long. A position owned by the
     * now-playing ViewModel dies with the screen while the audio stays pitched, so coming back to a
     * track still playing at 0.92x showed a fader at centre and a readout of +0.0%.
     */
    val tempoSlider: StateFlow<Float>

    /**
     * Whether the key holds while the tempo moves.
     *
     * Performance state, like [tempoSlider], and reset alongside it — not a saved preference. A
     * vinyl-mode setting that outlived the track it was set for is how a fresh song ends up
     * playing a semitone flat for no visible reason.
     */
    val keyLock: StateFlow<Boolean>

    /** Moves the fader without touching the player — cheap enough to call on every drag frame. */
    fun moveTempoFader(sliderValue: Float)

    /** Sets key lock. Takes effect on the next [applyTempo]. */
    fun setKeyLock(enabled: Boolean)

    /**
     * Applies the fader's current position to the player.
     *
     * Takes the position from [tempoSlider] rather than as an argument, so what is heard and what is
     * displayed cannot drift apart. This reconfigures the audio pipeline, so callers throttle it.
     */
    fun applyTempo(range: PitchRange)

    /** Filter state, mirrored for the UI. `FilterMode` is our own enum, not a Media3 type. */
    val filterState: StateFlow<FilterState>

    fun setFilterEnabled(enabled: Boolean)

    fun setFilterCutoff(hz: Float)

    fun setFilterMode(mode: FilterMode)

    /** Delay state, mirrored for the UI. */
    val delayState: StateFlow<DelayState>

    fun setDelayEnabled(enabled: Boolean)

    fun setDelayTime(ms: Float)

    fun setDelayFeedback(value: Float)

    fun setDelayMix(value: Float)
}
