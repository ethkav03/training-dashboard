package com.momentum.android.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class TimelineEntryDto(
    val date: String,
    val kind: TimelineEntryKind,
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val refId: String,
)
