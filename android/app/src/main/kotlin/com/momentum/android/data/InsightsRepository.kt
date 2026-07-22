package com.momentum.android.data

import com.momentum.android.network.MomentumApi
import com.momentum.android.network.dto.InsightDto

class InsightsRepository(private val api: MomentumApi) {
    suspend fun list(): List<InsightDto> = api.getInsights()
}
