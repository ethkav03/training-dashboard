package com.momentum.android.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.momentum.android.auth.TokenStore
import com.momentum.android.data.WeightRepository
import com.momentum.android.network.ApiClient
import com.momentum.android.network.dto.CreateWeightEntryRequest
import com.momentum.android.network.dto.UpdateWeightEntryRequest
import com.momentum.android.network.dto.WeightTrendDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BodyUiState(
    val trend: WeightTrendDto? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class BodyViewModel(private val repository: WeightRepository) : ViewModel() {
    private val _state = MutableStateFlow(BodyUiState())
    val state: StateFlow<BodyUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching { repository.trend() }
                .onSuccess { trend -> _state.value = _state.value.copy(trend = trend, isLoading = false) }
                .onFailure { error ->
                    _state.value = _state.value.copy(isLoading = false, errorMessage = error.message ?: "Couldn't load weight data.")
                }
        }
    }

    fun create(body: CreateWeightEntryRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.create(body) }
                .onSuccess { refresh(); onDone() }
                .onFailure { error -> _state.value = _state.value.copy(errorMessage = error.message ?: "Couldn't save.") }
        }
    }

    fun update(id: String, body: UpdateWeightEntryRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.update(id, body) }
                .onSuccess { refresh(); onDone() }
                .onFailure { error -> _state.value = _state.value.copy(errorMessage = error.message ?: "Couldn't save.") }
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
            return BodyViewModel(WeightRepository(api)) as T
        }
    }
}
