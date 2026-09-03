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

    fun play()

    fun pause()

    fun next()

    fun previous()

    fun seekTo(positionMs: Long)
}
