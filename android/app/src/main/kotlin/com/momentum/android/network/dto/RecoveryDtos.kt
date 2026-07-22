package com.momentum.android.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecoveryRecordDto(
    val id: String,
    val date: String,
    val sleepHours: Double? = null,
    val sleepQuality: Int? = null,
    val restingHr: Int? = null,
    val hrv: Double? = null,
    val soreness: Int? = null,
    val energy: Int? = null,
    val sleepScore: Int? = null,
    val strain: Double? = null,
    val readinessScore: Int,
    val readinessLevel: ReadinessLevel,
    val recommendation: String,
    val notes: String? = null,
    val source: DataSource,
)

// POST / is an upsert keyed by (userId, date) -- every field optional,
// server defaults date to "now" if omitted.
@Serializable
data class UpsertRecoveryRecordRequest(
    val date: String? = null,
    val sleepHours: Double? = null,
    val sleepQuality: Int? = null,
    val restingHr: Int? = null,
    val hrv: Double? = null,
    val soreness: Int? = null,
    val energy: Int? = null,
    val sleepScore: Int? = null,
    val strain: Double? = null,
    val notes: String? = null,
)
