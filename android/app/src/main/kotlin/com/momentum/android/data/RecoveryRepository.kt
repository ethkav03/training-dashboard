package com.momentum.android.data

import com.momentum.android.network.MomentumApi
import com.momentum.android.network.dto.RecoveryRecordDto
import com.momentum.android.network.dto.UpsertRecoveryRecordRequest

class RecoveryRepository(private val api: MomentumApi) {
    suspend fun today(): RecoveryRecordDto? = api.getRecoveryToday()

    suspend fun history(from: String? = null, to: String? = null): List<RecoveryRecordDto> =
        api.getRecoveryHistory(from, to)

    suspend fun upsert(body: UpsertRecoveryRecordRequest): RecoveryRecordDto = api.upsertRecoveryRecord(body)

    suspend fun delete(id: String) = api.deleteRecoveryRecord(id)
}
