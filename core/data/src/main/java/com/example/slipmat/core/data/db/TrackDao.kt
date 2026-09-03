package com.example.slipmat.core.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** One row per artist in the browse list. */
data class ArtistSummary(val artist: String, val trackCount: Int, val albumCount: Int)

/** One row per album. Grouped on albumArtist so compilations stay whole. */
data class AlbumSummary(
    val album: String,
    val albumArtist: String,
    val albumArtUri: String?,
    val trackCount: Int,
)

/** One row per folder on disk that holds audio. */
data class FolderSummary(val folderPath: String, val trackCount: Int)

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

    /**
     * Browse lists are derived, not stored.
     *
     * Albums group on `albumArtist`, never on `artist` — grouping a compilation by its per-track
     * artists turns one album into a dozen single-track ones.
     */
    @Query(
        """
        SELECT albumArtist AS artist,
               COUNT(*) AS trackCount,
               COUNT(DISTINCT album) AS albumCount
        FROM tracks
        GROUP BY albumArtist
        ORDER BY albumArtist COLLATE NOCASE
        """,
    )
    fun observeArtists(): Flow<List<ArtistSummary>>

    @Query(
        """
        SELECT album AS album,
               albumArtist AS albumArtist,
               MIN(albumArtUri) AS albumArtUri,
               COUNT(*) AS trackCount
        FROM tracks
        GROUP BY album, albumArtist
        ORDER BY albumArtist COLLATE NOCASE, album COLLATE NOCASE
        """,
    )
    fun observeAlbums(): Flow<List<AlbumSummary>>

    @Query(
        """
        SELECT folderPath AS folderPath, COUNT(*) AS trackCount
        FROM tracks
        WHERE folderPath != ''
        GROUP BY folderPath
        ORDER BY folderPath COLLATE NOCASE
        """,
    )
    fun observeFolders(): Flow<List<FolderSummary>>

    @Query("SELECT * FROM tracks WHERE album = :album AND albumArtist = :albumArtist ORDER BY title COLLATE NOCASE")
    fun observeTracksInAlbum(album: String, albumArtist: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE albumArtist = :artist ORDER BY album COLLATE NOCASE, title COLLATE NOCASE")
    fun observeTracksByArtist(artist: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE folderPath = :folderPath ORDER BY title COLLATE NOCASE")
    fun observeTracksInFolder(folderPath: String): Flow<List<TrackEntity>>
}
