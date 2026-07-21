import type { WeightEntry } from "@prisma/client";
import type { WeightEntryDto, WeightTrendDto } from "@momentum/shared";
import { prisma } from "../lib/prisma.js";

export function toWeightEntryDto(entry: WeightEntry): WeightEntryDto {
  return {
    id: entry.id,
    date: entry.date.toISOString(),
    weightKg: entry.weightKg,
    note: entry.note,
    source: entry.source as WeightEntryDto["source"],
    isManuallyEdited: entry.isManuallyEdited,
  };
}

const MOVING_AVERAGE_WINDOW_DAYS = 7;

function dayKey(date: Date): string {
  return date.toISOString().slice(0, 10);
}

/**
 * Trailing N-day mean computed over the raw series on every read — raw
 * weigh-ins are never mutated or overwritten by the smoothed value.
 */
function computeMovingAverage(entries: WeightEntry[], windowDays: number) {
  const sorted = [...entries].sort((a, b) => a.date.getTime() - b.date.getTime());
  const points: { date: string; value: number }[] = [];

  for (let i = 0; i < sorted.length; i++) {
    const current = sorted[i];
    const windowStart = current.date.getTime() - windowDays * 24 * 60 * 60 * 1000;
    const windowEntries = sorted.filter(
      (e) => e.date.getTime() > windowStart && e.date.getTime() <= current.date.getTime()
    );
    const avg = windowEntries.reduce((sum, e) => sum + e.weightKg, 0) / windowEntries.length;
    points.push({ date: dayKey(current.date), value: Math.round(avg * 10) / 10 });
  }

  // Collapse same-day duplicates (last write wins for the trend line).
  const byDay = new Map<string, number>();
  for (const p of points) byDay.set(p.date, p.value);
  return Array.from(byDay.entries()).map(([date, value]) => ({ date, value }));
}

const MIN_SPAN_DAYS_FOR_RATE = 3;

/** Simple linear regression slope over the raw series, expressed per week. */
function computeRateOfChangePerWeek(entries: WeightEntry[]): number | null {
  if (entries.length < 2) return null;
  const sorted = [...entries].sort((a, b) => a.date.getTime() - b.date.getTime());
  const t0 = sorted[0].date.getTime();
  const xs = sorted.map((e) => (e.date.getTime() - t0) / (1000 * 60 * 60 * 24));
  const ys = sorted.map((e) => e.weightKg);

  // A regression over a span of hours produces a technically-real but
  // meaningless per-week extrapolation (dividing by a near-zero time delta).
  // Require a few days of spread before reporting a rate at all.
  if (xs[xs.length - 1] < MIN_SPAN_DAYS_FOR_RATE) return null;

  const n = xs.length;
  const sumX = xs.reduce((a, b) => a + b, 0);
  const sumY = ys.reduce((a, b) => a + b, 0);
  const sumXY = xs.reduce((sum, x, i) => sum + x * ys[i], 0);
  const sumXX = xs.reduce((sum, x) => sum + x * x, 0);
  const denominator = n * sumXX - sumX * sumX;
  if (denominator === 0) return 0;
  const slopePerDay = (n * sumXY - sumX * sumY) / denominator;
  return Math.round(slopePerDay * 7 * 100) / 100;
}

export async function getWeightTrend(userId: string, from?: Date, to?: Date): Promise<WeightTrendDto> {
  const entries = await prisma.weightEntry.findMany({
    where: {
      userId,
      date: {
        gte: from,
        lte: to,
      },
    },
    orderBy: { date: "asc" },
  });

  const latest = entries.at(-1) ?? null;
  const movingAverage = computeMovingAverage(entries, MOVING_AVERAGE_WINDOW_DAYS);
  const rateOfChangeKgPerWeek = computeRateOfChangePerWeek(entries);

  const activeGoal = await prisma.goal.findFirst({
    where: { userId, type: "BODY_WEIGHT", status: { not: "ACHIEVED" } },
    orderBy: { createdAt: "desc" },
  });

  return {
    raw: entries.map(toWeightEntryDto),
    movingAverage,
    rateOfChangeKgPerWeek,
    latestWeightKg: latest?.weightKg ?? null,
    goalWeightKg: activeGoal?.targetValue ?? null,
  };
}

export async function getLatestWeightSummary(userId: string) {
  const entries = await prisma.weightEntry.findMany({
    where: { userId },
    orderBy: { date: "asc" },
  });
  const movingAverage = computeMovingAverage(entries, MOVING_AVERAGE_WINDOW_DAYS);
  const activeGoal = await prisma.goal.findFirst({
    where: { userId, type: "BODY_WEIGHT", status: { not: "ACHIEVED" } },
    orderBy: { createdAt: "desc" },
  });
  return {
    latestWeightKg: entries.at(-1)?.weightKg ?? null,
    movingAverageKg: movingAverage.at(-1)?.value ?? null,
    goalWeightKg: activeGoal?.targetValue ?? null,
    rateOfChangeKgPerWeek: computeRateOfChangePerWeek(entries),
  };
}
