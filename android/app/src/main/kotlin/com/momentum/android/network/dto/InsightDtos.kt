package com.momentum.android.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class InsightMetricsDto(
    val current: Double,
    val previous: Double,
    val unit: String,
    val windowDays: Int,
)

@Serializable
data class InsightDto(
    val id: String,
    val headline: String,
    val detail: String,
    // Wire values are literally "up" | "down" | "flat" -- plain String
    // rather than an enum since these aren't shared via enums.ts on the
    // TS side either.
    val trend: String,
    val metrics: InsightMetricsDto,
    val generatedAt: String,
)
