import type { MatchDetail, TrainingSession, Workout, WorkoutExercise, WorkoutSet } from "@prisma/client";
import type {
  ExerciseProgressionPointDto,
  MatchDetailDto,
  TrainingSessionDto,
  TrainingSessionWriteDto,
  WorkoutExerciseDto,
} from "@momentum/shared";
import { prisma } from "../lib/prisma.js";

type FullSession = TrainingSession & {
  workout: (Workout & { exercises: (WorkoutExercise & { sets: WorkoutSet[] })[] }) | null;
  matchDetail: MatchDetail | null;
};

export function computeTrainingLoad(durationMin: number, intensity: number): number {
  return durationMin * intensity;
}

/** Epley formula: a simple, explainable estimated one-rep max. */
export function estimateOneRepMax(weightKg: number, reps: number): number {
  return Math.round(weightKg * (1 + reps / 30) * 10) / 10;
}

function toWorkoutExerciseDto(exercise: WorkoutExercise & { sets: WorkoutSet[] }): WorkoutExerciseDto {
  return {
    id: exercise.id,
    name: exercise.name,
    orderIndex: exercise.orderIndex,
    sets: exercise.sets
      .sort((a, b) => a.setNumber - b.setNumber)
      .map((s) => ({
        id: s.id,
        setNumber: s.setNumber,
        reps: s.reps,
        weightKg: s.weightKg,
        rpe: s.rpe,
        isWarmup: s.isWarmup,
      })),
  };
}

function toMatchDetailDto(detail: MatchDetail): MatchDetailDto {
  return {
    opponent: detail.opponent,
    competition: detail.competition,
    position: detail.position,
    minutesPlayed: detail.minutesPlayed,
    result: detail.result as MatchDetailDto["result"],
    performanceRating: detail.performanceRating,
    keyStats: (detail.keyStats as Record<string, number> | null) ?? null,
    injuryNotes: detail.injuryNotes,
    reflection: detail.reflection,
  };
}

export function toTrainingSessionDto(session: FullSession): TrainingSessionDto {
  return {
    id: session.id,
    type: session.type as TrainingSessionDto["type"],
    date: session.date.toISOString(),
    durationMin: session.durationMin,
    intensity: session.intensity,
    caloriesBurned: session.caloriesBurned,
    trainingLoad: session.trainingLoad,
    notes: session.notes,
    source: session.source as TrainingSessionDto["source"],
    isManuallyEdited: session.isManuallyEdited,
    workout: session.workout
      ? { exercises: session.workout.exercises.map(toWorkoutExerciseDto) }
      : null,
    matchDetail: session.matchDetail ? toMatchDetailDto(session.matchDetail) : null,
  };
}

const fullInclude = {
  workout: { include: { exercises: { include: { sets: true } } } },
  matchDetail: true,
} as const;

export async function createTrainingSession(userId: string, input: TrainingSessionWriteDto): Promise<FullSession> {
  const trainingLoad = computeTrainingLoad(input.durationMin, input.intensity);

  return prisma.$transaction(async (tx) => {
    const session = await tx.trainingSession.create({
      data: {
        userId,
        type: input.type as never,
        date: new Date(input.date),
        durationMin: input.durationMin,
        intensity: input.intensity,
        caloriesBurned: input.caloriesBurned ?? undefined,
        notes: input.notes ?? undefined,
        trainingLoad,
        source: "MANUAL",
      },
    });

    if (input.workout) {
      const workout = await tx.workout.create({ data: { trainingSessionId: session.id } });
      for (const exercise of input.workout.exercises) {
        await tx.workoutExercise.create({
          data: {
            workoutId: workout.id,
            name: exercise.name,
            orderIndex: exercise.orderIndex,
            sets: {
              create: exercise.sets.map((s) => ({
                setNumber: s.setNumber,
                reps: s.reps,
                weightKg: s.weightKg,
                rpe: s.rpe ?? undefined,
                isWarmup: s.isWarmup,
              })),
            },
          },
        });
      }
    }

    if (input.matchDetail) {
      await tx.matchDetail.create({
        data: {
          trainingSessionId: session.id,
          opponent: input.matchDetail.opponent ?? undefined,
          competition: input.matchDetail.competition ?? undefined,
          position: input.matchDetail.position ?? undefined,
          minutesPlayed: input.matchDetail.minutesPlayed ?? undefined,
          result: (input.matchDetail.result as never) ?? undefined,
          performanceRating: input.matchDetail.performanceRating ?? undefined,
          keyStats: input.matchDetail.keyStats ?? undefined,
          injuryNotes: input.matchDetail.injuryNotes ?? undefined,
          reflection: input.matchDetail.reflection ?? undefined,
        },
      });
    }

    return tx.trainingSession.findUniqueOrThrow({ where: { id: session.id }, include: fullInclude });
  });
}

export async function replaceTrainingSession(
  userId: string,
  id: string,
  input: TrainingSessionWriteDto
): Promise<FullSession | null> {
  const existing = await prisma.trainingSession.findFirst({ where: { id, userId } });
  if (!existing) return null;

  const trainingLoad = computeTrainingLoad(input.durationMin, input.intensity);

  return prisma.$transaction(async (tx) => {
    await tx.workout.deleteMany({ where: { trainingSessionId: id } });
    await tx.matchDetail.deleteMany({ where: { trainingSessionId: id } });

    await tx.trainingSession.update({
      where: { id },
      data: {
        type: input.type as never,
        date: new Date(input.date),
        durationMin: input.durationMin,
        intensity: input.intensity,
        caloriesBurned: input.caloriesBurned ?? null,
        notes: input.notes ?? null,
        trainingLoad,
        isManuallyEdited: true,
      },
    });

    if (input.workout) {
      const workout = await tx.workout.create({ data: { trainingSessionId: id } });
      for (const exercise of input.workout.exercises) {
        await tx.workoutExercise.create({
          data: {
            workoutId: workout.id,
            name: exercise.name,
            orderIndex: exercise.orderIndex,
            sets: {
              create: exercise.sets.map((s) => ({
                setNumber: s.setNumber,
                reps: s.reps,
                weightKg: s.weightKg,
                rpe: s.rpe ?? undefined,
                isWarmup: s.isWarmup,
              })),
            },
          },
        });
      }
    }

    if (input.matchDetail) {
      await tx.matchDetail.create({
        data: {
          trainingSessionId: id,
          opponent: input.matchDetail.opponent ?? undefined,
          competition: input.matchDetail.competition ?? undefined,
          position: input.matchDetail.position ?? undefined,
          minutesPlayed: input.matchDetail.minutesPlayed ?? undefined,
          result: (input.matchDetail.result as never) ?? undefined,
          performanceRating: input.matchDetail.performanceRating ?? undefined,
          keyStats: input.matchDetail.keyStats ?? undefined,
          injuryNotes: input.matchDetail.injuryNotes ?? undefined,
          reflection: input.matchDetail.reflection ?? undefined,
        },
      });
    }

    return tx.trainingSession.findUniqueOrThrow({ where: { id }, include: fullInclude });
  });
}

export async function findSessionWithDetail(userId: string, id: string): Promise<FullSession | null> {
  return prisma.trainingSession.findFirst({ where: { id, userId }, include: fullInclude });
}

export async function listTrainingSessions(
  userId: string,
  filters: { from?: Date; to?: Date; type?: string }
): Promise<FullSession[]> {
  return prisma.trainingSession.findMany({
    where: {
      userId,
      date: { gte: filters.from, lte: filters.to },
      type: filters.type as never,
    },
    include: fullInclude,
    orderBy: { date: "desc" },
  });
}

export async function listDistinctExerciseNames(userId: string): Promise<string[]> {
  const rows = await prisma.workoutExercise.findMany({
    where: { workout: { trainingSession: { userId } } },
    select: { name: true },
    distinct: ["name"],
  });
  return rows.map((r) => r.name).sort((a, b) => a.localeCompare(b));
}

export async function getExerciseProgression(
  userId: string,
  exerciseName: string
): Promise<ExerciseProgressionPointDto[]> {
  const exercises = await prisma.workoutExercise.findMany({
    where: { name: exerciseName, workout: { trainingSession: { userId } } },
    include: { sets: true, workout: { include: { trainingSession: true } } },
    orderBy: { workout: { trainingSession: { date: "asc" } } },
  });

  const points: ExerciseProgressionPointDto[] = [];
  let runningBestWeight = 0;
  let runningBest1RM = 0;

  for (const exercise of exercises) {
    const workingSets = exercise.sets.filter((s) => !s.isWarmup);
    if (workingSets.length === 0) continue;

    const bestWeightKg = Math.max(...workingSets.map((s) => s.weightKg));
    const estimated1RM = Math.max(...workingSets.map((s) => estimateOneRepMax(s.weightKg, s.reps)));
    const volume = workingSets.reduce((sum, s) => sum + s.reps * s.weightKg, 0);

    const isPr = estimated1RM > runningBest1RM;
    runningBestWeight = Math.max(runningBestWeight, bestWeightKg);
    runningBest1RM = Math.max(runningBest1RM, estimated1RM);

    points.push({
      date: exercise.workout.trainingSession.date.toISOString(),
      bestWeightKg,
      estimated1RM,
      volume: Math.round(volume * 10) / 10,
      isPr,
    });
  }

  return points;
}

const DAY_MS = 24 * 60 * 60 * 1000;

export async function getLoadTrend(userId: string, from: Date, to: Date) {
  const sessions = await prisma.trainingSession.findMany({
    where: { userId, date: { gte: from, lte: to } },
    select: { date: true, trainingLoad: true },
    orderBy: { date: "asc" },
  });

  const byDay = new Map<string, number>();
  for (const s of sessions) {
    const key = s.date.toISOString().slice(0, 10);
    byDay.set(key, (byDay.get(key) ?? 0) + s.trainingLoad);
  }

  return Array.from(byDay.entries())
    .map(([date, load]) => ({ date, load }))
    .sort((a, b) => a.date.localeCompare(b.date));
}

export async function getWeeklyLoadSummary(userId: string) {
  const now = new Date();
  const sevenDaysAgo = new Date(now.getTime() - 7 * DAY_MS);
  const twentyEightDaysAgo = new Date(now.getTime() - 28 * DAY_MS);

  const [recentSessions, chronicSessions] = await Promise.all([
    prisma.trainingSession.findMany({ where: { userId, date: { gte: sevenDaysAgo } } }),
    prisma.trainingSession.findMany({ where: { userId, date: { gte: twentyEightDaysAgo } } }),
  ]);

  const weeklyLoad = recentSessions.reduce((sum, s) => sum + s.trainingLoad, 0);
  const acuteAvgDaily = weeklyLoad / 7;
  const chronicAvgDaily = chronicSessions.reduce((sum, s) => sum + s.trainingLoad, 0) / 28;
  const acwr = chronicAvgDaily > 0 ? Math.round((acuteAvgDaily / chronicAvgDaily) * 100) / 100 : null;

  return {
    weeklyLoad,
    acwr,
    sessionsThisWeek: recentSessions.length,
  };
}
