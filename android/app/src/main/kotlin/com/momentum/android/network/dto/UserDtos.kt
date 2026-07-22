package com.momentum.android.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val name: String,
    val avatarUrl: String? = null,
    val unitSystem: UnitSystem,
    val dateOfBirth: String? = null,
    val heightCm: Double? = null,
    val estimatedDailyBurnKcal: Int? = null,
    val onboardingStatus: OnboardingStatus,
    val createdAt: String,
)

@Serializable
data class PrimaryGoalRequest(
    val type: GoalType,
    val title: String,
    val targetValue: Double,
    val targetUnit: String,
    val direction: GoalDirection,
    val targetDate: String? = null,
)

@Serializable
data class OnboardingRequest(
    val dateOfBirth: String? = null,
    val heightCm: Double? = null,
    val currentWeightKg: Double? = null,
    val unitSystem: UnitSystem? = null,
    val trainingFrequencyPerWeek: Int? = null,
    val primaryGoal: PrimaryGoalRequest? = null,
)

@Serializable
data class UpdateProfileRequest(
    val name: String? = null,
    val unitSystem: UnitSystem? = null,
    val dateOfBirth: String? = null,
    val heightCm: Double? = null,
    val estimatedDailyBurnKcal: Int? = null,
)
