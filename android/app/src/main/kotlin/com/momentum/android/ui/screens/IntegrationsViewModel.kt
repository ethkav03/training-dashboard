package com.momentum.android.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.momentum.android.auth.TokenStore
import com.momentum.android.data.IntegrationsRepository
import com.momentum.android.network.ApiClient
import com.momentum.android.network.dto.IntegrationConnectionDto
import com.momentum.android.network.dto.WhoopSyncResultDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class IntegrationsUiState(
    val connections: List<IntegrationConnectionDto> = emptyList(),
    val isSyncingWhoop: Boolean = false,
    val lastWhoopResult: WhoopSyncResultDto? = null,
    val errorMessage: String? = null,
)

class IntegrationsViewModel(private val repository: IntegrationsRepository) : ViewModel() {
    private val _state = MutableStateFlow(IntegrationsUiState())
    val state: StateFlow<IntegrationsUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            runCatching { repository.list() }
                .onSuccess { connections -> _state.value = _state.value.copy(connections = connections) }
        }
    }

    fun syncWhoopNow() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSyncingWhoop = true, errorMessage = null)
            runCatching { repository.syncWhoop() }
                .onSuccess { result ->
                    _state.value = _state.value.copy(isSyncingWhoop = false, lastWhoopResult = result)
                    refresh()
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(isSyncingWhoop = false, errorMessage = error.message ?: "Sync failed.")
                }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val api = ApiClient.create(TokenStore(context))
            @Suppress("UNCHECKED_CAST")
            return IntegrationsViewModel(IntegrationsRepository(api)) as T
        }
    }
}
