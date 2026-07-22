package com.momentum.android.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class NutritionEntryDto(
    val id: String,
    val date: String,
    val mealType: MealType,
    val mealName: String? = null,
    val calories: Int,
    val proteinG: Double? = null,
    val carbsG: Double? = null,
    val fatG: Double? = null,
    val notes: String? = null,
    val source: DataSource,
    val isManuallyEdited: Boolean,
)

@Serializable
data class EstimatedBurnDto(
    val baselineKcal: Int,
    val trainingKcal: Int,
    val totalKcal: Int,
    val isEstimate: Boolean = true,
)

@Serializable
data class NutritionSummaryDto(
    val date: String,
    val totalCalories: Int,
    val totalProteinG: Double,
    val totalCarbsG: Double,
    val totalFatG: Double,
    val targetCalories: Int? = null,
    val targetProteinG: Double? = null,
    val estimatedBurn: EstimatedBurnDto,
    val balanceKcal: Int? = null,
)

@Serializable
data class EnergyBalancePointDto(
    val period: String,
    val totalCalories: Int,
    val totalBurnKcal: Int,
    val balanceKcal: Int? = null,
)

@Serializable
data class CreateNutritionEntryRequest(
    val date: String,
    val mealType: MealType = MealType.SNACKS,
    val mealName: String? = null,
    val calories: Int,
    val proteinG: Double? = null,
    val carbsG: Double? = null,
    val fatG: Double? = null,
    val notes: String? = null,
)

@Serializable
data class UpdateNutritionEntryRequest(
    val date: String? = null,
    val mealType: MealType? = null,
    val mealName: String? = null,
    val calories: Int? = null,
    val proteinG: Double? = null,
    val carbsG: Double? = null,
    val fatG: Double? = null,
    val notes: String? = null,
)
