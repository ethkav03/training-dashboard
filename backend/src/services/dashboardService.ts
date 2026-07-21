import type { AchievementDto, DashboardTodayDto } from "@momentum/shared";
import { getLatestWeightSummary } from "./weightService.js";
import { getNutritionSummaryForDate } from "./nutritionService.js";
import { getWeeklyLoadSummary } from "./trainingService.js";
import { getRecoveryToday, toRecoveryRecordDto } from "./recoveryService.js";
import { listGoals } from "./goalService.js";
import { getTimeline } from "./timelineService.js";
import { computeLoggingStreak, computeTrainingStreak, computeWeeklyCompletionScore, getRecentAchievements } from "./gamificationService.js";
import { getTopInsights } from "./insightsService.js";

function dayBounds(date: Date) {
  const start = new Date(date);
  start.setHours(0, 0, 0, 0);
  const end = new Date(start);
  end.setDate(end.getDate() + 1);
  end.setMilliseconds(-1);
  return { start, end };
}

function toAchievementDto(a: { id: string; type: string; title: string; description: string | null; value: number | null; achievedAt: Date }): AchievementDto {
  return {
    id: a.id,
    type: a.type as AchievementDto["type"],
    title: a.title,
    description: a.description,
    value: a.value,
    achievedAt: a.achievedAt.toISOString(),
  };
}

export async function getDashboardToday(userId: string): Promise<DashboardTodayDto> {
  const { start, end } = dayBounds(new Date());

  const [
    weightSummary,
    nutritionSummary,
    trainingLoadSummary,
    recoveryToday,
    activeGoals,
    timelineToday,
    loggingStreakDays,
    trainingStreakDays,
    weeklyCompletionScore,
    recentAchievements,
    topInsights,
  ] = await Promise.all([
    getLatestWeightSummary(userId),
    getNutritionSummaryForDate(userId, new Date()),
    getWeeklyLoadSummary(userId),
    getRecoveryToday(userId),
    listGoals(userId, {}),
    getTimeline(userId, start, end),
    computeLoggingStreak(userId),
    computeTrainingStreak(userId),
    computeWeeklyCompletionScore(userId),
    getRecentAchievements(userId, 5),
    getTopInsights(userId, 3),
  ]);

  return {
    readiness: recoveryToday ? toRecoveryRecordDto(recoveryToday) : null,
    weightSummary,
    nutritionSummary,
    trainingLoadSummary,
    timelineToday,
    goalsStrip: activeGoals.filter((g) => g.status !== "ACHIEVED" && g.status !== "PAUSED").slice(0, 4),
    gamification: {
      loggingStreakDays,
      trainingStreakDays,
      weeklyCompletionScore,
      recentAchievements: recentAchievements.map(toAchievementDto),
    },
    topInsights,
  };
}
