package com.example.slipmat.core.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Just the two columns an incremental rescan needs, so it never loads whole rows to diff. */
data class TrackStamp(
    val id: Long,
    val dateModified: Long,
)

@Dao
interface TrackDao {

    /** Insert-or-update, so a rescan of unchanged files is idempotent. */
    @Upsert
    suspend fun upsertAll(tracks: List<TrackEntity>)

    @Query("DELETE FROM tracks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    /** Drives the rescan diff: what we already hold, and how fresh it is. */
    @Query("SELECT id, dateModified FROM tracks")
    suspend fun getAllIdsWithDateModified(): List<TrackStamp>

    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE")
    fun observeAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun count(): Int
}
