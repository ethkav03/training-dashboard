package com.momentum.android.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.momentum.android.auth.TokenStore
import com.momentum.android.data.RecoveryRepository
import com.momentum.android.network.ApiClient
import com.momentum.android.network.dto.RecoveryRecordDto
import com.momentum.android.network.dto.UpsertRecoveryRecordRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecoveryUiState(
    val today: RecoveryRecordDto? = null,
    val history: List<RecoveryRecordDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class RecoveryViewModel(private val repository: RecoveryRepository) : ViewModel() {
    private val _state = MutableStateFlow(RecoveryUiState())
    val state: StateFlow<RecoveryUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                val today = repository.today()
                val history = repository.history()
                today to history
            }.onSuccess { (today, history) ->
                _state.value = _state.value.copy(today = today, history = history, isLoading = false)
            }.onFailure { error ->
                _state.value = _state.value.copy(isLoading = false, errorMessage = error.message ?: "Couldn't load recovery data.")
            }
        }
    }

    fun logRecovery(body: UpsertRecoveryRecordRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.upsert(body) }
                .onSuccess { refresh(); onDone() }
                .onFailure { error -> _state.value = _state.value.copy(errorMessage = error.message ?: "Couldn't save.") }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val api = ApiClient.create(TokenStore(context))
            @Suppress("UNCHECKED_CAST")
            return RecoveryViewModel(RecoveryRepository(api)) as T
        }
    }
}
