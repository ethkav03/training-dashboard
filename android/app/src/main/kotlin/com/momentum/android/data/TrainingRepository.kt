package com.momentum.android.data

import com.momentum.android.network.MomentumApi
import com.momentum.android.network.dto.ActivityType
import com.momentum.android.network.dto.ExerciseProgressionPointDto
import com.momentum.android.network.dto.LoadSummaryDto
import com.momentum.android.network.dto.LoadTrendPointDto
import com.momentum.android.network.dto.TrainingSessionDto
import com.momentum.android.network.dto.TrainingSessionWriteDto

class TrainingRepository(private val api: MomentumApi) {
    suspend fun exerciseNames(): List<String> = api.getExerciseNames()

    suspend fun exerciseProgression(name: String): List<ExerciseProgressionPointDto> = api.getExerciseProgression(name)

    suspend fun loadTrend(from: String? = null, to: String? = null, type: ActivityType? = null): List<LoadTrendPointDto> =
        api.getLoadTrend(from, to, type)

    suspend fun loadSummary(): LoadSummaryDto = api.getLoadSummary()

    suspend fun list(from: String? = null, to: String? = null, type: ActivityType? = null): List<TrainingSessionDto> =
        api.getTrainingSessions(from, to, type)

    suspend fun get(id: String): TrainingSessionDto = api.getTrainingSession(id)

    suspend fun create(body: TrainingSessionWriteDto): TrainingSessionDto = api.createTrainingSession(body)

    suspend fun replace(id: String, body: TrainingSessionWriteDto): TrainingSessionDto =
        api.replaceTrainingSession(id, body)

    suspend fun delete(id: String) = api.deleteTrainingSession(id)
}
