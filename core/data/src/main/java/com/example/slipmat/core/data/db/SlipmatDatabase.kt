package com.example.slipmat.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Local library index.
 *
 * Version 1 holds only tracks. The waveform cache (Phase 5) and EQ presets (Phase 7) each add a
 * table and a migration, which is why schemas are exported from the start — a migration test needs
 * the previous schema on disk to migrate *from*.
 */
@Database(
    entities = [TrackEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class SlipmatDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao

    companion object {
        const val NAME = "slipmat.db"
    }
}
