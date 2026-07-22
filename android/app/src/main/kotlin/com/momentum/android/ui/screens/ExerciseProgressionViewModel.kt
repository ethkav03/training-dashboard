package com.momentum.android.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.momentum.android.auth.TokenStore
import com.momentum.android.data.TrainingRepository
import com.momentum.android.network.ApiClient
import com.momentum.android.network.dto.ExerciseProgressionPointDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExerciseProgressionUiState(
    val points: List<ExerciseProgressionPointDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class ExerciseProgressionViewModel(private val repository: TrainingRepository) : ViewModel() {
    private val _state = MutableStateFlow(ExerciseProgressionUiState())
    val state: StateFlow<ExerciseProgressionUiState> = _state.asStateFlow()

    fun load(exerciseName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching { repository.exerciseProgression(exerciseName) }
                .onSuccess { points -> _state.value = _state.value.copy(points = points, isLoading = false) }
                .onFailure { error ->
                    _state.value = _state.value.copy(isLoading = false, errorMessage = error.message ?: "Couldn't load progression.")
                }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val api = ApiClient.create(TokenStore(context))
            @Suppress("UNCHECKED_CAST")
            return ExerciseProgressionViewModel(TrainingRepository(api)) as T
        }
    }
}
