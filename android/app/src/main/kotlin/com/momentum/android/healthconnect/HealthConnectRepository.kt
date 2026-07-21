package com.momentum.android.healthconnect

import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant

// Manual "Sync now" only this sprint -- no persisted changes-token yet (that
// lands in Sprint 13's WorkManager periodic sync). Each sync simply re-reads
// this trailing window; the backend's externalId dedup makes re-sending
// already-synced records a no-op, so repeating the read is safe, just not
// bandwidth-efficient for a background job.
private val SYNC_LOOKBACK: Duration = Duration.ofDays(30)

data class ExerciseSessionWithVitals(
    val session: ExerciseSessionRecord,
    val totalEnergyKcal: Double?,
    val avgHeartRate: Double?,
)

class HealthConnectRepository(private val manager: HealthConnectManager) {

    private val client get() = manager.client

    suspend fun readWeightRecords(since: Instant): List<WeightRecord> =
        client.readRecords(
            ReadRecordsRequest(WeightRecord::class, timeRangeFilter = TimeRangeFilter.after(since))
        ).records

    suspend fun readSleepSessions(since: Instant): List<SleepSessionRecord> =
        client.readRecords(
            ReadRecordsRequest(SleepSessionRecord::class, timeRangeFilter = TimeRangeFilter.after(since))
        ).records

    /**
     * Exercise sessions carry no energy/heart-rate totals on the record
     * itself -- Health Connect stores those as separate time-series records
     * -- so each session needs its own aggregate query scoped to that
     * session's own [startTime, endTime] window.
     */
    suspend fun readExerciseSessions(since: Instant): List<ExerciseSessionWithVitals> {
        val sessions = client.readRecords(
            ReadRecordsRequest(ExerciseSessionRecord::class, timeRangeFilter = TimeRangeFilter.after(since))
        ).records

        return sessions.map { session ->
            val aggregate = client.aggregate(
                AggregateRequest(
                    // Active, not total: TotalCaloriesBurnedRecord folds in
                    // basal metabolic rate for the whole window, which would
                    // overcount "calories burned by this workout" -- the
                    // same non-invented-precision standard applied to WHOOP
                    // workouts (kJ -> kcal) and Health Connect's placeholder
                    // intensity elsewhere in this sync path.
                    metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL, HeartRateRecord.BPM_AVG),
                    timeRangeFilter = TimeRangeFilter.between(session.startTime, session.endTime),
                )
            )
            ExerciseSessionWithVitals(
                session = session,
                totalEnergyKcal = aggregate[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories,
                avgHeartRate = aggregate[HeartRateRecord.BPM_AVG]?.toDouble(),
            )
        }
    }

    companion object {
        fun defaultSince(): Instant = Instant.now().minus(SYNC_LOOKBACK)
    }
}
