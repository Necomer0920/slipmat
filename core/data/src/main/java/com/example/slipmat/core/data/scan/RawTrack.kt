package com.example.slipmat.core.data.scan

/**
 * One row exactly as MediaStore reported it — nothing cleaned up, nothing defaulted.
 *
 * Every text field is nullable because MediaStore genuinely returns nulls, and separately returns
 * the literal string `<unknown>` for missing tags. Sanitising happens in the mapper, which is a
 * pure function over this type and therefore testable without a device.
 */
data class RawTrack(
    val id: Long,
    val title: String?,
    val artist: String?,
    val albumArtist: String?,
    val album: String?,
    val albumId: Long,
    val durationMs: Long,
    val dateModified: Long,
    val displayName: String?,
    /** Absolute file path. Deprecated since API 29 and can be null under scoped storage. */
    val data: String?,
    /** Bucket-relative path, e.g. `Music/Soulseek/`. Null below API 29. */
    val relativePath: String?,
)
