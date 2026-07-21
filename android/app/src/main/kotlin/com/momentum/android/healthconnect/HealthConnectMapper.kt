package com.momentum.android.healthconnect

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.WeightRecord
import com.momentum.android.network.HealthConnectExerciseSessionDto
import com.momentum.android.network.HealthConnectSleepSessionDto
import com.momentum.android.network.HealthConnectSyncRequest
import com.momentum.android.network.HealthConnectWeightRecordDto
import java.time.Duration

// Mirrors backend/src/services/healthConnectService.ts's
// EXERCISE_TYPE_TO_ACTIVITY_TYPE lookup -- these key strings must match
// exactly. Anything not in this map already falls back to ActivityType.OTHER
// server-side, so an unmapped Health Connect exercise type is a display
// nuance, not a sync failure.
private val EXERCISE_TYPE_NAMES: Map<Int, String> = mapOf(
    ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING to "strength_training",
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING to "running",
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL to "running_treadmill",
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING to "biking",
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY to "biking_stationary",
    ExerciseSessionRecord.EXERCISE_TYPE_WALKING to "walking",
    ExerciseSessionRecord.EXERCISE_TYPE_HIKING to "hiking",
    ExerciseSessionRecord.EXERCISE_TYPE_SOCCER to "soccer",
    ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL to "basketball",
    ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN to "football_american",
    ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AUSTRALIAN to "football_australian",
    ExerciseSessionRecord.EXERCISE_TYPE_RUGBY to "rugby",
)

private val NON_SLEEP_STAGE_TYPES = setOf(
    SleepSessionRecord.STAGE_TYPE_AWAKE,
    SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED,
    SleepSessionRecord.STAGE_TYPE_OUT_OF_BED,
)

object HealthConnectMapper {

    fun toSyncRequest(
        weightRecords: List<WeightRecord>,
        exerciseSessions: List<ExerciseSessionWithVitals>,
        sleepSessions: List<SleepSessionRecord>,
    ): HealthConnectSyncRequest = HealthConnectSyncRequest(
        weightRecords = weightRecords.map {
            HealthConnectWeightRecordDto(
                externalId = it.metadata.id,
                date = it.time.toString(),
                weightKg = it.weight.inKilograms,
            )
        },
        exerciseSessions = exerciseSessions.map { (session, energyKcal, avgHr) ->
            HealthConnectExerciseSessionDto(
                externalId = session.metadata.id,
                startTime = session.startTime.toString(),
                endTime = session.endTime.toString(),
                exerciseType = EXERCISE_TYPE_NAMES[session.exerciseType] ?: "other",
                totalEnergyKcal = energyKcal,
                avgHeartRate = avgHr,
            )
        },
        sleepSessions = sleepSessions.map { session ->
            HealthConnectSleepSessionDto(
                externalId = session.metadata.id,
                date = session.startTime.toString(),
                totalSleepMinutes = totalSleepMinutes(session),
            )
        },
    )

    /**
     * Sums only the stages actually asleep (light/deep/REM/unknown-but-
     * recorded), excluding awake-in-bed and out-of-bed time -- falls back to
     * the full session span when the device reported no stages at all,
     * since a whole night with zero stage detail is still better signal
     * than discarding it outright.
     */
    private fun totalSleepMinutes(session: SleepSessionRecord): Double {
        if (session.stages.isEmpty()) {
            return Duration.between(session.startTime, session.endTime).toMinutes().toDouble()
        }
        return session.stages
            .filter { it.stage !in NON_SLEEP_STAGE_TYPES }
            .sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
            .toDouble()
    }
}
