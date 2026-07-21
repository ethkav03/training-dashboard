package com.momentum.android.auth

import com.momentum.android.network.GoogleMobileAuthRequest
import com.momentum.android.network.MomentumApi
import com.momentum.android.network.UserProfileResponse

class AuthRepository(
    private val api: MomentumApi,
    private val tokenStore: TokenStore,
) {
    val storedToken: String?
        get() = tokenStore.token

    suspend fun exchangeIdToken(idToken: String): Result<String> = runCatching {
        val response = api.exchangeGoogleIdToken(GoogleMobileAuthRequest(idToken))
        tokenStore.token = response.token
        response.token
    }

    suspend fun fetchCurrentUser(): Result<UserProfileResponse> = runCatching { api.getCurrentUser() }

    fun signOut() {
        tokenStore.token = null
    }
}
