package com.example.slipmat.core.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "playback_settings")

private val PITCH_RANGE = stringPreferencesKey("pitch_range")

/**
 * Small, durable playback preferences.
 *
 * Configuration only. The pitch range has to survive a restart because it changes what the same
 * slider travel *means*, so resetting it silently would make a track the user had set up sound
 * wrong. Performance state — the fader position, key lock — deliberately does not live here: it
 * belongs to the queue being played and is reset when a new one is loaded.
 *
 * Stored as the enum's name rather than its ordinal — reordering the enum would otherwise
 * reinterpret everyone's saved setting.
 */
interface PlaybackSettings {

    val pitchRangeName: Flow<String?>

    suspend fun setPitchRangeName(name: String)
}

/**
 * An interface with a DataStore implementation behind it, rather than the DataStore class itself:
 * a view model that reads settings should be testable without a `Context`.
 */
@Singleton
class DataStorePlaybackSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) : PlaybackSettings {

    override val pitchRangeName: Flow<String?> = context.dataStore.data.map { it[PITCH_RANGE] }

    override suspend fun setPitchRangeName(name: String) {
        context.dataStore.edit { it[PITCH_RANGE] = name }
    }
}
