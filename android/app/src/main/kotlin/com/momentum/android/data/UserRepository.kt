package com.momentum.android.data

import com.momentum.android.network.MomentumApi
import com.momentum.android.network.dto.OnboardingRequest
import com.momentum.android.network.dto.UpdateProfileRequest
import com.momentum.android.network.dto.UserDto
import okhttp3.ResponseBody

class UserRepository(private val api: MomentumApi) {
    suspend fun me(): UserDto = api.getCurrentUser()

    suspend fun updateProfile(body: UpdateProfileRequest): UserDto = api.updateProfile(body)

    suspend fun completeOnboarding(body: OnboardingRequest): UserDto = api.completeOnboarding(body)

    suspend fun skipOnboarding(): UserDto = api.skipOnboarding()

    suspend fun deleteAccount() = api.deleteAccount()

    suspend fun exportData(): ResponseBody = api.exportData()
}
