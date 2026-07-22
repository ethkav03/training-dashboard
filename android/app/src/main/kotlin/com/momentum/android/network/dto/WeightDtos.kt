package com.momentum.android.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class WeightEntryDto(
    val id: String,
    val date: String,
    val weightKg: Double,
    val note: String? = null,
    val source: DataSource,
    val isManuallyEdited: Boolean,
)

@Serializable
data class WeightTrendPointDto(
    val date: String,
    val value: Double,
)

@Serializable
data class WeightTrendDto(
    val raw: List<WeightEntryDto>,
    val movingAverage: List<WeightTrendPointDto>,
    val rateOfChangeKgPerWeek: Double? = null,
    val latestWeightKg: Double? = null,
    val goalWeightKg: Double? = null,
)

@Serializable
data class CreateWeightEntryRequest(
    val date: String,
    val weightKg: Double,
    val note: String? = null,
)

@Serializable
data class UpdateWeightEntryRequest(
    val date: String? = null,
    val weightKg: Double? = null,
    val note: String? = null,
)
