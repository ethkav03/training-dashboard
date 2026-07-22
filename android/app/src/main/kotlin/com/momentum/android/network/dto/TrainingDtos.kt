package com.momentum.android.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutSetDto(
    val id: String? = null,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double,
    val rpe: Double? = null,
    val isWarmup: Boolean = false,
)

@Serializable
data class WorkoutExerciseDto(
    val id: String? = null,
    val name: String,
    val orderIndex: Int,
    val sets: List<WorkoutSetDto>,
)

@Serializable
data class WorkoutDto(
    val exercises: List<WorkoutExerciseDto>,
)

@Serializable
data class MatchDetailDto(
    val opponent: String? = null,
    val competition: String? = null,
    val position: String? = null,
    val minutesPlayed: Int? = null,
    val result: MatchResult? = null,
    val performanceRating: Int? = null,
    val keyStats: Map<String, Double>? = null,
    val injuryNotes: String? = null,
    val reflection: String? = null,
)

@Serializable
data class TrainingSessionDto(
    val id: String,
    val type: ActivityType,
    val date: String,
    val durationMin: Int,
    val intensity: Int,
    val caloriesBurned: Int? = null,
    val trainingLoad: Double,
    val notes: String? = null,
    val source: DataSource,
    val isManuallyEdited: Boolean,
    val workout: WorkoutDto? = null,
    val matchDetail: MatchDetailDto? = null,
)

// POST / and PUT /:id both use this exact shape (PUT is a full replace, not
// a partial patch -- there is no separate training update DTO).
@Serializable
data class TrainingSessionWriteDto(
    val type: ActivityType,
    val date: String,
    val durationMin: Int,
    val intensity: Int,
    val caloriesBurned: Int? = null,
    val notes: String? = null,
    val workout: WorkoutDto? = null,
    val matchDetail: MatchDetailDto? = null,
)

@Serializable
data class ExerciseProgressionPointDto(
    val date: String,
    val bestWeightKg: Double,
    val estimated1RM: Double,
    val volume: Double,
    val isPr: Boolean,
)

@Serializable
data class LoadTrendPointDto(
    val date: String,
    val load: Double,
)

@Serializable
data class LoadSummaryDto(
    val weeklyLoad: Double,
    val acwr: Double? = null,
    val sessionsThisWeek: Int,
)
