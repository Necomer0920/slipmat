package com.example.slipmat.core.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * A named set of EQ band gains.
 *
 * The name is the primary key, so saving over an existing name replaces it — which is what "save
 * as Club" means to anyone who has used an EQ, and it removes the need for a separate rename or
 * overwrite-confirm path.
 *
 * Gains reuse [WaveformConverters], which already stores a `List<Float>` as text; the alternative
 * is eight columns that would have to change every time the band count does.
 */
@Entity(tableName = "eq_presets")
data class EqPresetEntity(
    @PrimaryKey val name: String,
    val gainsDb: List<Float>,
    val savedAt: Long,
)

@Dao
interface EqPresetDao {

    @Upsert
    suspend fun upsert(preset: EqPresetEntity)

    /** Alphabetical and case-insensitive, so the list does not reorder itself as things are saved. */
    @Query("SELECT * FROM eq_presets ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<EqPresetEntity>>

    @Query("SELECT * FROM eq_presets WHERE name = :name")
    suspend fun get(name: String): EqPresetEntity?

    @Query("DELETE FROM eq_presets WHERE name = :name")
    suspend fun delete(name: String)

    @Query("SELECT COUNT(*) FROM eq_presets")
    suspend fun count(): Int
}
