package com.momentum.android.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class GoalDto(
    val id: String,
    val type: GoalType,
    val title: String,
    val targetValue: Double? = null,
    val targetUnit: String? = null,
    val direction: GoalDirection,
    val startValue: Double? = null,
    val currentValue: Double? = null,
    val progressPercent: Double? = null,
    val startDate: String,
    val targetDate: String? = null,
    val status: GoalStatus,
    val relatedExerciseName: String? = null,
    val notes: String? = null,
    val achievedAt: String? = null,
)

@Serializable
data class CreateGoalRequest(
    val type: GoalType,
    val title: String,
    val targetValue: Double? = null,
    val targetUnit: String? = null,
    val direction: GoalDirection,
    val targetDate: String? = null,
    val relatedExerciseName: String? = null,
    val notes: String? = null,
)

// Only ON_TRACK/PAUSED are settable via this endpoint -- AT_RISK/ACHIEVED
// are always server-computed, never client-set. Use GoalStatus.ON_TRACK or
// GoalStatus.PAUSED here; never construct with the other two values.
//
// Note: the backend's Zod schema allows targetDate to be explicit null
// (clear the date) vs omitted (leave unchanged) -- a distinction this plain
// nullable Kotlin field can't represent yet, since the default Json config
// omits null-valued properties rather than sending them as literal `null`.
// Not needed until the Goals edit form actually ships (a later sprint);
// revisit then if "clear target date" needs to be a real user action.
@Serializable
data class UpdateGoalRequest(
    val title: String? = null,
    val targetValue: Double? = null,
    val targetUnit: String? = null,
    val targetDate: String? = null,
    val notes: String? = null,
    val status: GoalStatus? = null,
)
