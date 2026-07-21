import { z } from "zod";

export const upsertRecoveryRecordSchema = z.object({
  date: z.string().datetime().optional(),
  sleepHours: z.number().min(0).max(24).optional(),
  sleepQuality: z.number().int().min(1).max(5).optional(),
  restingHr: z.number().int().positive().optional(),
  hrv: z.number().positive().optional(),
  soreness: z.number().int().min(1).max(5).optional(),
  energy: z.number().int().min(1).max(5).optional(),
  sleepScore: z.number().int().min(0).max(100).optional(),
  strain: z.number().min(0).max(21).optional(),
  notes: z.string().optional(),
});
