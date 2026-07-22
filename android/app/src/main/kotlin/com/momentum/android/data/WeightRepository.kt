package com.momentum.android.data

import com.momentum.android.network.MomentumApi
import com.momentum.android.network.dto.CreateWeightEntryRequest
import com.momentum.android.network.dto.UpdateWeightEntryRequest
import com.momentum.android.network.dto.WeightEntryDto
import com.momentum.android.network.dto.WeightTrendDto

class WeightRepository(private val api: MomentumApi) {
    suspend fun list(from: String? = null, to: String? = null): List<WeightEntryDto> = api.getWeightEntries(from, to)

    suspend fun trend(from: String? = null, to: String? = null): WeightTrendDto = api.getWeightTrend(from, to)

    suspend fun create(body: CreateWeightEntryRequest): WeightEntryDto = api.createWeightEntry(body)

    suspend fun update(id: String, body: UpdateWeightEntryRequest): WeightEntryDto = api.updateWeightEntry(id, body)

    suspend fun delete(id: String) = api.deleteWeightEntry(id)
}
