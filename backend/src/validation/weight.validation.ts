import { z } from "zod";

export const createWeightEntrySchema = z.object({
  date: z.string().datetime(),
  weightKg: z.number().positive(),
  note: z.string().optional(),
});

export const updateWeightEntrySchema = createWeightEntrySchema.partial();

export const dateRangeQuerySchema = z.object({
  from: z.string().datetime().optional(),
  to: z.string().datetime().optional(),
});
