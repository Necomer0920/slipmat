package com.example.slipmat.core.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Local library index.
 *
 * v1 held only tracks; v2 adds resume positions. The waveform cache (Phase 5) and EQ presets
 * (Phase 7) each add another table, so their migrations are 2→3 and 3→4 rather than the 1→2 and
 * 2→3 the ledger originally assumed. Schemas are exported from the start because a migration test
 * needs the previous schema on disk to migrate *from*.
 */
@Database(
    entities = [TrackEntity::class, PlaybackPositionEntity::class, WaveformEntity::class],
    version = 3,
    exportSchema = true,
    // Adding a table is a purely additive change, so Room can generate the migration from the
    // exported schemas. Hand-written migrations are reserved for changes it cannot infer.
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
@TypeConverters(WaveformConverters::class)
abstract class SlipmatDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao

    abstract fun playbackPositionDao(): PlaybackPositionDao

    abstract fun waveformDao(): WaveformDao

    companion object {
        const val NAME = "slipmat.db"
    }
}
