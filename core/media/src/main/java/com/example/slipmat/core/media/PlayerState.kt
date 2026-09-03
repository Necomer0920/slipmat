package com.example.slipmat.core.media

/**
 * A snapshot of playback, in plain Kotlin types so `:app` can consume it without Media3.
 *
 * [durationMs] is `0` whenever the real duration is unknown — Media3 reports `C.TIME_UNSET`
 * (a large negative) before a track is prepared, and letting that reach the UI produces negative
 * progress bars.
 */
data class PlayerState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val mediaId: String? = null,
    val title: String? = null,
    val artist: String? = null,
) {
    val hasMedia: Boolean get() = mediaId != null

    /** Fraction played, always within 0f..1f, and 0f when the duration is not yet known. */
    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    companion object {
        val EMPTY = PlayerState()
    }
}

/**
 * Builds a [PlayerState] from raw player values, sanitising the two things Media3 reports in a
 * shape the UI cannot use: an unset duration, and a position that can briefly exceed the duration
 * or go negative during seeks.
 */
fun playerStateOf(
    isPlaying: Boolean,
    rawPositionMs: Long,
    rawDurationMs: Long,
    mediaId: String? = null,
    title: String? = null,
    artist: String? = null,
): PlayerState {
    val duration = if (rawDurationMs > 0L) rawDurationMs else 0L
    val position = when {
        rawPositionMs < 0L -> 0L
        duration > 0L && rawPositionMs > duration -> duration
        else -> rawPositionMs
    }
    return PlayerState(
        isPlaying = isPlaying,
        positionMs = position,
        durationMs = duration,
        mediaId = mediaId,
        title = title?.takeIf { it.isNotBlank() },
        artist = artist?.takeIf { it.isNotBlank() },
    )
}
