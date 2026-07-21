import type { NutritionEntry } from "@prisma/client";
import type { EnergyBalanceGranularity, EnergyBalancePointDto, NutritionEntryDto, NutritionSummaryDto } from "@momentum/shared";
import { prisma } from "../lib/prisma.js";

export function toNutritionEntryDto(entry: NutritionEntry): NutritionEntryDto {
  return {
    id: entry.id,
    date: entry.date.toISOString(),
    mealType: entry.mealType as NutritionEntryDto["mealType"],
    mealName: entry.mealName,
    calories: entry.calories,
    proteinG: entry.proteinG,
    carbsG: entry.carbsG,
    fatG: entry.fatG,
    notes: entry.notes,
    source: entry.source as NutritionEntryDto["source"],
    isManuallyEdited: entry.isManuallyEdited,
  };
}

function dayBounds(date: Date): { start: Date; end: Date } {
  const start = new Date(date);
  start.setHours(0, 0, 0, 0);
  const end = new Date(start);
  end.setDate(end.getDate() + 1);
  return { start, end };
}

export async function getNutritionSummaryForDate(userId: string, date: Date): Promise<NutritionSummaryDto> {
  const { start, end } = dayBounds(date);

  const [entries, trainingSessions, calorieGoal, proteinGoal, user] = await Promise.all([
    prisma.nutritionEntry.findMany({ where: { userId, date: { gte: start, lt: end } } }),
    prisma.trainingSession.findMany({ where: { userId, date: { gte: start, lt: end } } }),
    prisma.goal.findFirst({ where: { userId, type: "CALORIE_INTAKE", status: { not: "ACHIEVED" } }, orderBy: { createdAt: "desc" } }),
    prisma.goal.findFirst({ where: { userId, type: "PROTEIN_INTAKE", status: { not: "ACHIEVED" } }, orderBy: { createdAt: "desc" } }),
    prisma.user.findUniqueOrThrow({ where: { id: userId } }),
  ]);

  const totalCalories = entries.reduce((sum, e) => sum + e.calories, 0);
  const totalProteinG = entries.reduce((sum, e) => sum + (e.proteinG ?? 0), 0);
  const totalCarbsG = entries.reduce((sum, e) => sum + (e.carbsG ?? 0), 0);
  const totalFatG = entries.reduce((sum, e) => sum + (e.fatG ?? 0), 0);

  const baselineKcal = user.estimatedDailyBurnKcal ?? 0;
  const trainingKcal = trainingSessions.reduce((sum, s) => sum + (s.caloriesBurned ?? 0), 0);
  const totalBurnKcal = baselineKcal + trainingKcal;

  return {
    date: start.toISOString().slice(0, 10),
    totalCalories,
    totalProteinG: Math.round(totalProteinG * 10) / 10,
    totalCarbsG: Math.round(totalCarbsG * 10) / 10,
    totalFatG: Math.round(totalFatG * 10) / 10,
    targetCalories: calorieGoal?.targetValue ?? null,
    targetProteinG: proteinGoal?.targetValue ?? null,
    estimatedBurn: {
      baselineKcal,
      trainingKcal,
      totalKcal: totalBurnKcal,
      isEstimate: true,
    },
    balanceKcal: baselineKcal > 0 ? totalCalories - totalBurnKcal : null,
  };
}

export async function getNutritionSummaryRange(userId: string, from: Date, to: Date): Promise<NutritionSummaryDto[]> {
  const days: Date[] = [];
  const cursor = new Date(from);
  while (cursor <= to) {
    days.push(new Date(cursor));
    cursor.setDate(cursor.getDate() + 1);
  }
  return Promise.all(days.map((d) => getNutritionSummaryForDate(userId, d)));
}

function bucketKey(date: Date, granularity: EnergyBalanceGranularity): string {
  if (granularity === "day") return date.toISOString().slice(0, 10);
  if (granularity === "week") {
    // Monday-start week, keyed by that Monday's date.
    const dayOfWeek = date.getDay(); // 0=Sun..6=Sat
    const diffToMonday = (dayOfWeek + 6) % 7;
    const monday = new Date(date);
    monday.setDate(date.getDate() - diffToMonday);
    return monday.toISOString().slice(0, 10);
  }
  if (granularity === "month") {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
  }
  return String(date.getFullYear());
}

function rangeForGranularity(granularity: EnergyBalanceGranularity): { from: Date; to: Date } {
  const to = new Date();
  to.setHours(23, 59, 59, 999);
  const from = new Date();
  if (granularity === "day") from.setDate(from.getDate() - 29); // last 30 days
  else if (granularity === "week") from.setDate(from.getDate() - 7 * 11); // last 12 weeks
  else if (granularity === "month") from.setMonth(from.getMonth() - 11); // last 12 months
  else from.setFullYear(from.getFullYear() - 4); // last 5 years
  from.setHours(0, 0, 0, 0);
  return { from, to };
}

/**
 * Single pair of range queries regardless of granularity or how far back it
 * spans -- unlike getNutritionSummaryRange (which calls the per-day summary
 * function in a loop), this walks the raw rows once and buckets them in
 * memory, so a "last 5 years" request isn't hundreds of separate queries.
 */
export async function getEnergyBalanceSeries(
  userId: string,
  granularity: EnergyBalanceGranularity
): Promise<EnergyBalancePointDto[]> {
  const { from, to } = rangeForGranularity(granularity);

  const [entries, sessions, user] = await Promise.all([
    prisma.nutritionEntry.findMany({
      where: { userId, date: { gte: from, lte: to } },
      select: { date: true, calories: true },
    }),
    prisma.trainingSession.findMany({
      where: { userId, date: { gte: from, lte: to } },
      select: { date: true, caloriesBurned: true },
    }),
    prisma.user.findUniqueOrThrow({ where: { id: userId }, select: { estimatedDailyBurnKcal: true } }),
  ]);

  const baselineKcal = user.estimatedDailyBurnKcal ?? 0;

  // Walk every calendar day in range so the baseline burn is counted even on
  // days with no logged food -- a week/month's total burn shouldn't quietly
  // exclude days you just didn't log a meal on.
  const buckets = new Map<string, { totalCalories: number; totalBurnKcal: number }>();
  const cursor = new Date(from);
  while (cursor <= to) {
    const key = bucketKey(cursor, granularity);
    if (!buckets.has(key)) buckets.set(key, { totalCalories: 0, totalBurnKcal: 0 });
    buckets.get(key)!.totalBurnKcal += baselineKcal;
    cursor.setDate(cursor.getDate() + 1);
  }

  for (const entry of entries) {
    const bucket = buckets.get(bucketKey(entry.date, granularity));
    if (bucket) bucket.totalCalories += entry.calories;
  }
  for (const session of sessions) {
    const bucket = buckets.get(bucketKey(session.date, granularity));
    if (bucket && session.caloriesBurned) bucket.totalBurnKcal += session.caloriesBurned;
  }

  return Array.from(buckets.entries())
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([period, totals]) => ({
      period,
      totalCalories: totals.totalCalories,
      totalBurnKcal: Math.round(totals.totalBurnKcal),
      balanceKcal: baselineKcal > 0 ? totals.totalCalories - Math.round(totals.totalBurnKcal) : null,
    }));
}
