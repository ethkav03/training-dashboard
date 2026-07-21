import { z } from "zod";

export const createNutritionEntrySchema = z.object({
  date: z.string().datetime(),
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
