import { prisma } from "../lib/prisma.js";

const DAY_MS = 24 * 60 * 60 * 1000;

function dayKey(date: Date): string {
  return date.toISOString().slice(0, 10);
}

async function getLoggedDaySet(userId: string, sinceDays: number): Promise<Set<string>> {
  const since = new Date(Date.now() - sinceDays * DAY_MS);
  const [weights, meals, sessions, recoveries] = await Promise.all([
    prisma.weightEntry.findMany({ where: { userId, date: { gte: since } }, select: { date: true } }),
    prisma.nutritionEntry.findMany({ where: { userId, date: { gte: since } }, select: { date: true } }),
    prisma.trainingSession.findMany({ where: { userId, date: { gte: since } }, select: { date: true } }),
    prisma.recoveryRecord.findMany({ where: { userId, date: { gte: since } }, select: { date: true } }),
  ]);
  const days = new Set<string>();
  for (const row of [...weights, ...meals, ...sessions, ...recoveries]) {
    days.add(dayKey(row.date));
  }
  return days;
}

/** Consecutive days up to today with at least one logged entry of any kind. */
export async function computeLoggingStreak(userId: string): Promise<number> {
  const loggedDays = await getLoggedDaySet(userId, 90);
  let streak = 0;
  const cursor = new Date();
  cursor.setHours(0, 0, 0, 0);
  while (loggedDays.has(dayKey(cursor))) {
    streak++;
    cursor.setDate(cursor.getDate() - 1);
  }
  return streak;
}

/** Consecutive days up to today with at least one training session. */
export async function computeTrainingStreak(userId: string): Promise<number> {
  const since = new Date(Date.now() - 90 * DAY_MS);
  const sessions = await prisma.trainingSession.findMany({ where: { userId, date: { gte: since } }, select: { date: true } });
  const days = new Set(sessions.map((s) => dayKey(s.date)));
  let streak = 0;
  const cursor = new Date();
  cursor.setHours(0, 0, 0, 0);
  while (days.has(dayKey(cursor))) {
    streak++;
    cursor.setDate(cursor.getDate() - 1);
  }
  return streak;
}

/** % of the last 7 days with at least one logged entry of any kind. */
export async function computeWeeklyCompletionScore(userId: string): Promise<number> {
  const loggedDays = await getLoggedDaySet(userId, 7);
  const daysWithData = Array.from(loggedDays).filter((key) => {
    const diff = (Date.now() - new Date(key).getTime()) / DAY_MS;
    return diff <= 7;
  }).length;
  return Math.round((daysWithData / 7) * 100);
}

export async function getRecentAchievements(userId: string, limit = 5) {
  return prisma.achievement.findMany({
    where: { userId },
    orderBy: { achievedAt: "desc" },
    take: limit,
  });
}
