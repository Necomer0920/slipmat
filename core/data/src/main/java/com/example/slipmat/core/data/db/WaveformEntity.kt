package com.example.slipmat.core.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverter
import androidx.room.Upsert

/**
 * A decoded waveform, cached so a track only ever pays for decoding once.
 *
 * Keyed by URI to match [PlaybackPositionEntity] — a rescan can change a MediaStore id while the
 * file itself is untouched, and re-decoding the whole library for that would be absurd.
 *
 * Peaks are stored as a comma-separated string rather than a BLOB: 400 short decimals compress
 * about as well, and a readable column makes the cache inspectable when something looks wrong.
 */
@Entity(tableName = "waveforms")
data class WaveformEntity(
    @PrimaryKey val mediaUri: String,
    val peaks: List<Float>,
    val generatedAt: Long,
)

class WaveformConverters {

    @TypeConverter
    fun fromPeaks(peaks: List<Float>): String =
        peaks.joinToString(separator = ",") { "%.4f".format(it) }

    @TypeConverter
    fun toPeaks(stored: String): List<Float> =
        if (stored.isEmpty()) emptyList() else stored.split(',').mapNotNull { it.toFloatOrNull() }
}

@Dao
interface WaveformDao {

    @Upsert
    suspend fun upsert(waveform: WaveformEntity)

    @Query("SELECT * FROM waveforms WHERE mediaUri = :mediaUri")
    suspend fun get(mediaUri: String): WaveformEntity?

    @Query("DELETE FROM waveforms WHERE mediaUri = :mediaUri")
    suspend fun clear(mediaUri: String)

    @Query("SELECT COUNT(*) FROM waveforms")
    suspend fun count(): Int
}
