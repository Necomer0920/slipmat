package com.example.slipmat.core.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One audio file, as indexed from MediaStore.
 *
 * The primary key is MediaStore's own id rather than a generated one: rescans must be able to tell
 * "same file, changed tags" from "new file", and MediaStore's id is the only stable identity
 * available. [dateModified] is what makes an incremental rescan possible.
 *
 * Artists, albums and folders are derived from this table with `GROUP BY` rather than getting
 * tables of their own — browse is read-only, so there is nothing to keep in sync.
 */
@Entity(
    tableName = "tracks",
    indices = [
        Index("albumArtist"),
        Index("album"),
        Index("folderPath"),
    ],
)
data class TrackEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    /** Falls back to [artist] when the file has no album-artist tag; see the scanner. */
    val albumArtist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: String,
    val folderPath: String,
    val dateModified: Long,
    val albumArtUri: String?,
)
