import type { RecoveryRecord } from "@prisma/client";
import type { RecoveryRecordDto, ReadinessLevel } from "@momentum/shared";
import { prisma } from "../lib/prisma.js";

interface ReadinessInputs {
  sleepHours?: number | null;
  sleepQuality?: number | null;
  restingHr?: number | null;
  hrv?: number | null;
  soreness?: number | null;
  energy?: number | null;
}

const RECOMMENDATIONS: Record<ReadinessLevel, string> = {
  HIGH: "Good recovery and manageable load — normal or challenging training is appropriate today.",
  MODERATE: "Some fatigue or mixed signals — train, but consider reducing intensity or volume.",
  LOW: "Poor recovery or accumulated fatigue — prioritise recovery and be cautious with high intensity.",
};

function clamp(value: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, value));
}

export function scoreToReadinessLevel(score: number): ReadinessLevel {
  return score >= 75 ? "HIGH" : score >= 50 ? "MODERATE" : "LOW";
}

/**
 * Weighted 0-100 readiness score from whichever components have data today,
 * renormalized over just those components so a missing metric (e.g. no HRV
 * device) doesn't silently drag the score down.
 */
export function computeReadiness(
  inputs: ReadinessInputs,
  baseline: { avgRestingHr: number | null; avgHrv: number | null }
): { score: number; level: ReadinessLevel; recommendation: string } {
  const components: { value: number; weight: number }[] = [];

  if (inputs.sleepHours != null) {
    components.push({ value: clamp((inputs.sleepHours / 8) * 100, 0, 100), weight: 0.25 });
  }
  if (inputs.sleepQuality != null) {
    components.push({ value: clamp((inputs.sleepQuality / 5) * 100, 0, 100), weight: 0.15 });
  }
  if (inputs.restingHr != null && baseline.avgRestingHr != null) {
    const diff = inputs.restingHr - baseline.avgRestingHr;
    components.push({ value: clamp(100 - Math.max(0, diff) * 5, 0, 100), weight: 0.15 });
  }
  if (inputs.hrv != null && baseline.avgHrv != null && baseline.avgHrv > 0) {
    const ratio = inputs.hrv / baseline.avgHrv;
    components.push({ value: clamp(ratio * 100, 0, 100), weight: 0.15 });
  }
  if (inputs.soreness != null) {
    components.push({ value: clamp(((6 - inputs.soreness) / 5) * 100, 0, 100), weight: 0.15 });
  }
  if (inputs.energy != null) {
    components.push({ value: clamp((inputs.energy / 5) * 100, 0, 100), weight: 0.15 });
  }

  const totalWeight = components.reduce((sum, c) => sum + c.weight, 0);
  const score =
    totalWeight > 0
      ? Math.round(components.reduce((sum, c) => sum + c.value * c.weight, 0) / totalWeight)
      : 50;

  const level = scoreToReadinessLevel(score);

  return { score, level, recommendation: RECOMMENDATIONS[level] };
}

export function toRecoveryRecordDto(record: RecoveryRecord): RecoveryRecordDto {
  const level = record.readinessLevel as ReadinessLevel;
  return {
    id: record.id,
    date: record.date.toISOString(),
    sleepHours: record.sleepHours,
    sleepQuality: record.sleepQuality,
    restingHr: record.restingHr,
    hrv: record.hrv,
    soreness: record.soreness,
    energy: record.energy,
    readinessScore: record.readinessScore,
    readinessLevel: level,
    recommendation: RECOMMENDATIONS[level],
    notes: record.notes,
    source: record.source as RecoveryRecordDto["source"],
  };
}

export async function getBaseline(userId: string, excludeDate: Date) {
  const thirtyDaysAgo = new Date(excludeDate.getTime() - 30 * 24 * 60 * 60 * 1000);
  const records = await prisma.recoveryRecord.findMany({
    where: {
      userId,
      date: { gte: thirtyDaysAgo, lt: excludeDate },
    },
  });
  const withHr = records.filter((r) => r.restingHr != null);
  const withHrv = records.filter((r) => r.hrv != null);
  return {
    avgRestingHr: withHr.length ? withHr.reduce((s, r) => s + r.restingHr!, 0) / withHr.length : null,
    avgHrv: withHrv.length ? withHrv.reduce((s, r) => s + r.hrv!, 0) / withHrv.length : null,
  };
}

function dayStart(date: Date): Date {
  const d = new Date(date);
  d.setHours(0, 0, 0, 0);
  return d;
}

export async function upsertRecoveryRecord(
  userId: string,
  date: Date,
  inputs: ReadinessInputs & { notes?: string }
): Promise<RecoveryRecord> {
  const day = dayStart(date);
  const baseline = await getBaseline(userId, day);
  const { score, level } = computeReadiness(inputs, baseline);

  return prisma.recoveryRecord.upsert({
    where: { userId_date: { userId, date: day } },
    update: {
      sleepHours: inputs.sleepHours,
      sleepQuality: inputs.sleepQuality,
      restingHr: inputs.restingHr,
      hrv: inputs.hrv,
      soreness: inputs.soreness,
      energy: inputs.energy,
      notes: inputs.notes,
      readinessScore: score,
      readinessLevel: level,
      isManuallyEdited: true,
    },
    create: {
      userId,
      date: day,
      sleepHours: inputs.sleepHours,
      sleepQuality: inputs.sleepQuality,
      restingHr: inputs.restingHr,
      hrv: inputs.hrv,
      soreness: inputs.soreness,
      energy: inputs.energy,
      notes: inputs.notes,
      readinessScore: score,
      readinessLevel: level,
      source: "MANUAL",
    },
  });
}

export async function getRecoveryToday(userId: string): Promise<RecoveryRecord | null> {
  return prisma.recoveryRecord.findUnique({ where: { userId_date: { userId, date: dayStart(new Date()) } } });
}

export async function listRecoveryRecords(userId: string, from?: Date, to?: Date): Promise<RecoveryRecord[]> {
  return prisma.recoveryRecord.findMany({
    where: { userId, date: { gte: from, lte: to } },
    orderBy: { date: "desc" },
  });
}
