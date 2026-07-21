import type { TimelineEntryDto } from "@momentum/shared";
import { prisma } from "../lib/prisma.js";

export async function getTimeline(userId: string, from: Date, to: Date): Promise<TimelineEntryDto[]> {
  const [weightEntries, nutritionEntries, trainingSessions, recoveryRecords, achievements] = await Promise.all([
    prisma.weightEntry.findMany({ where: { userId, date: { gte: from, lte: to } } }),
    prisma.nutritionEntry.findMany({ where: { userId, date: { gte: from, lte: to } } }),
    prisma.trainingSession.findMany({
      where: { userId, date: { gte: from, lte: to } },
      include: { matchDetail: true },
    }),
    prisma.recoveryRecord.findMany({ where: { userId, date: { gte: from, lte: to } } }),
    prisma.achievement.findMany({ where: { userId, achievedAt: { gte: from, lte: to } } }),
  ]);

  const entries: TimelineEntryDto[] = [
    ...weightEntries.map((w) => ({
      date: w.date.toISOString(),
      kind: "WEIGHT" as const,
      id: `weight-${w.id}`,
      title: `Weighed in at ${w.weightKg} kg`,
      subtitle: w.note,
      refId: w.id,
    })),
    ...nutritionEntries.map((n) => ({
      date: n.date.toISOString(),
      kind: "MEAL" as const,
      id: `meal-${n.id}`,
      title: n.mealName || "Meal logged",
      subtitle: `${n.calories} kcal`,
      refId: n.id,
    })),
    ...trainingSessions.map((s) => ({
      date: s.date.toISOString(),
      kind: "TRAINING" as const,
      id: `training-${s.id}`,
      title: s.matchDetail
        ? `Match${s.matchDetail.opponent ? ` vs ${s.matchDetail.opponent}` : ""}`
        : `${s.type.replace(/_/g, " ").toLowerCase()} session`,
      subtitle: `${s.durationMin} min · intensity ${s.intensity}/10`,
      refId: s.id,
    })),
    ...recoveryRecords.map((r) => ({
      date: r.date.toISOString(),
      kind: "RECOVERY" as const,
      id: `recovery-${r.id}`,
      title: `Readiness: ${r.readinessLevel.toLowerCase()}`,
      subtitle: `Score ${r.readinessScore}`,
      refId: r.id,
    })),
    ...achievements.map((a) => ({
      date: a.achievedAt.toISOString(),
      kind: "ACHIEVEMENT" as const,
      id: `achievement-${a.id}`,
      title: a.title,
      subtitle: a.description,
      refId: a.id,
    })),
  ];

  return entries.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
}
