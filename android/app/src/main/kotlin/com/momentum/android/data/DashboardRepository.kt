package com.momentum.android.data

import com.momentum.android.network.MomentumApi
import com.momentum.android.network.dto.DashboardTodayDto

class DashboardRepository(private val api: MomentumApi) {
    suspend fun today(): DashboardTodayDto = api.getDashboardToday()
}
