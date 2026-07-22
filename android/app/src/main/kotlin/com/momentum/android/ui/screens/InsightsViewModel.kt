package com.momentum.android.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.momentum.android.auth.TokenStore
import com.momentum.android.data.InsightsRepository
import com.momentum.android.network.ApiClient
import com.momentum.android.network.dto.InsightDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InsightsUiState(
    val insights: List<InsightDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class InsightsViewModel(private val repository: InsightsRepository) : ViewModel() {
    private val _state = MutableStateFlow(InsightsUiState())
    val state: StateFlow<InsightsUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching { repository.list() }
                .onSuccess { insights -> _state.value = _state.value.copy(insights = insights, isLoading = false) }
                .onFailure { error -> _state.value = _state.value.copy(isLoading = false, errorMessage = error.message ?: "Couldn't load insights.") }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val api = ApiClient.create(TokenStore(context))
            @Suppress("UNCHECKED_CAST")
            return InsightsViewModel(InsightsRepository(api)) as T
        }
    }
}
