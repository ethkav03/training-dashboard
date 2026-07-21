import type { NutritionEntry } from "@prisma/client";
import type { NutritionEntryDto, NutritionSummaryDto } from "@momentum/shared";
import { prisma } from "../lib/prisma.js";

export function toNutritionEntryDto(entry: NutritionEntry): NutritionEntryDto {
  return {
    id: entry.id,
    date: entry.date.toISOString(),
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
