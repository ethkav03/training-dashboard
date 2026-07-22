package com.momentum.android.data

import com.momentum.android.network.MomentumApi
import com.momentum.android.network.dto.IntegrationConnectionDto

// WHOOP connect/disconnect stay web-only -- this repository only ever reads
// status (for a read-only Settings row), it never drives an OAuth flow.
class IntegrationsRepository(private val api: MomentumApi) {
    suspend fun list(): List<IntegrationConnectionDto> = api.getIntegrations()
}
