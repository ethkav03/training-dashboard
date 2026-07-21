import { z } from "zod";
import { GoalDirection, GoalType, UnitSystem } from "@momentum/shared";

export const updateProfileSchema = z.object({
  name: z.string().min(1).optional(),
  unitSystem: z.nativeEnum(UnitSystem).optional(),
  dateOfBirth: z.string().datetime().optional(),
  heightCm: z.number().positive().optional(),
  estimatedDailyBurnKcal: z.number().int().positive().optional(),
});

export const onboardingSchema = z.object({
  dateOfBirth: z.string().datetime().optional(),
  heightCm: z.number().positive().optional(),
  currentWeightKg: z.number().positive().optional(),
  unitSystem: z.nativeEnum(UnitSystem).optional(),
  trainingFrequencyPerWeek: z.number().int().min(0).max(14).optional(),
  primaryGoal: z
    .object({
      type: z.nativeEnum(GoalType),
      title: z.string().min(1),
      targetValue: z.number(),
      targetUnit: z.string().min(1),
      direction: z.nativeEnum(GoalDirection),
      targetDate: z.string().datetime().optional(),
    })
    .optional(),
});
