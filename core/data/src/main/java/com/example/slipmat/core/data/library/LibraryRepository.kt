package com.example.slipmat.core.data.library

import android.provider.MediaStore
import androidx.room.withTransaction
import com.example.slipmat.core.data.db.SlipmatDatabase
import com.example.slipmat.core.data.scan.ScanDiff
import com.example.slipmat.core.data.scan.computeScanDiff
import com.example.slipmat.core.data.db.AlbumSummary
import com.example.slipmat.core.data.db.ArtistSummary
import com.example.slipmat.core.data.db.FolderSummary
import com.example.slipmat.core.data.db.TrackDao
import com.example.slipmat.core.data.db.TrackEntity
import com.example.slipmat.core.data.scan.AudioSource
import com.example.slipmat.core.data.scan.toEntityOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the local library index: reads MediaStore, keeps Room in step, and exposes the result.
 *
 * The UI never touches [AudioSource] or [TrackDao] directly.
 */
@Singleton
class LibraryRepository @Inject constructor(
    private val scanner: AudioSource,
    private val trackDao: TrackDao,
    private val database: SlipmatDatabase,
) {

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)

    /** Observable scan progress, so the UI can show something during a long first scan. */
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    fun observeTracks(): Flow<List<TrackEntity>> = trackDao.observeAllTracks()

    // Browse lists are GROUP BY results, computed on demand rather than stored.
    fun observeArtists(): Flow<List<ArtistSummary>> = trackDao.observeArtists()

    fun observeAlbums(): Flow<List<AlbumSummary>> = trackDao.observeAlbums()

    fun observeFolders(): Flow<List<FolderSummary>> = trackDao.observeFolders()

    fun observeTracksByArtist(artist: String): Flow<List<TrackEntity>> =
        trackDao.observeTracksByArtist(artist)

    fun observeTracksInAlbum(album: String, albumArtist: String): Flow<List<TrackEntity>> =
        trackDao.observeTracksInAlbum(album, albumArtist)

    fun observeTracksInFolder(folderPath: String): Flow<List<TrackEntity>> =
        trackDao.observeTracksInFolder(folderPath)

    /**
     * Full scan: read everything MediaStore has and write it all.
     *
     * Upsert makes this idempotent, so re-running it is harmless — but it rewrites every row every
     * time. [scanIncremental] replaces it for routine rescans.
     */
    suspend fun scan(): Int = withContext(Dispatchers.IO) {
        val entities = scanner.queryAudio().mapNotNull { it.toEntityOrNull(CONTENT_URI_BASE) }
        trackDao.upsertAll(entities)
        entities.size
    }

    /**
     * Rescan, writing only what changed.
     *
     * The whole diff is applied in one transaction: a scan interrupted midway must not leave the
     * library with deletions applied and insertions missing.
     */
    suspend fun scanIncremental(): ScanDiff = withContext(Dispatchers.IO) {
        _scanState.value = ScanState.Scanning(done = 0, total = 0)

        val rows = scanner.queryAudio()
        val scanned = ArrayList<TrackEntity>(rows.size)
        rows.forEachIndexed { index, row ->
            row.toEntityOrNull(CONTENT_URI_BASE)?.let(scanned::add)
            // Reporting every row would thrash the flow on a large library.
            if (index % PROGRESS_STRIDE == 0) {
                _scanState.value = ScanState.Scanning(done = index, total = rows.size)
            }
        }

        val diff = computeScanDiff(scanned, trackDao.getAllIdsWithDateModified())

        if (!diff.isEmpty) {
            database.withTransaction {
                // SQLite caps host parameters per statement, and a large library blows past it,
                // so deletions go in batches rather than one enormous IN clause.
                diff.deletedIds.chunked(SQLITE_VARIABLE_LIMIT).forEach { trackDao.deleteByIds(it) }
                (diff.inserted + diff.updated)
                    .chunked(SQLITE_VARIABLE_LIMIT)
                    .forEach { trackDao.upsertAll(it) }
            }
        }
        _scanState.value = ScanState.Complete(
            trackCount = scanned.size,
            changeCount = diff.changeCount,
        )
        diff
    }

    private companion object {
        val CONTENT_URI_BASE: String = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString()

        /** SQLite's default `SQLITE_MAX_VARIABLE_NUMBER` is 999; stay comfortably under it. */
        const val SQLITE_VARIABLE_LIMIT = 900

        /** Emit progress every N rows rather than on every one. */
        const val PROGRESS_STRIDE = 50
    }
}
