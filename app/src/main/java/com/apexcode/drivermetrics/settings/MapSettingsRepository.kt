package com.apexcode.drivermetrics.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * Persists the mini-map's zoom preference: wide (some margin around the route on every side —
 * the current default) vs. close (the route fills the whole map, no margin — how it looked
 * before this setting was added). Read by OverlayController/OverlayContent, written from
 * MainActivity's settings checkbox.
 */
@Singleton
class MapSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.settingsDataStore

    val wideMapZoom: Flow<Boolean> = dataStore.data.map { it[WIDE_MAP_ZOOM_KEY] ?: true }

    suspend fun setWideMapZoom(enabled: Boolean) {
        dataStore.edit { it[WIDE_MAP_ZOOM_KEY] = enabled }
    }

    private companion object {
        val WIDE_MAP_ZOOM_KEY = booleanPreferencesKey("wide_map_zoom")
    }
}
