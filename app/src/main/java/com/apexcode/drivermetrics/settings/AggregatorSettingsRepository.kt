package com.apexcode.drivermetrics.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.apexcode.drivermetrics.core.model.settings.AggregatorId
import com.apexcode.drivermetrics.core.model.settings.AggregatorSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

// Own DataStore file, distinct from MapSettingsRepository's "settings" one: androidx.datastore
// requires each on-disk file to be backed by exactly one preferencesDataStore property per
// process, so reusing that name from a second file would crash at runtime.
private val Context.aggregatorSettingsDataStore by preferencesDataStore(name = "aggregator_settings")

/**
 * Persists per-aggregator settings (9.1): route display mode, evaluation criteria and filter
 * rules, each stored as one JSON blob per aggregator plus a "shared" blob used while [syncEnabled]
 * is on. Reading/writing through [settingsFor]/[updateSettings] keeps that sync behavior in one
 * place rather than pushing "which bucket am I supposed to touch" onto every caller.
 */
@Singleton
class AggregatorSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val json: Json,
) {
    private val dataStore = context.aggregatorSettingsDataStore

    val syncEnabled: Flow<Boolean> = dataStore.data.map { it[SYNC_ENABLED_KEY] ?: true }

    suspend fun setSyncEnabled(enabled: Boolean) {
        dataStore.edit { it[SYNC_ENABLED_KEY] = enabled }
    }

    /** Shared settings when sync is on; that aggregator's own settings otherwise. */
    fun settingsFor(aggregatorId: AggregatorId): Flow<AggregatorSettings> = dataStore.data.map { prefs ->
        val sync = prefs[SYNC_ENABLED_KEY] ?: true
        val key = if (sync) SHARED_SETTINGS_KEY else aggregatorKey(aggregatorId)
        decode(prefs[key])
    }

    /**
     * Applies [transform] to the settings currently in effect for [aggregatorId] and writes the
     * result back to whichever bucket is currently active (shared while synced, that aggregator's
     * own bucket otherwise) — so a change made to any one aggregator while synced is immediately
     * visible to all the others, per 9.1.
     */
    suspend fun updateSettings(aggregatorId: AggregatorId, transform: (AggregatorSettings) -> AggregatorSettings) {
        val sync = syncEnabled.first()
        val key = if (sync) SHARED_SETTINGS_KEY else aggregatorKey(aggregatorId)
        dataStore.edit { prefs ->
            val current = decode(prefs[key])
            prefs[key] = json.encodeToString(AggregatorSettings.serializer(), transform(current))
        }
    }

    private fun decode(raw: String?): AggregatorSettings =
        raw?.let { json.decodeFromString(AggregatorSettings.serializer(), it) } ?: AggregatorSettings()

    private fun aggregatorKey(aggregatorId: AggregatorId) = stringPreferencesKey("settings_${aggregatorId.name}")

    private companion object {
        val SYNC_ENABLED_KEY = booleanPreferencesKey("sync_enabled")
        val SHARED_SETTINGS_KEY = stringPreferencesKey("settings_shared")
    }
}
