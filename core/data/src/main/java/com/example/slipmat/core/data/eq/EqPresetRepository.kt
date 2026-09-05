package com.example.slipmat.core.data.eq

import com.example.slipmat.core.data.db.EqPresetDao
import com.example.slipmat.core.data.db.EqPresetEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** A saved EQ setting, as anything outside `:core:data` sees it. */
data class EqPreset(
    val name: String,
    val gainsDb: List<Float>,
)

/**
 * Named EQ settings.
 *
 * An interface with the Room implementation behind it, so a view model that saves a preset can be
 * tested without a database — the same reason `PlaybackSettings` is shaped this way.
 */
interface EqPresetStore {

    val presets: Flow<List<EqPreset>>

    suspend fun save(name: String, gainsDb: List<Float>)

    suspend fun load(name: String): EqPreset?

    suspend fun delete(name: String)
}

@Singleton
class EqPresetRepository @Inject constructor(
    private val dao: EqPresetDao,
) : EqPresetStore {

    override val presets: Flow<List<EqPreset>> =
        dao.observeAll().map { rows -> rows.map { EqPreset(it.name, it.gainsDb) } }

    /**
     * Blank names are dropped rather than saved.
     *
     * A preset with no name is unloadable and undeletable from a list that shows names, so the
     * only thing storing it achieves is a row nobody can reach.
     */
    override suspend fun save(name: String, gainsDb: List<Float>) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        dao.upsert(
            EqPresetEntity(
                name = trimmed,
                gainsDb = gainsDb,
                savedAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun load(name: String): EqPreset? =
        dao.get(name)?.let { EqPreset(it.name, it.gainsDb) }

    override suspend fun delete(name: String) = dao.delete(name)
}
