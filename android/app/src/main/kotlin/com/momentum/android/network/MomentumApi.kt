package com.momentum.android.network

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

@Serializable
data class GoogleMobileAuthRequest(val idToken: String)

@Serializable
data class AuthTokenResponse(val token: String)

@Serializable
data class UserProfileResponse(
    val id: String,
    val email: String,
    val name: String,
    val avatarUrl: String? = null,
)

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

interface MomentumApi {
    @POST("auth/google/mobile")
    suspend fun exchangeGoogleIdToken(@Body body: GoogleMobileAuthRequest): AuthTokenResponse

    @GET("users/me")
    suspend fun getCurrentUser(): UserProfileResponse

    @POST("integrations/health-connect/sync")
    suspend fun syncHealthConnect(@Body body: HealthConnectSyncRequest): HealthConnectSyncResultDto
}
