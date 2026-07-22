package com.momentum.android.data

import com.momentum.android.network.MomentumApi
import com.momentum.android.network.dto.TimelineEntryDto

class TimelineRepository(private val api: MomentumApi) {
    // Unlike most other range queries, from/to are both required here --
    // matches the backend's timelineQuerySchema, which has no defaults.
    suspend fun range(from: String, to: String): List<TimelineEntryDto> = api.getTimeline(from, to)
}
