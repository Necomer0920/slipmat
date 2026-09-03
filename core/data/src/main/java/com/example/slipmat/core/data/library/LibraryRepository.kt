package com.example.slipmat.core.data.library

import android.provider.MediaStore
import com.example.slipmat.core.data.db.TrackDao
import com.example.slipmat.core.data.db.TrackEntity
import com.example.slipmat.core.data.scan.MediaStoreScanner
import com.example.slipmat.core.data.scan.toEntityOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the local library index: reads MediaStore, keeps Room in step, and exposes the result.
 *
 * The UI never touches [MediaStoreScanner] or [TrackDao] directly.
 */
@Singleton
class LibraryRepository @Inject constructor(
    private val scanner: MediaStoreScanner,
    private val trackDao: TrackDao,
) {

    fun observeTracks(): Flow<List<TrackEntity>> = trackDao.observeAllTracks()

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

    private companion object {
        val CONTENT_URI_BASE: String = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString()
    }
}
