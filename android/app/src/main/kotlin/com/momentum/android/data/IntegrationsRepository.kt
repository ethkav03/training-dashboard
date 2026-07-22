package com.momentum.android.data

import com.momentum.android.network.MomentumApi
import com.momentum.android.network.dto.IntegrationConnectionDto
import com.momentum.android.network.dto.WhoopSyncResultDto

// WHOOP connect/disconnect stay web-only -- this repository never drives an
// OAuth flow. Syncing an already-connected account is a plain authenticated
// POST, though, so that's fair game from Android.
class IntegrationsRepository(private val api: MomentumApi) {
    suspend fun list(): List<IntegrationConnectionDto> = api.getIntegrations()

    suspend fun syncWhoop(): WhoopSyncResultDto = api.syncWhoop()
}
