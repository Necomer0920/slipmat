package com.example.slipmat.core.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "playback_settings")

private val KEY_LOCK = booleanPreferencesKey("key_lock")
private val PITCH_RANGE = stringPreferencesKey("pitch_range")

/**
 * Small, durable playback preferences.
 *
 * The pitch range in particular has to survive a restart: it changes what the same slider travel
 * means, so silently resetting it to ±8% would make a track the user had set up sound wrong.
 *
 * Stored as the enum's name rather than its ordinal — reordering the enum would otherwise
 * reinterpret everyone's saved setting.
 */
@Singleton
class PlaybackSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    val keyLock: Flow<Boolean> = context.dataStore.data.map { it[KEY_LOCK] ?: true }

    val pitchRangeName: Flow<String?> = context.dataStore.data.map { it[PITCH_RANGE] }

    suspend fun setKeyLock(enabled: Boolean) {
        context.dataStore.edit { it[KEY_LOCK] = enabled }
    }

    suspend fun setPitchRangeName(name: String) {
        context.dataStore.edit { it[PITCH_RANGE] = name }
    }
}
