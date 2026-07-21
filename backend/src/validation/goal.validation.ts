import { z } from "zod";
import { GoalDirection, GoalType } from "@momentum/shared";

export const createGoalSchema = z
  .object({
    type: z.nativeEnum(GoalType),
    title: z.string().min(1),
    targetValue: z.number().optional(),
    targetUnit: z.string().optional(),
    direction: z.nativeEnum(GoalDirection),
    targetDate: z.string().datetime().optional(),
    relatedExerciseName: z.string().optional(),
    notes: z.string().optional(),
  })
  .superRefine((data, ctx) => {
    if (data.type === GoalType.EXERCISE_PERFORMANCE && !data.relatedExerciseName) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["relatedExerciseName"],
        message: "Exercise performance goals need a linked exercise",
      });
    }
  });

export const updateGoalSchema = z.object({
  title: z.string().min(1).optional(),
  targetValue: z.number().optional(),
  targetUnit: z.string().optional(),
  targetDate: z.string().datetime().nullable().optional(),
  notes: z.string().optional(),
  status: z.enum(["ON_TRACK", "PAUSED"]).optional(),
});

export const goalQuerySchema = z.object({
  status: z.string().optional(),
  type: z.string().optional(),
});
