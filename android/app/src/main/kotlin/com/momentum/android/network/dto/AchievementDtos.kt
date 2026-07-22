package com.momentum.android.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class AchievementDto(
    val id: String,
    val type: AchievementType,
    val title: String,
    val description: String? = null,
    val value: Double? = null,
    val achievedAt: String,
)
