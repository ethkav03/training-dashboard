package com.momentum.android.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.momentum.android.auth.TokenStore
import com.momentum.android.healthconnect.ChangesResult
import com.momentum.android.healthconnect.HealthConnectManager
import com.momentum.android.healthconnect.HealthConnectMapper
import com.momentum.android.healthconnect.HealthConnectRepository
import com.momentum.android.healthconnect.HealthConnectSyncState
import com.momentum.android.network.ApiClient
import com.momentum.android.network.HealthConnectSyncRequest
import kotlinx.coroutines.flow.first

/**
 * Background counterpart to the manual "Sync now" button on the Settings
 * screen --
 * same read-map-post pipeline, but incremental via Health Connect's
 * changes-token instead of always re-reading a 30-day window, since this one
 * runs unattended every ~6h rather than once per user tap.
 *
 * Constructed via WorkManager's default reflective WorkerFactory (public
 * (Context, WorkerParameters) constructor) -- no custom DI wiring needed,
 * since every dependency it uses is built by hand inside doWork(), the same
 * way each ViewModel.Factory in this app builds its own dependencies.
 */
class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val tokenStore = TokenStore(applicationContext)
        // Not signed in -- nothing to authenticate the sync request with.
        // Not a failure; just nothing to do until the user signs back in.
        if (tokenStore.token == null) return Result.success()

        val manager = HealthConnectManager(applicationContext)
        if (manager.availability != HealthConnectManager.Availability.AVAILABLE) return Result.success()
        if (!manager.hasAllPermissions()) return Result.success()

        val repository = HealthConnectRepository(manager)
        val syncState = HealthConnectSyncState(applicationContext)
        val api = ApiClient.create(tokenStore)

        return try {
            val storedToken = syncState.changesToken.first()
            val request = if (storedToken == null) {
                fullReadAndResetToken(repository, syncState)
            } else {
                when (val result = repository.readChanges(storedToken)) {
                    is ChangesResult.TokenExpired -> fullReadAndResetToken(repository, syncState)
                    is ChangesResult.Success -> {
                        syncState.updateToken(result.nextToken)
                        HealthConnectMapper.toSyncRequest(result.data)
                    }
                }
            }
            api.syncHealthConnect(request)
            Result.success()
        } catch (e: Exception) {
            // Network hiccup, expired JWT, transient Health Connect error --
            // WorkManager's own backoff policy handles the retry cadence;
            // nothing here can distinguish "worth retrying" from "won't ever
            // work" without more error detail than these SDKs surface.
            Result.retry()
        }
    }

    private suspend fun fullReadAndResetToken(
        repository: HealthConnectRepository,
        syncState: HealthConnectSyncState,
    ): HealthConnectSyncRequest {
        val bounded = repository.readBounded(HealthConnectRepository.defaultSince())
        // Token fetched *after* the read so it only covers changes from this
        // point forward -- anything already captured by this bounded read
        // shouldn't also show up as a "change" next time.
        syncState.updateToken(repository.newChangesToken())
        return HealthConnectMapper.toSyncRequest(bounded)
    }
}
