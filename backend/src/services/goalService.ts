import type { Goal } from "@prisma/client";
import type { GoalDto, GoalStatus } from "@momentum/shared";
import { prisma } from "../lib/prisma.js";
import { estimateOneRepMax } from "./trainingService.js";

const DAY_MS = 24 * 60 * 60 * 1000;

interface GoalMetricParams {
  type: string;
  relatedExerciseName?: string | null;
}

export async function computeCurrentValue(userId: string, goal: GoalMetricParams): Promise<number | null> {
  const sevenDaysAgo = new Date(Date.now() - 7 * DAY_MS);

  switch (goal.type) {
    case "BODY_WEIGHT": {
      const latest = await prisma.weightEntry.findFirst({ where: { userId }, orderBy: { date: "desc" } });
      return latest?.weightKg ?? null;
    }
    case "CALORIE_INTAKE": {
      const entries = await prisma.nutritionEntry.findMany({ where: { userId, date: { gte: sevenDaysAgo } } });
      if (!entries.length) return null;
      const totalByDay = new Map<string, number>();
      for (const e of entries) {
        const key = e.date.toISOString().slice(0, 10);
        totalByDay.set(key, (totalByDay.get(key) ?? 0) + e.calories);
      }
      const values = Array.from(totalByDay.values());
      return values.reduce((a, b) => a + b, 0) / values.length;
    }
    case "PROTEIN_INTAKE": {
      const entries = await prisma.nutritionEntry.findMany({ where: { userId, date: { gte: sevenDaysAgo } } });
      if (!entries.length) return null;
      const totalByDay = new Map<string, number>();
      for (const e of entries) {
        const key = e.date.toISOString().slice(0, 10);
        totalByDay.set(key, (totalByDay.get(key) ?? 0) + (e.proteinG ?? 0));
      }
      const values = Array.from(totalByDay.values());
      return values.reduce((a, b) => a + b, 0) / values.length;
    }
    case "TRAINING_FREQUENCY": {
      return prisma.trainingSession.count({ where: { userId, date: { gte: sevenDaysAgo } } });
    }
    case "EXERCISE_PERFORMANCE": {
      if (!goal.relatedExerciseName) return null;
      const sets = await prisma.workoutSet.findMany({
        where: {
          isWarmup: false,
          workoutExercise: { name: goal.relatedExerciseName, workout: { trainingSession: { userId } } },
        },
      });
      if (!sets.length) return null;
      return Math.max(...sets.map((s) => estimateOneRepMax(s.weightKg, s.reps)));
    }
    case "SPORT_PERFORMANCE": {
      const matches = await prisma.matchDetail.findMany({
        where: { trainingSession: { userId }, performanceRating: { not: null } },
        include: { trainingSession: true },
        orderBy: { trainingSession: { date: "desc" } },
        take: 5,
      });
      if (!matches.length) return null;
      return matches.reduce((sum, m) => sum + (m.performanceRating ?? 0), 0) / matches.length;
    }
    case "SLEEP_RECOVERY": {
      const records = await prisma.recoveryRecord.findMany({
        where: { userId, date: { gte: sevenDaysAgo }, sleepHours: { not: null } },
      });
      if (!records.length) return null;
      return records.reduce((sum, r) => sum + (r.sleepHours ?? 0), 0) / records.length;
    }
    case "CUSTOM":
    default:
      return null;
  }
}

function computeProgressPercent(goal: Goal, currentValue: number | null): number | null {
  if (currentValue == null || goal.targetValue == null || goal.startValue == null) return null;
  const range = goal.direction === "DECREASE" ? goal.startValue - goal.targetValue : goal.targetValue - goal.startValue;
  if (range === 0) return 100;
  const progressed =
    goal.direction === "DECREASE" ? goal.startValue - currentValue : currentValue - goal.startValue;
  return Math.round((progressed / range) * 1000) / 10;
}

function computeStatus(goal: Goal, progressPercent: number | null): { status: GoalStatus; achievedNow: boolean } {
  if (goal.status === "PAUSED") return { status: "PAUSED", achievedNow: false };
  if (progressPercent == null) return { status: goal.status as GoalStatus, achievedNow: false };
  if (progressPercent >= 100) return { status: "ACHIEVED", achievedNow: goal.achievedAt == null };

  if (goal.targetDate) {
    const totalMs = goal.targetDate.getTime() - goal.startDate.getTime();
    const elapsedMs = Date.now() - goal.startDate.getTime();
    const expectedPercent = totalMs > 0 ? Math.min(100, (elapsedMs / totalMs) * 100) : 100;
    return { status: progressPercent >= expectedPercent - 10 ? "ON_TRACK" : "AT_RISK", achievedNow: false };
  }

  return { status: progressPercent >= 70 ? "ON_TRACK" : "AT_RISK", achievedNow: false };
}

export async function refreshGoal(userId: string, goal: Goal): Promise<GoalDto> {
  const currentValue = await computeCurrentValue(userId, goal);
  const progressPercent = computeProgressPercent(goal, currentValue);
  const { status, achievedNow } = computeStatus(goal, progressPercent);

  let updated = goal;
  if (status !== goal.status || achievedNow) {
    updated = await prisma.goal.update({
      where: { id: goal.id },
      data: { status, achievedAt: achievedNow ? new Date() : goal.achievedAt },
    });
  }

  return {
    id: updated.id,
    type: updated.type as GoalDto["type"],
    title: updated.title,
    targetValue: updated.targetValue,
    targetUnit: updated.targetUnit,
    direction: updated.direction as GoalDto["direction"],
    startValue: updated.startValue,
    currentValue,
    progressPercent,
    startDate: updated.startDate.toISOString(),
    targetDate: updated.targetDate?.toISOString() ?? null,
    status: updated.status as GoalDto["status"],
    relatedExerciseName: updated.relatedExerciseName,
    notes: updated.notes,
    achievedAt: updated.achievedAt?.toISOString() ?? null,
  };
}

export async function listGoals(userId: string, filters: { status?: string; type?: string }): Promise<GoalDto[]> {
  const goals = await prisma.goal.findMany({
    where: { userId, status: filters.status as never, type: filters.type as never },
    orderBy: { createdAt: "desc" },
  });
  return Promise.all(goals.map((g) => refreshGoal(userId, g)));
}

export async function getGoal(userId: string, id: string): Promise<GoalDto | null> {
  const goal = await prisma.goal.findFirst({ where: { id, userId } });
  if (!goal) return null;
  return refreshGoal(userId, goal);
}

interface CreateGoalInput {
  type: string;
  title: string;
  targetValue?: number;
  targetUnit?: string;
  direction: string;
  targetDate?: string;
  relatedExerciseName?: string;
  notes?: string;
}

export async function createGoal(userId: string, input: CreateGoalInput): Promise<GoalDto> {
  // Snapshot the current metric as the starting point so progress-percent
  // math has a baseline — a goal created today always starts at 0% progress.
  const startValue = await computeCurrentValue(userId, {
    type: input.type,
    relatedExerciseName: input.relatedExerciseName,
  });

  const goal = await prisma.goal.create({
    data: {
      userId,
      type: input.type as never,
      title: input.title,
      targetValue: input.targetValue,
      targetUnit: input.targetUnit,
      direction: input.direction as never,
      startValue,
      startDate: new Date(),
      targetDate: input.targetDate ? new Date(input.targetDate) : undefined,
      relatedExerciseName: input.relatedExerciseName,
      notes: input.notes,
    },
  });
  return refreshGoal(userId, goal);
}

interface UpdateGoalInput {
  title?: string;
  targetValue?: number;
  targetUnit?: string;
  targetDate?: string | null;
  notes?: string;
  status?: "ON_TRACK" | "PAUSED";
}

export async function updateGoal(userId: string, id: string, input: UpdateGoalInput): Promise<GoalDto | null> {
  const existing = await prisma.goal.findFirst({ where: { id, userId } });
  if (!existing) return null;

  const goal = await prisma.goal.update({
    where: { id },
    data: {
      title: input.title,
      targetValue: input.targetValue,
      targetUnit: input.targetUnit,
      targetDate: input.targetDate === undefined ? undefined : input.targetDate ? new Date(input.targetDate) : null,
      notes: input.notes,
      status: input.status,
      pausedAt: input.status === "PAUSED" ? new Date() : input.status === "ON_TRACK" ? null : undefined,
    },
  });
  return refreshGoal(userId, goal);
}

export async function deleteGoal(userId: string, id: string): Promise<boolean> {
  const existing = await prisma.goal.findFirst({ where: { id, userId } });
  if (!existing) return false;
  await prisma.goal.delete({ where: { id } });
  return true;
}
