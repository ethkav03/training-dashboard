package com.momentum.android.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.momentum.android.auth.TokenStore
import com.momentum.android.data.TrainingRepository
import com.momentum.android.network.ApiClient
import com.momentum.android.network.dto.LoadSummaryDto
import com.momentum.android.network.dto.TrainingSessionDto
import com.momentum.android.network.dto.TrainingSessionWriteDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TrainingUiState(
    val sessions: List<TrainingSessionDto> = emptyList(),
    val loadSummary: LoadSummaryDto? = null,
    val exerciseNames: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class TrainingViewModel(private val repository: TrainingRepository) : ViewModel() {
    private val _state = MutableStateFlow(TrainingUiState())
    val state: StateFlow<TrainingUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                val sessions = repository.list()
                val loadSummary = repository.loadSummary()
                val exerciseNames = repository.exerciseNames()
                Triple(sessions, loadSummary, exerciseNames)
            }.onSuccess { (sessions, loadSummary, exerciseNames) ->
                _state.value = _state.value.copy(
                    sessions = sessions,
                    loadSummary = loadSummary,
                    exerciseNames = exerciseNames,
                    isLoading = false,
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(isLoading = false, errorMessage = error.message ?: "Couldn't load training data.")
            }
        }
    }

    fun create(body: TrainingSessionWriteDto, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.create(body) }
                .onSuccess { refresh(); onDone() }
                .onFailure { error -> _state.value = _state.value.copy(errorMessage = error.message ?: "Couldn't save this session.") }
        }
    }

    fun replace(id: String, body: TrainingSessionWriteDto, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.replace(id, body) }
                .onSuccess { refresh(); onDone() }
                .onFailure { error -> _state.value = _state.value.copy(errorMessage = error.message ?: "Couldn't save this session.") }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            runCatching { repository.delete(id) }.onSuccess { refresh() }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val api = ApiClient.create(TokenStore(context))
            @Suppress("UNCHECKED_CAST")
            return TrainingViewModel(TrainingRepository(api)) as T
        }
    }
}
