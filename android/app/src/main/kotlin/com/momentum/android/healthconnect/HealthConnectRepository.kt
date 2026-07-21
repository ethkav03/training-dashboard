package com.momentum.android.healthconnect

import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant

// Used for the very first sync (no changes-token yet) and as a fallback when
// a stored token has expired -- Health Connect only guarantees a token stays
// valid for a limited window, so a device that hasn't synced in a while (app
// uninstalled Health Connect data, long time since last background run) needs
// a bounded do-over rather than an error. The backend's externalId dedup
// makes re-sending already-synced records a no-op, so re-reading this window
// is safe, just not bandwidth-efficient -- which is exactly why the periodic
// background job (SyncWorker) prefers the changes-token path whenever it has one.
private val SYNC_LOOKBACK: Duration = Duration.ofDays(30)

// Steps and heart rate aren't tracked here -- heart rate only ever matters as
// an aggregate over an exercise session's own window (see
// readExerciseSessions), and steps has no home in our data model yet (see
// docs/roadmap.md). Tracking changes for record types we never sync would
// just burn through the token's limited change-log budget for no reason.
private val TRACKED_RECORD_TYPES = setOf(WeightRecord::class, ExerciseSessionRecord::class, SleepSessionRecord::class)

data class ExerciseSessionWithVitals(
    val session: ExerciseSessionRecord,
    val totalEnergyKcal: Double?,
    val avgHeartRate: Double?,
)

data class HealthConnectReadResult(
    val weightRecords: List<WeightRecord>,
    val exerciseSessions: List<ExerciseSessionWithVitals>,
    val sleepSessions: List<SleepSessionRecord>,
)

sealed interface ChangesResult {
    data class Success(val data: HealthConnectReadResult, val nextToken: String) : ChangesResult
    data object TokenExpired : ChangesResult
}

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

    suspend fun readExerciseSessions(since: Instant): List<ExerciseSessionWithVitals> {
        val sessions = client.readRecords(
            ReadRecordsRequest(ExerciseSessionRecord::class, timeRangeFilter = TimeRangeFilter.after(since))
        ).records
        return withVitals(sessions)
    }

    suspend fun readBounded(since: Instant): HealthConnectReadResult = HealthConnectReadResult(
        weightRecords = readWeightRecords(since),
        exerciseSessions = readExerciseSessions(since),
        sleepSessions = readSleepSessions(since),
    )

    /** A fresh token for callers with no stored token yet (first sync ever, or one just discarded as expired). */
    suspend fun newChangesToken(): String = client.getChangesToken(ChangesTokenRequest(TRACKED_RECORD_TYPES))

    /**
     * Walks every page of changes since [token], returning the accumulated
     * upserts plus the token to persist for next time. Deletions aren't
     * propagated to the backend in this v1 -- an on-device delete doesn't
     * retract an already-synced Momentum row -- the same accepted row-level
     * coarseness already documented for manual-edit precedence elsewhere.
     */
    suspend fun readChanges(token: String): ChangesResult {
        val weight = mutableListOf<WeightRecord>()
        val exerciseSessions = mutableListOf<ExerciseSessionRecord>()
        val sleep = mutableListOf<SleepSessionRecord>()
        var currentToken = token

        while (true) {
            val response = client.getChanges(currentToken)
            if (response.changesTokenExpired) return ChangesResult.TokenExpired

            for (change in response.changes) {
                if (change !is UpsertionChange) continue
                when (val record = change.record) {
                    is WeightRecord -> weight += record
                    is ExerciseSessionRecord -> exerciseSessions += record
                    is SleepSessionRecord -> sleep += record
                }
            }

            currentToken = response.nextChangesToken
            if (!response.hasMore) break
        }

        return ChangesResult.Success(
            data = HealthConnectReadResult(weight, withVitals(exerciseSessions), sleep),
            nextToken = currentToken,
        )
    }

    /**
     * Exercise sessions carry no energy/heart-rate totals on the record
     * itself -- Health Connect stores those as separate time-series records
     * -- so each session needs its own aggregate query scoped to that
     * session's own [startTime, endTime] window.
     */
    private suspend fun withVitals(sessions: List<ExerciseSessionRecord>): List<ExerciseSessionWithVitals> =
        sessions.map { session ->
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

    companion object {
        fun defaultSince(): Instant = Instant.now().minus(SYNC_LOOKBACK)
    }
}
