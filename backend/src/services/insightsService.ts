import type { InsightDto } from "@momentum/shared";
import { prisma } from "../lib/prisma.js";

const DAY_MS = 24 * 60 * 60 * 1000;

function daysAgo(n: number): Date {
  return new Date(Date.now() - n * DAY_MS);
}

function average(values: number[]): number | null {
  return values.length ? values.reduce((a, b) => a + b, 0) / values.length : null;
}

function round(value: number, decimals = 1): number {
  const factor = 10 ** decimals;
  return Math.round(value * factor) / factor;
}

async function weightTrendInsight(userId: string): Promise<InsightDto | null> {
  const entries = await prisma.weightEntry.findMany({ where: { userId, date: { gte: daysAgo(28) } } });
  const recent = entries.filter((e) => e.date >= daysAgo(7)).map((e) => e.weightKg);
  const older = entries.filter((e) => e.date < daysAgo(21) && e.date >= daysAgo(28)).map((e) => e.weightKg);

  const currentAvg = average(recent);
  const previousAvg = average(older);
  if (currentAvg == null || previousAvg == null) return null;

  const delta = currentAvg - previousAvg;
  if (Math.abs(delta) < 0.2) return null;

  const direction = delta < 0 ? "decreased" : "increased";
  return {
    id: "weight-trend",
    headline: `Your average weight has ${direction} over the last 3 weeks.`,
    detail: `7-day average is now ${round(currentAvg)} kg, vs ${round(previousAvg)} kg three weeks ago (${delta > 0 ? "+" : ""}${round(delta)} kg).`,
    trend: delta < 0 ? "down" : "up",
    metrics: { current: round(currentAvg), previous: round(previousAvg), unit: "kg", windowDays: 21 },
    generatedAt: new Date().toISOString(),
  };
}

async function trainingVolumeInsight(userId: string): Promise<InsightDto | null> {
  const sessions = await prisma.trainingSession.findMany({ where: { userId, date: { gte: daysAgo(35) } } });
  const thisWeekLoad = sessions.filter((s) => s.date >= daysAgo(7)).reduce((sum, s) => sum + s.trainingLoad, 0);
  const precedingLoad = sessions
    .filter((s) => s.date >= daysAgo(35) && s.date < daysAgo(7))
    .reduce((sum, s) => sum + s.trainingLoad, 0);
  const precedingWeeklyAvg = precedingLoad / 4;

  if (precedingWeeklyAvg === 0) return null;
  const percentChange = ((thisWeekLoad - precedingWeeklyAvg) / precedingWeeklyAvg) * 100;
  if (Math.abs(percentChange) < 15) return null;

  return {
    id: "training-volume",
    headline: `Your training volume is ${percentChange > 0 ? "higher" : "lower"} this week than your recent average.`,
    detail: `This week's total load is ${round(thisWeekLoad, 0)}, vs a ${round(precedingWeeklyAvg, 0)} average over the preceding 4 weeks (${percentChange > 0 ? "+" : ""}${round(percentChange, 0)}%).`,
    trend: percentChange > 0 ? "up" : "down",
    metrics: { current: round(thisWeekLoad, 0), previous: round(precedingWeeklyAvg, 0), unit: "load", windowDays: 7 },
    generatedAt: new Date().toISOString(),
  };
}

async function sleepBaselineInsight(userId: string): Promise<InsightDto | null> {
  const records = await prisma.recoveryRecord.findMany({
    where: { userId, date: { gte: daysAgo(30) }, sleepHours: { not: null } },
  });
  const recent = records.filter((r) => r.date >= daysAgo(7)).map((r) => r.sleepHours!);
  const baseline = records.map((r) => r.sleepHours!);

  const currentAvg = average(recent);
  const baselineAvg = average(baseline);
  if (currentAvg == null || baselineAvg == null || records.filter((r) => r.date < daysAgo(7)).length === 0) return null;

  const deviationMinutes = (currentAvg - baselineAvg) * 60;
  if (Math.abs(deviationMinutes) < 30) return null;

  return {
    id: "sleep-baseline",
    headline: `Your average sleep has ${deviationMinutes < 0 ? "fallen below" : "risen above"} your recent baseline.`,
    detail: `7-day average is ${round(currentAvg)}h, vs a ${round(baselineAvg)}h 30-day baseline (${deviationMinutes > 0 ? "+" : ""}${round(deviationMinutes, 0)} min).`,
    trend: deviationMinutes < 0 ? "down" : "up",
    metrics: { current: round(currentAvg), previous: round(baselineAvg), unit: "hours", windowDays: 7 },
    generatedAt: new Date().toISOString(),
  };
}

async function nutritionTargetInsight(
  userId: string,
  goalType: "CALORIE_INTAKE" | "PROTEIN_INTAKE",
  label: string,
  unit: string,
  field: "calories" | "proteinG"
): Promise<InsightDto | null> {
  const goal = await prisma.goal.findFirst({
    where: { userId, type: goalType, status: { not: "ACHIEVED" } },
    orderBy: { createdAt: "desc" },
  });
  if (!goal?.targetValue) return null;

  const entries = await prisma.nutritionEntry.findMany({ where: { userId, date: { gte: daysAgo(7) } } });
  if (!entries.length) return null;

  const totalByDay = new Map<string, number>();
  for (const e of entries) {
    const key = e.date.toISOString().slice(0, 10);
    const value = field === "calories" ? e.calories : e.proteinG ?? 0;
    totalByDay.set(key, (totalByDay.get(key) ?? 0) + value);
  }
  const weeklyAvg = average(Array.from(totalByDay.values()));
  if (weeklyAvg == null) return null;

  const percentDiff = ((weeklyAvg - goal.targetValue) / goal.targetValue) * 100;
  if (Math.abs(percentDiff) < 5) return null;

  return {
    id: `${goalType.toLowerCase().replace("_", "-")}-vs-target`,
    headline: `Your ${label} has been ${percentDiff > 0 ? "above" : "below"} target on average this week.`,
    detail: `7-day average is ${round(weeklyAvg, 0)} ${unit}, vs a ${round(goal.targetValue, 0)} ${unit} target (${percentDiff > 0 ? "+" : ""}${round(percentDiff, 0)}%).`,
    trend: percentDiff > 0 ? "up" : "down",
    metrics: { current: round(weeklyAvg, 0), previous: round(goal.targetValue, 0), unit, windowDays: 7 },
    generatedAt: new Date().toISOString(),
  };
}

async function readinessTrendInsight(userId: string): Promise<InsightDto | null> {
  const records = await prisma.recoveryRecord.findMany({
    where: { userId, date: { gte: daysAgo(3) } },
    orderBy: { date: "desc" },
  });
  if (records.length < 2) return null;

  const avgScore = average(records.map((r) => r.readinessScore));
  if (avgScore == null || avgScore >= 50) return null;

  return {
    id: "readiness-low",
    headline: "Your readiness has been low over the last few days.",
    detail: `Average readiness score over the last ${records.length} logged days is ${round(avgScore, 0)}, below the "moderate" threshold of 50.`,
    trend: "down",
    metrics: { current: round(avgScore, 0), previous: 50, unit: "score", windowDays: 3 },
    generatedAt: new Date().toISOString(),
  };
}

export async function getAllInsights(userId: string): Promise<InsightDto[]> {
  const results = await Promise.all([
    weightTrendInsight(userId),
    trainingVolumeInsight(userId),
    sleepBaselineInsight(userId),
    nutritionTargetInsight(userId, "CALORIE_INTAKE", "calorie intake", "kcal", "calories"),
    nutritionTargetInsight(userId, "PROTEIN_INTAKE", "protein intake", "g", "proteinG"),
    readinessTrendInsight(userId),
  ]);
  return results.filter((i): i is InsightDto => i !== null);
}

export async function getTopInsights(userId: string, limit: number): Promise<InsightDto[]> {
  const all = await getAllInsights(userId);
  return all.slice(0, limit);
}
