package com.example.slipmat.core.media

/**
 * One entry in a playback queue, in plain strings so `:app` can build queues without Media3.
 *
 * Metadata travels with the item rather than being left to Media3's tag reader. The reader only
 * sees what is *embedded* in the file, so a track whose artwork lives in MediaStore's album-art
 * store — the common case — would otherwise show no cover at all while the album grid shows one.
 */
data class QueueItem(
    val uri: String,
    val title: String? = null,
    val artist: String? = null,
    val artworkUri: String? = null,
)
