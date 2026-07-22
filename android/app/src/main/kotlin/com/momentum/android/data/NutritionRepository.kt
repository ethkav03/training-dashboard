package com.momentum.android.data

import com.momentum.android.network.MomentumApi
import com.momentum.android.network.dto.CreateNutritionEntryRequest
import com.momentum.android.network.dto.EnergyBalanceGranularity
import com.momentum.android.network.dto.EnergyBalancePointDto
import com.momentum.android.network.dto.NutritionEntryDto
import com.momentum.android.network.dto.NutritionSummaryDto
import com.momentum.android.network.dto.UpdateNutritionEntryRequest

class NutritionRepository(private val api: MomentumApi) {
    suspend fun list(from: String? = null, to: String? = null): List<NutritionEntryDto> =
        api.getNutritionEntries(from, to)

    suspend fun summary(date: String? = null): NutritionSummaryDto = api.getNutritionSummary(date)

    suspend fun summaryRange(from: String, to: String): List<NutritionSummaryDto> =
        api.getNutritionSummaryRange(from, to)

    suspend fun energyBalance(granularity: EnergyBalanceGranularity): List<EnergyBalancePointDto> =
        api.getEnergyBalanceSeries(granularity.wireValue)

    suspend fun create(body: CreateNutritionEntryRequest): NutritionEntryDto = api.createNutritionEntry(body)

    suspend fun update(id: String, body: UpdateNutritionEntryRequest): NutritionEntryDto =
        api.updateNutritionEntry(id, body)

    suspend fun delete(id: String) = api.deleteNutritionEntry(id)
}
