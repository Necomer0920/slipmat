package com.example.slipmat.core.media

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
}
