package com.momentum.android.network

import com.momentum.android.network.dto.ActivityType
import com.momentum.android.network.dto.AchievementDto
import com.momentum.android.network.dto.CreateGoalRequest
import com.momentum.android.network.dto.CreateNutritionEntryRequest
import com.momentum.android.network.dto.CreateWeightEntryRequest
import com.momentum.android.network.dto.DashboardTodayDto
import com.momentum.android.network.dto.EnergyBalancePointDto
import com.momentum.android.network.dto.ExerciseProgressionPointDto
import com.momentum.android.network.dto.GoalDto
import com.momentum.android.network.dto.InsightDto
import com.momentum.android.network.dto.IntegrationConnectionDto
import com.momentum.android.network.dto.LoadSummaryDto
import com.momentum.android.network.dto.LoadTrendPointDto
import com.momentum.android.network.dto.NutritionEntryDto
import com.momentum.android.network.dto.NutritionSummaryDto
import com.momentum.android.network.dto.OnboardingRequest
import com.momentum.android.network.dto.RecoveryRecordDto
import com.momentum.android.network.dto.TimelineEntryDto
import com.momentum.android.network.dto.TrainingSessionDto
import com.momentum.android.network.dto.TrainingSessionWriteDto
import com.momentum.android.network.dto.UpdateGoalRequest
import com.momentum.android.network.dto.UpdateNutritionEntryRequest
import com.momentum.android.network.dto.UpdateProfileRequest
import com.momentum.android.network.dto.UpdateWeightEntryRequest
import com.momentum.android.network.dto.UpsertRecoveryRecordRequest
import com.momentum.android.network.dto.UserDto
import com.momentum.android.network.dto.WeightEntryDto
import com.momentum.android.network.dto.WeightTrendDto
import com.momentum.android.network.dto.WhoopSyncResultDto
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class GoogleMobileAuthRequest(val idToken: String)

@Serializable
data class AuthTokenResponse(val token: String)

// Field names/shapes here must match backend/src/validation/healthConnect.validation.ts
// and healthConnectService.ts exactly -- this is a hand-kept contract, not a
// generated client.
@Serializable
data class HealthConnectWeightRecordDto(
    val externalId: String,
    val date: String,
    val weightKg: Double,
)

@Serializable
data class HealthConnectExerciseSessionDto(
    val externalId: String,
    val startTime: String,
    val endTime: String,
    val exerciseType: String,
    val totalEnergyKcal: Double? = null,
    val avgHeartRate: Double? = null,
)

@Serializable
data class HealthConnectSleepSessionDto(
    val externalId: String,
    val date: String,
    val totalSleepMinutes: Double,
)

@Serializable
data class HealthConnectSyncRequest(
    val weightRecords: List<HealthConnectWeightRecordDto>,
    val exerciseSessions: List<HealthConnectExerciseSessionDto>,
    val sleepSessions: List<HealthConnectSleepSessionDto>,
)

@Serializable
data class HealthConnectSyncResultDto(
    val status: String,
    val syncedAt: String,
    val weightRecordsSynced: Int,
    val weightRecordsSkippedManualEdit: Int,
    val trainingSessionsSynced: Int,
    val trainingSessionsSkippedManualEdit: Int,
    val recoveryRecordsSynced: Int,
    val recoveryRecordsSkippedManualEdit: Int,
)

/**
 * Full REST surface mirroring the backend's route files under
 * backend/src/routes/, grouped the same way the backend itself groups them.
 * Every DTO comes from network/dto/ and is a hand-ported mirror of
 * packages/shared/src/dto.ts -- kept name-aligned so the two are easy to
 * cross-reference, not generated.
 */
interface MomentumApi {

    // -- Auth --
    @POST("auth/google/mobile")
    suspend fun exchangeGoogleIdToken(@Body body: GoogleMobileAuthRequest): AuthTokenResponse

    // -- Users --
    @GET("users/me")
    suspend fun getCurrentUser(): UserDto

    @PATCH("users/me")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): UserDto

    @POST("users/me/onboarding")
    suspend fun completeOnboarding(@Body body: OnboardingRequest): UserDto

    @POST("users/me/onboarding/skip")
    suspend fun skipOnboarding(): UserDto

    @DELETE("users/me")
    suspend fun deleteAccount()

    // -- Weight (Body) --
    @GET("weight")
    suspend fun getWeightEntries(@Query("from") from: String? = null, @Query("to") to: String? = null): List<WeightEntryDto>

    @GET("weight/trend")
    suspend fun getWeightTrend(@Query("from") from: String? = null, @Query("to") to: String? = null): WeightTrendDto

    @POST("weight")
    suspend fun createWeightEntry(@Body body: CreateWeightEntryRequest): WeightEntryDto

    @PATCH("weight/{id}")
    suspend fun updateWeightEntry(@Path("id") id: String, @Body body: UpdateWeightEntryRequest): WeightEntryDto

    @DELETE("weight/{id}")
    suspend fun deleteWeightEntry(@Path("id") id: String)

    // -- Nutrition (Fuel) --
    @GET("nutrition")
    suspend fun getNutritionEntries(@Query("from") from: String? = null, @Query("to") to: String? = null): List<NutritionEntryDto>

    @GET("nutrition/summary")
    suspend fun getNutritionSummary(@Query("date") date: String? = null): NutritionSummaryDto

    @GET("nutrition/summary/range")
    suspend fun getNutritionSummaryRange(@Query("from") from: String, @Query("to") to: String): List<NutritionSummaryDto>

    // granularity is passed as its wireValue (lowercase) -- see
    // EnergyBalanceGranularity's comment in network/dto/Enums.kt for why a
    // raw String param is used here instead of the enum type directly.
    @GET("nutrition/energy-balance")
    suspend fun getEnergyBalanceSeries(@Query("granularity") granularity: String): List<EnergyBalancePointDto>

    @POST("nutrition")
    suspend fun createNutritionEntry(@Body body: CreateNutritionEntryRequest): NutritionEntryDto

    @PATCH("nutrition/{id}")
    suspend fun updateNutritionEntry(@Path("id") id: String, @Body body: UpdateNutritionEntryRequest): NutritionEntryDto

    @DELETE("nutrition/{id}")
    suspend fun deleteNutritionEntry(@Path("id") id: String)

    // -- Training --
    @GET("training/gym/exercises")
    suspend fun getExerciseNames(): List<String>

    @GET("training/gym/exercises/{name}/progression")
    suspend fun getExerciseProgression(@Path("name") name: String): List<ExerciseProgressionPointDto>

    @GET("training/load-trend")
    suspend fun getLoadTrend(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("type") type: ActivityType? = null,
    ): List<LoadTrendPointDto>

    @GET("training/load-summary")
    suspend fun getLoadSummary(): LoadSummaryDto

    @GET("training")
    suspend fun getTrainingSessions(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("type") type: ActivityType? = null,
    ): List<TrainingSessionDto>

    @GET("training/{id}")
    suspend fun getTrainingSession(@Path("id") id: String): TrainingSessionDto

    @POST("training")
    suspend fun createTrainingSession(@Body body: TrainingSessionWriteDto): TrainingSessionDto

    // Full replace, not a partial patch -- matches the backend, which has
    // no separate update schema for training sessions.
    @PUT("training/{id}")
    suspend fun replaceTrainingSession(@Path("id") id: String, @Body body: TrainingSessionWriteDto): TrainingSessionDto

    @DELETE("training/{id}")
    suspend fun deleteTrainingSession(@Path("id") id: String)

    // -- Recovery --
    // Backend returns a literal JSON `null` body (not 204, not an empty
    // object) when there's no record for today -- Retrofit 2.9+'s suspend
    // support handles a nullable return type for this correctly, but this
    // hasn't been exercised against a real device yet; worth a first check
    // when the Recovery screen actually calls it (Sprint 19).
    @GET("recovery/today")
    suspend fun getRecoveryToday(): RecoveryRecordDto?

    @GET("recovery")
    suspend fun getRecoveryHistory(@Query("from") from: String? = null, @Query("to") to: String? = null): List<RecoveryRecordDto>

    // Upsert keyed by (userId, date) server-side -- always POST, never PATCH.
    @POST("recovery")
    suspend fun upsertRecoveryRecord(@Body body: UpsertRecoveryRecordRequest): RecoveryRecordDto

    @DELETE("recovery/{id}")
    suspend fun deleteRecoveryRecord(@Path("id") id: String)

    // -- Goals --
    @GET("goals")
    suspend fun getGoals(@Query("status") status: String? = null, @Query("type") type: String? = null): List<GoalDto>

    @GET("goals/{id}")
    suspend fun getGoal(@Path("id") id: String): GoalDto

    @POST("goals")
    suspend fun createGoal(@Body body: CreateGoalRequest): GoalDto

    @PATCH("goals/{id}")
    suspend fun updateGoal(@Path("id") id: String, @Body body: UpdateGoalRequest): GoalDto

    @DELETE("goals/{id}")
    suspend fun deleteGoal(@Path("id") id: String)

    // -- Insights --
    @GET("insights")
    suspend fun getInsights(): List<InsightDto>

    // -- Timeline --
    @GET("timeline")
    suspend fun getTimeline(@Query("from") from: String, @Query("to") to: String): List<TimelineEntryDto>

    // -- Achievements --
    @GET("achievements")
    suspend fun getAchievements(@Query("type") type: String? = null): List<AchievementDto>

    // -- Dashboard --
    @GET("dashboard/today")
    suspend fun getDashboardToday(): DashboardTodayDto

    // -- Integrations --
    @GET("integrations")
    suspend fun getIntegrations(): List<IntegrationConnectionDto>

    // Connect/disconnect stay web-only (see docs/architecture.md's Native
    // Android app section) -- but syncing an *already-connected* WHOOP
    // account needs no OAuth flow, just this same authenticated POST the
    // web "Sync now" button already calls, so it's fair game from Android too.
    @POST("integrations/whoop/sync")
    suspend fun syncWhoop(): WhoopSyncResultDto

    @POST("integrations/health-connect/sync")
    suspend fun syncHealthConnect(@Body body: HealthConnectSyncRequest): HealthConnectSyncResultDto
}
