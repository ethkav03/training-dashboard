package com.momentum.android.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.momentum.android.auth.TokenStore
import com.momentum.android.data.DashboardRepository
import com.momentum.android.network.ApiClient
import com.momentum.android.network.dto.DashboardTodayDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TodayUiState(
    val data: DashboardTodayDto? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class TodayViewModel(private val repository: DashboardRepository) : ViewModel() {
    private val _state = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching { repository.today() }
                .onSuccess { data -> _state.value = _state.value.copy(data = data, isLoading = false) }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Couldn't load today's data.",
                    )
                }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val api = ApiClient.create(TokenStore(context))
            @Suppress("UNCHECKED_CAST")
            return TodayViewModel(DashboardRepository(api)) as T
        }
    }
}
