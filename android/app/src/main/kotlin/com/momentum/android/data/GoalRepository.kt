package com.momentum.android.data

import com.momentum.android.network.MomentumApi
import com.momentum.android.network.dto.CreateGoalRequest
import com.momentum.android.network.dto.GoalDto
import com.momentum.android.network.dto.UpdateGoalRequest

class GoalRepository(private val api: MomentumApi) {
    suspend fun list(status: String? = null, type: String? = null): List<GoalDto> = api.getGoals(status, type)

    suspend fun get(id: String): GoalDto = api.getGoal(id)

    suspend fun create(body: CreateGoalRequest): GoalDto = api.createGoal(body)

    suspend fun update(id: String, body: UpdateGoalRequest): GoalDto = api.updateGoal(id, body)

    suspend fun delete(id: String) = api.deleteGoal(id)
}
