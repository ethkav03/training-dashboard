import type { ActivityType, DataSource, ReadinessLevel } from "@momentum/shared";
import { prisma } from "../lib/prisma.js";

export type ExternalSyncResult = "created" | "updated" | "skipped_manual_edit";

/**
 * A sync must never silently overwrite a row the user has hand-edited.
 * Every external-sync upsert in this file follows the same shape: look up
 * by the entity's dedup key, skip if `isManuallyEdited`, otherwise
 * create/update -- implemented once here so WHOOP and Health Connect (and
 * any future provider) share exactly one conflict-resolution rule.
 */

interface WeightEntryInput {
  userId: string;
  source: DataSource;
  externalId: string;
  date: Date;
  weightKg: number;
}

export async function upsertExternalWeightEntry(input: WeightEntryInput): Promise<ExternalSyncResult> {
  const existing = await prisma.weightEntry.findUnique({
    where: { userId_source_externalId: { userId: input.userId, source: input.source, externalId: input.externalId } },
  });
  if (existing) {
    if (existing.isManuallyEdited) return "skipped_manual_edit";
    await prisma.weightEntry.update({
      where: { id: existing.id },
      data: { date: input.date, weightKg: input.weightKg },
    });
    return "updated";
  }
  await prisma.weightEntry.create({
    data: {
      userId: input.userId,
      source: input.source,
      externalId: input.externalId,
      date: input.date,
      weightKg: input.weightKg,
    },
  });
  return "created";
}

interface TrainingSessionInput {
  userId: string;
  source: DataSource;
  externalId: string;
  type: ActivityType;
  date: Date;
  durationMin: number;
  intensity: number;
  caloriesBurned?: number | null;
  avgHeartRate?: number | null;
  trainingLoad: number;
}

export async function upsertExternalTrainingSession(input: TrainingSessionInput): Promise<ExternalSyncResult> {
  const existing = await prisma.trainingSession.findUnique({
    where: { userId_source_externalId: { userId: input.userId, source: input.source, externalId: input.externalId } },
  });
  if (existing) {
    if (existing.isManuallyEdited) return "skipped_manual_edit";
    await prisma.trainingSession.update({
      where: { id: existing.id },
      data: {
        type: input.type as never,
        date: input.date,
        durationMin: input.durationMin,
        intensity: input.intensity,
        caloriesBurned: input.caloriesBurned,
        avgHeartRate: input.avgHeartRate,
        trainingLoad: input.trainingLoad,
      },
    });
    return "updated";
  }
  await prisma.trainingSession.create({
    data: {
      userId: input.userId,
      source: input.source,
      externalId: input.externalId,
      type: input.type as never,
      date: input.date,
      durationMin: input.durationMin,
      intensity: input.intensity,
      caloriesBurned: input.caloriesBurned,
      avgHeartRate: input.avgHeartRate,
      trainingLoad: input.trainingLoad,
    },
  });
  return "created";
}

interface RecoveryRecordInput {
  userId: string;
  source: DataSource;
  externalId?: string | null;
  date: Date; // must already be day-truncated (00:00:00)
  sleepHours?: number | null;
  sleepQuality?: number | null;
  restingHr?: number | null;
  hrv?: number | null;
  soreness?: number | null;
  energy?: number | null;
  sleepScore?: number | null;
  strain?: number | null;
  readinessScore: number;
  readinessLevel: ReadinessLevel;
}

export async function upsertExternalRecoveryRecord(input: RecoveryRecordInput): Promise<ExternalSyncResult> {
  const existing = await prisma.recoveryRecord.findUnique({
    where: { userId_date: { userId: input.userId, date: input.date } },
  });
  if (existing) {
    if (existing.isManuallyEdited) return "skipped_manual_edit";
    await prisma.recoveryRecord.update({
      where: { id: existing.id },
      data: {
        source: input.source,
        externalId: input.externalId,
        sleepHours: input.sleepHours,
        sleepQuality: input.sleepQuality,
        restingHr: input.restingHr,
        hrv: input.hrv,
        soreness: input.soreness,
        energy: input.energy,
        sleepScore: input.sleepScore,
        strain: input.strain,
        readinessScore: input.readinessScore,
        readinessLevel: input.readinessLevel as never,
      },
    });
    return "updated";
  }
  await prisma.recoveryRecord.create({
    data: {
      userId: input.userId,
      source: input.source,
      externalId: input.externalId,
      date: input.date,
      sleepHours: input.sleepHours,
      sleepQuality: input.sleepQuality,
      restingHr: input.restingHr,
      hrv: input.hrv,
      soreness: input.soreness,
      energy: input.energy,
      sleepScore: input.sleepScore,
      strain: input.strain,
      readinessScore: input.readinessScore,
      readinessLevel: input.readinessLevel as never,
    },
  });
  return "created";
}
