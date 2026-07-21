package com.momentum.android.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration

private const val UNIQUE_WORK_NAME = "health_connect_periodic_sync"
private val SYNC_INTERVAL: Duration = Duration.ofHours(6)

/**
 * Schedules SyncWorker to run roughly every 6 hours. "Roughly" is the honest
 * word -- WorkManager (and the Doze/App Standby buckets underneath it) can
 * and will delay this well past 6 hours if the device is idle or in a
 * restricted standby bucket. That's an OS constraint being documented, not
 * fought: no foreground service, exact alarm, or battery-exemption prompt in
 * this build (see docs/roadmap.md's Android scope cuts).
 */
object SyncScheduler {

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<SyncWorker>(SYNC_INTERVAL)
            .setConstraints(constraints)
            .build()

        // KEEP, not REPLACE: calling schedule() again (e.g. every time the
        // sync screen re-checks permission status) shouldn't reset an
        // already-running periodic job's schedule.
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}
