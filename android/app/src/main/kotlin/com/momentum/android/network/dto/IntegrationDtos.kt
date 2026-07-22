package com.momentum.android.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class IntegrationConnectionDto(
    val provider: IntegrationProvider,
    val configured: Boolean,
    val connected: Boolean,
    val connectedAt: String? = null,
    val lastSyncAt: String? = null,
    val lastSyncStatus: SyncStatus,
    val lastSyncError: String? = null,
)

// WHOOP connect/disconnect stay web-only (Android shows read-only status --
// see docs/architecture.md's Native Android app section for why); this DTO
// exists only so a future WHOOP status display can parse the same shape the
// web app already reads from GET /integrations.
@Serializable
data class WhoopSyncResultDto(
    val status: String,
    val syncedAt: String,
    val recoveryRecordsSynced: Int,
    val recoveryRecordsSkippedManualEdit: Int,
    val trainingSessionsSynced: Int,
    val errorMessage: String? = null,
)
