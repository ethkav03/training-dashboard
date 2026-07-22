package com.momentum.android.data

import com.momentum.android.network.MomentumApi
import com.momentum.android.network.dto.AchievementDto

class AchievementRepository(private val api: MomentumApi) {
    suspend fun list(type: String? = null): List<AchievementDto> = api.getAchievements(type)
}
