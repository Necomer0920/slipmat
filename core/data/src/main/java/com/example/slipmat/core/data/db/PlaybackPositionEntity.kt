package com.example.slipmat.core.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

/**
 * Where the user got to in a track.
 *
 * Keyed by URI rather than by MediaStore id: the queue is built from URIs, and a rescan can change
 * a row's id while the file stays put.
 *
 * [updatedAt] does double duty — it is what makes "the most recently played track" answerable,
 * which is how playback is restored after the process dies.
 */
@Entity(tableName = "playback_positions")
data class PlaybackPositionEntity(
    @PrimaryKey val mediaUri: String,
    val positionMs: Long,
    val updatedAt: Long,
)

@Dao
interface PlaybackPositionDao {

    @Upsert
    suspend fun upsert(position: PlaybackPositionEntity)

    @Query("SELECT * FROM playback_positions WHERE mediaUri = :mediaUri")
    suspend fun get(mediaUri: String): PlaybackPositionEntity?

    /** The track to resume on a cold start. */
    @Query("SELECT * FROM playback_positions ORDER BY updatedAt DESC LIMIT 1")
    suspend fun mostRecent(): PlaybackPositionEntity?

    @Query("DELETE FROM playback_positions WHERE mediaUri = :mediaUri")
    suspend fun clear(mediaUri: String)
}
