package com.example.slipmat.core.data.scan

import com.example.slipmat.core.data.db.TrackEntity

/** What MediaStore returns for a tag it could not read. It is a literal string, not a null. */
const val MEDIASTORE_UNKNOWN = "<unknown>"

const val UNKNOWN_TITLE = "Unknown title"
const val UNKNOWN_ARTIST = "Unknown artist"
const val UNKNOWN_ALBUM = "Unknown album"

/**
 * Turns a raw MediaStore row into a storable track, or `null` if it should not be stored at all.
 *
 * Kept free of `android.net.Uri` and every other framework type so it runs as a plain JVM unit
 * test — the tag-handling rules here are the part most likely to be wrong, so they need to be
 * cheap to test.
 *
 * @param contentUriBase normally `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString()`; passed
 *   in rather than referenced so this stays testable off-device.
 */
fun RawTrack.toEntityOrNull(contentUriBase: String): TrackEntity? {
    // A zero or negative duration means MediaStore could not decode it. Unplayable; skip it.
    if (durationMs <= 0L) return null

    val cleanArtist = artist.cleanTag() ?: UNKNOWN_ARTIST

    return TrackEntity(
        id = id,
        title = title.cleanTag() ?: displayName.withoutExtension() ?: UNKNOWN_TITLE,
        artist = cleanArtist,
        albumArtist = cleanArtist,
        album = album.cleanTag() ?: UNKNOWN_ALBUM,
        albumId = albumId,
        durationMs = durationMs,
        uri = "$contentUriBase/$id",
        folderPath = "",
        dateModified = dateModified,
        albumArtUri = null,
    )
}

/** Null, blank, and MediaStore's `<unknown>` sentinel all mean "no tag". */
private fun String?.cleanTag(): String? =
    this?.trim()?.takeIf { it.isNotEmpty() && !it.equals(MEDIASTORE_UNKNOWN, ignoreCase = true) }

/** `Some Song.mp3` reads better than `Unknown title` when the title tag is missing. */
private fun String?.withoutExtension(): String? =
    cleanTag()?.substringBeforeLast('.')?.takeIf { it.isNotEmpty() }
