import { z } from "zod";
import { ActivityType, MatchResult } from "@momentum/shared";

const workoutSetSchema = z.object({
  setNumber: z.number().int().positive(),
  reps: z.number().int().positive(),
  weightKg: z.number().nonnegative(),
  rpe: z.number().min(0).max(10).optional(),
  isWarmup: z.boolean().default(false),
});

const workoutExerciseSchema = z.object({
  name: z.string().min(1),
  orderIndex: z.number().int().nonnegative(),
  sets: z.array(workoutSetSchema).min(1),
});

const matchDetailSchema = z.object({
  opponent: z.string().optional(),
  competition: z.string().optional(),
  position: z.string().optional(),
  minutesPlayed: z.number().int().nonnegative().optional(),
  result: z.nativeEnum(MatchResult).optional(),
  performanceRating: z.number().int().min(1).max(10).optional(),
  keyStats: z.record(z.number()).optional(),
  injuryNotes: z.string().optional(),
  reflection: z.string().optional(),
});

export const createTrainingSessionSchema = z
  .object({
    type: z.nativeEnum(ActivityType),
    date: z.string().datetime(),
    durationMin: z.number().int().positive(),
    intensity: z.number().int().min(1).max(10),
    caloriesBurned: z.number().int().nonnegative().optional(),
    notes: z.string().optional(),
    workout: z.object({ exercises: z.array(workoutExerciseSchema).min(1) }).optional(),
    matchDetail: matchDetailSchema.optional(),
  })
  .superRefine((data, ctx) => {
    if (data.type === ActivityType.GYM && !data.workout) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ["workout"], message: "Gym sessions require exercises" });
    }
    if (data.type !== ActivityType.GYM && data.workout) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ["workout"], message: "Only gym sessions can include exercises" });
    }
    if (data.type === ActivityType.MATCH && !data.matchDetail) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ["matchDetail"], message: "Matches require match details" });
    }
    if (data.type !== ActivityType.MATCH && data.matchDetail) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ["matchDetail"], message: "Only matches can include match details" });
    }
  });

export const trainingQuerySchema = z.object({
  from: z.string().datetime().optional(),
  to: z.string().datetime().optional(),
  type: z.nativeEnum(ActivityType).optional(),
});
