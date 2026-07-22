package com.momentum.android.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.momentum.android.auth.TokenStore
import com.momentum.android.data.NutritionRepository
import com.momentum.android.network.ApiClient
import com.momentum.android.network.dto.CreateNutritionEntryRequest
import com.momentum.android.network.dto.EnergyBalanceGranularity
import com.momentum.android.network.dto.EnergyBalancePointDto
import com.momentum.android.network.dto.NutritionEntryDto
import com.momentum.android.network.dto.NutritionSummaryDto
import com.momentum.android.network.dto.UpdateNutritionEntryRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class FuelUiState(
    val summary: NutritionSummaryDto? = null,
    val todayEntries: List<NutritionEntryDto> = emptyList(),
    val energyBalance: List<EnergyBalancePointDto> = emptyList(),
    val granularity: EnergyBalanceGranularity = EnergyBalanceGranularity.DAY,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class FuelViewModel(private val repository: NutritionRepository) : ViewModel() {
    private val _state = MutableStateFlow(FuelUiState())
    val state: StateFlow<FuelUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toString()
            val todayEnd = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toString()
            runCatching {
                val summary = repository.summary()
                val entries = repository.list(from = todayStart, to = todayEnd)
                val balance = repository.energyBalance(_state.value.granularity)
                Triple(summary, entries, balance)
            }.onSuccess { (summary, entries, balance) ->
                _state.value = _state.value.copy(
                    summary = summary,
                    todayEntries = entries,
                    energyBalance = balance,
                    isLoading = false,
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(isLoading = false, errorMessage = error.message ?: "Couldn't load Fuel data.")
            }
        }
    }

    fun setGranularity(granularity: EnergyBalanceGranularity) {
        _state.value = _state.value.copy(granularity = granularity)
        viewModelScope.launch {
            runCatching { repository.energyBalance(granularity) }
                .onSuccess { balance -> _state.value = _state.value.copy(energyBalance = balance) }
        }
    }

    fun create(body: CreateNutritionEntryRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.create(body) }
                .onSuccess { refresh(); onDone() }
                .onFailure { error -> _state.value = _state.value.copy(errorMessage = error.message ?: "Couldn't save.") }
        }
    }

    fun update(id: String, body: UpdateNutritionEntryRequest, onDone: () -> Unit) {
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
            return FuelViewModel(NutritionRepository(api)) as T
        }
    }
}
