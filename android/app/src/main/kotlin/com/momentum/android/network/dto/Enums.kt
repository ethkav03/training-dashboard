package com.momentum.android.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Ported from packages/shared/src/enums.ts -- wire values must match exactly.
// Every enum here serializes by its constant name (kotlinx.serialization's
// default), which already equals the wire string for all but
// EnergyBalanceGranularity (lowercase on the wire).

@Serializable
enum class DataSource { MANUAL, GOOGLE_FIT, WHOOP, MYFITNESSPAL, HEALTH_CONNECT }

@Serializable
enum class IntegrationProvider { WHOOP, HEALTH_CONNECT }

@Serializable
enum class SyncStatus { IDLE, SYNCING, SUCCESS, ERROR }

@Serializable
enum class UnitSystem { METRIC, IMPERIAL }

@Serializable
enum class OnboardingStatus { PENDING, SKIPPED, COMPLETED }

@Serializable
enum class ActivityType {
    GYM, RUNNING, TEAM_SPORT_TRAINING, MATCH, CYCLING, WALKING, RECOVERY_SESSION, OTHER,
}

@Serializable
enum class MatchResult { WIN, LOSS, DRAW }

@Serializable
enum class GoalType {
    BODY_WEIGHT, CALORIE_INTAKE, PROTEIN_INTAKE, TRAINING_FREQUENCY,
    EXERCISE_PERFORMANCE, SPORT_PERFORMANCE, SLEEP_RECOVERY, CUSTOM,
}

@Serializable
enum class GoalDirection { INCREASE, DECREASE }

@Serializable
enum class GoalStatus { ON_TRACK, AT_RISK, ACHIEVED, PAUSED }

@Serializable
enum class ReadinessLevel { HIGH, MODERATE, LOW }

@Serializable
enum class AchievementType { STREAK, PERSONAL_RECORD, MILESTONE, WEEKLY_COMPLETION }

@Serializable
enum class MealType { BREAKFAST, LUNCH, DINNER, SNACKS }

// Retrofit's @Query params are stringified via plain .toString(), which
// would default to the Kotlin constant name (e.g. "DAY"), NOT the
// @SerialName -- that annotation only governs kotlinx.serialization's JSON
// (de)serialization. Since this enum is only ever sent as a query param
// (never inside a JSON body), the explicit wireValue property is what
// callers must actually use; @SerialName is kept too so JSON round-tripping
// stays correct if this type is ever embedded in a body later.
@Serializable
enum class EnergyBalanceGranularity(val wireValue: String) {
    @SerialName("day") DAY("day"),
    @SerialName("week") WEEK("week"),
    @SerialName("month") MONTH("month"),
    @SerialName("year") YEAR("year"),
}

@Serializable
enum class TimelineEntryKind { WEIGHT, MEAL, TRAINING, RECOVERY, ACHIEVEMENT }
