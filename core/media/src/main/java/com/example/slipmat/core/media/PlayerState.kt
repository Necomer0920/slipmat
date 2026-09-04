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
    /** Artwork location as a plain string; resolving it is the UI's business, not this module's. */
    val artworkUri: String? = null,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.Off,
    /** The whole queue, in play order. Empty until something is queued. */
    val queue: List<QueueItem> = emptyList(),
    /** Index of the playing item within [queue], or `-1` when nothing is queued. */
    val queueIndex: Int = -1,
) {
    val hasMedia: Boolean get() = mediaId != null

    /** What is still to come, excluding the item playing now. */
    val upNext: List<QueueItem>
        get() = if (queueIndex in queue.indices) queue.drop(queueIndex + 1) else emptyList()

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
    artworkUri: String? = null,
    shuffleEnabled: Boolean = false,
    repeatMode: RepeatMode = RepeatMode.Off,
    queue: List<QueueItem> = emptyList(),
    queueIndex: Int = -1,
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
        artworkUri = artworkUri?.takeIf { it.isNotBlank() },
        shuffleEnabled = shuffleEnabled,
        repeatMode = repeatMode,
        queue = queue,
        queueIndex = queueIndex,
    )
}
