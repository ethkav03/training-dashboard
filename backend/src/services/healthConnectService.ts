import type { ActivityType } from "@momentum/shared";
import { computeReadiness, getBaseline } from "./recoveryService.js";
import {
  upsertExternalRecoveryRecord,
  upsertExternalTrainingSession,
  upsertExternalWeightEntry,
} from "./externalSyncService.js";
import { computeTrainingLoad } from "./trainingService.js";

export interface HealthConnectSyncPayload {
  weightRecords: { externalId: string; date: string; weightKg: number }[];
  exerciseSessions: {
    externalId: string;
    startTime: string;
    endTime: string;
    exerciseType: string;
    totalEnergyKcal?: number | null;
    avgHeartRate?: number | null;
  }[];
  sleepSessions: { externalId: string; date: string; totalSleepMinutes: number }[];
}

export interface HealthConnectSyncResult {
  weightRecordsSynced: number;
  weightRecordsSkippedManualEdit: number;
  trainingSessionsSynced: number;
  trainingSessionsSkippedManualEdit: number;
  recoveryRecordsSynced: number;
  recoveryRecordsSkippedManualEdit: number;
}

// The Android client resolves Health Connect's numeric ExerciseSessionRecord
// type constants to a human-readable name before sending, so this matches
// WHOOP's sport-name approach for consistency rather than hardcoding ints
// here that would drift silently if Health Connect's enum changes.
const EXERCISE_TYPE_TO_ACTIVITY_TYPE: Record<string, ActivityType> = {
  strength_training: "GYM",
  weightlifting: "GYM",
  running: "RUNNING",
  running_treadmill: "RUNNING",
  biking: "CYCLING",
  biking_stationary: "CYCLING",
  walking: "WALKING",
  hiking: "WALKING",
  soccer: "TEAM_SPORT_TRAINING",
  basketball: "TEAM_SPORT_TRAINING",
  football_american: "TEAM_SPORT_TRAINING",
  football_australian: "TEAM_SPORT_TRAINING",
  rugby: "TEAM_SPORT_TRAINING",
};

function mapExerciseType(exerciseType: string): ActivityType {
  return EXERCISE_TYPE_TO_ACTIVITY_TYPE[exerciseType.trim().toLowerCase()] ?? "OTHER";
}

// Health Connect exercise sessions carry no perceived-exertion signal at
// all -- a fixed placeholder avoids inventing an RPE from heart rate, the
// same "no invented precision" call made for WHOOP workouts.
const PLACEHOLDER_INTENSITY = 5;

function dayStart(date: Date): Date {
  const d = new Date(date);
  d.setHours(0, 0, 0, 0);
  return d;
}

export async function syncHealthConnectData(
  userId: string,
  payload: HealthConnectSyncPayload
): Promise<HealthConnectSyncResult> {
  const result: HealthConnectSyncResult = {
    weightRecordsSynced: 0,
    weightRecordsSkippedManualEdit: 0,
    trainingSessionsSynced: 0,
    trainingSessionsSkippedManualEdit: 0,
    recoveryRecordsSynced: 0,
    recoveryRecordsSkippedManualEdit: 0,
  };

  for (const record of payload.weightRecords) {
    const outcome = await upsertExternalWeightEntry({
      userId,
      source: "HEALTH_CONNECT",
      externalId: record.externalId,
      date: new Date(record.date),
      weightKg: record.weightKg,
    });
    if (outcome === "skipped_manual_edit") result.weightRecordsSkippedManualEdit++;
    else result.weightRecordsSynced++;
  }

  for (const session of payload.exerciseSessions) {
    const durationMin = Math.round(
      (new Date(session.endTime).getTime() - new Date(session.startTime).getTime()) / 60_000
    );
    const outcome = await upsertExternalTrainingSession({
      userId,
      source: "HEALTH_CONNECT",
      externalId: session.externalId,
      type: mapExerciseType(session.exerciseType),
      date: new Date(session.startTime),
      durationMin,
      intensity: PLACEHOLDER_INTENSITY,
      caloriesBurned: session.totalEnergyKcal ? Math.round(session.totalEnergyKcal) : null,
      avgHeartRate: session.avgHeartRate ? Math.round(session.avgHeartRate) : null,
      trainingLoad: computeTrainingLoad(durationMin, PLACEHOLDER_INTENSITY),
    });
    if (outcome === "skipped_manual_edit") result.trainingSessionsSkippedManualEdit++;
    else result.trainingSessionsSynced++;
  }

  for (const sleep of payload.sleepSessions) {
    const date = dayStart(new Date(sleep.date));
    const sleepHours = sleep.totalSleepMinutes / 60;

    // Health Connect has no pre-computed readiness score of its own, unlike
    // WHOOP -- these days run through our own weighted formula instead,
    // against whatever HR/HRV baseline the user's other RecoveryRecord rows
    // provide (see docs/calculations.md's readiness-score section for why
    // this asymmetry between providers is intentional).
    const baseline = await getBaseline(userId, date);
    const { score, level } = computeReadiness({ sleepHours }, baseline);

    const outcome = await upsertExternalRecoveryRecord({
      userId,
      source: "HEALTH_CONNECT",
      externalId: sleep.externalId,
      date,
      sleepHours,
      readinessScore: score,
      readinessLevel: level,
    });
    if (outcome === "skipped_manual_edit") result.recoveryRecordsSkippedManualEdit++;
    else result.recoveryRecordsSynced++;
  }

  return result;
}
