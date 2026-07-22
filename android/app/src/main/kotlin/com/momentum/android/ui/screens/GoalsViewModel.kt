package com.momentum.android.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.momentum.android.auth.TokenStore
import com.momentum.android.data.GoalRepository
import com.momentum.android.network.ApiClient
import com.momentum.android.network.dto.CreateGoalRequest
import com.momentum.android.network.dto.GoalDto
import com.momentum.android.network.dto.GoalStatus
import com.momentum.android.network.dto.UpdateGoalRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GoalsUiState(
    val goals: List<GoalDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class GoalsViewModel(private val repository: GoalRepository) : ViewModel() {
    private val _state = MutableStateFlow(GoalsUiState())
    val state: StateFlow<GoalsUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching { repository.list() }
                .onSuccess { goals -> _state.value = _state.value.copy(goals = goals, isLoading = false) }
                .onFailure { error -> _state.value = _state.value.copy(isLoading = false, errorMessage = error.message ?: "Couldn't load goals.") }
        }
    }

    fun create(body: CreateGoalRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.create(body) }
                .onSuccess { refresh(); onDone() }
                .onFailure { error -> _state.value = _state.value.copy(errorMessage = error.message ?: "Couldn't save.") }
        }
    }

    fun togglePause(goal: GoalDto) {
        val newStatus = if (goal.status == GoalStatus.PAUSED) GoalStatus.ON_TRACK else GoalStatus.PAUSED
        viewModelScope.launch {
            runCatching { repository.update(goal.id, UpdateGoalRequest(status = newStatus)) }
                .onSuccess { refresh() }
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
            return GoalsViewModel(GoalRepository(api)) as T
        }
    }
}
