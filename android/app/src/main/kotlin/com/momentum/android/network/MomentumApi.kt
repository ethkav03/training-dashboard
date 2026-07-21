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

interface MomentumApi {
    @POST("auth/google/mobile")
    suspend fun exchangeGoogleIdToken(@Body body: GoogleMobileAuthRequest): AuthTokenResponse

    @GET("users/me")
    suspend fun getCurrentUser(): UserProfileResponse
}
