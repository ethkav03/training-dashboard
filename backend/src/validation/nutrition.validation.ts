import { z } from "zod";
import { MealType } from "@momentum/shared";

export const createNutritionEntrySchema = z.object({
  date: z.string().datetime(),
  mealType: z.nativeEnum(MealType).default(MealType.SNACKS),
  mealName: z.string().optional(),
  calories: z.number().int().nonnegative(),
  proteinG: z.number().nonnegative().optional(),
  carbsG: z.number().nonnegative().optional(),
  fatG: z.number().nonnegative().optional(),
  notes: z.string().optional(),
});

export const updateNutritionEntrySchema = createNutritionEntrySchema.partial();

export const summaryQuerySchema = z.object({
  date: z.string().datetime().optional(),
});

export const summaryRangeQuerySchema = z.object({
  from: z.string().datetime(),
  to: z.string().datetime(),
});
