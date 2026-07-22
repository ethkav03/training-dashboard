package com.momentum.android.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "theme_preference")
private val THEME_PREFERENCE_KEY = stringPreferencesKey("theme_preference")

/** Same DataStore-backed persistence pattern as HealthConnectSyncState -- on-device only, no reason to sync this anywhere. */
class ThemePreferenceStore(private val context: Context) {
    val preference: Flow<ThemePreference> = context.themeDataStore.data.map { prefs ->
        prefs[THEME_PREFERENCE_KEY]?.let { stored ->
            runCatching { ThemePreference.valueOf(stored) }.getOrNull()
        } ?: ThemePreference.SYSTEM
    }

    suspend fun setPreference(preference: ThemePreference) {
        context.themeDataStore.edit { it[THEME_PREFERENCE_KEY] = preference.name }
    }
}
