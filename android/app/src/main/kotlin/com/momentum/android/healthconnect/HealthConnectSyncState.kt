package com.momentum.android.healthconnect

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.healthConnectSyncDataStore by preferencesDataStore(name = "health_connect_sync")
private val CHANGES_TOKEN_KEY = stringPreferencesKey("changes_token")

/**
 * Persists Health Connect's incremental changes-token on-device only -- it's
 * meaningless off-device (Health Connect scopes it to this app's own read
 * history), so unlike every actual health record this app syncs, it is never
 * sent to the backend.
 */
class HealthConnectSyncState(private val context: Context) {
    val changesToken: Flow<String?>
        get() = context.healthConnectSyncDataStore.data.map { it[CHANGES_TOKEN_KEY] }

    suspend fun updateToken(token: String) {
        context.healthConnectSyncDataStore.edit { it[CHANGES_TOKEN_KEY] = token }
    }

    suspend fun clearToken() {
        context.healthConnectSyncDataStore.edit { it.remove(CHANGES_TOKEN_KEY) }
    }
}
