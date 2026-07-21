import { z } from "zod";

export const healthConnectSyncSchema = z.object({
  weightRecords: z
    .array(
      z.object({
        externalId: z.string().min(1),
        date: z.string().datetime(),
        weightKg: z.number().positive(),
      })
    )
    .default([]),
  exerciseSessions: z
    .array(
      z.object({
        externalId: z.string().min(1),
        startTime: z.string().datetime(),
        endTime: z.string().datetime(),
        exerciseType: z.string().min(1),
        totalEnergyKcal: z.number().nonnegative().optional().nullable(),
        avgHeartRate: z.number().positive().optional().nullable(),
      })
    )
    .default([]),
  sleepSessions: z
    .array(
      z.object({
        externalId: z.string().min(1),
        date: z.string().datetime(),
        totalSleepMinutes: z.number().nonnegative(),
      })
    )
    .default([]),
});
