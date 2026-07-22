package com.momentum.android.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class WeightSummaryDto(
    val latestWeightKg: Double? = null,
    val movingAverageKg: Double? = null,
    val goalWeightKg: Double? = null,
    val rateOfChangeKgPerWeek: Double? = null,
)

@Serializable
data class TrainingLoadSummaryDto(
    val weeklyLoad: Double,
    val acwr: Double? = null,
    val sessionsThisWeek: Int,
)

@Serializable
data class GamificationDto(
    val loggingStreakDays: Int,
    val trainingStreakDays: Int,
    val weeklyCompletionScore: Int,
    val recentAchievements: List<AchievementDto>,
)

@Serializable
data class DashboardTodayDto(
    val readiness: RecoveryRecordDto? = null,
    val weightSummary: WeightSummaryDto,
    val nutritionSummary: NutritionSummaryDto,
    val trainingLoadSummary: TrainingLoadSummaryDto,
    val timelineToday: List<TimelineEntryDto>,
    val goalsStrip: List<GoalDto>,
    val gamification: GamificationDto,
    val topInsights: List<InsightDto>,
)
