package com.momentum.android.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.momentum.android.auth.TokenStore
import com.momentum.android.data.TimelineRepository
import com.momentum.android.network.ApiClient
import com.momentum.android.network.dto.TimelineEntryDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class TimelineUiState(
    val weekOffset: Long = 0,
    val entries: List<TimelineEntryDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class TimelineViewModel(private val repository: TimelineRepository) : ViewModel() {
    private val _state = MutableStateFlow(TimelineUiState())
    val state: StateFlow<TimelineUiState> = _state.asStateFlow()

    /** The week's [start, end] as LocalDates -- exposed for the screen's range label and day grouping. */
    fun weekRange(weekOffset: Long): Pair<LocalDate, LocalDate> {
        val rangeEnd = LocalDate.now().plusWeeks(weekOffset)
        return rangeEnd.minusDays(6) to rangeEnd
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val zone = ZoneId.systemDefault()
            val (rangeStart, rangeEnd) = weekRange(_state.value.weekOffset)
            val from = rangeStart.atStartOfDay(zone).toInstant().toString()
            val to = rangeEnd.atTime(LocalTime.MAX).atZone(zone).toInstant().toString()

            runCatching { repository.range(from, to) }
                .onSuccess { entries -> _state.value = _state.value.copy(entries = entries, isLoading = false) }
                .onFailure { error -> _state.value = _state.value.copy(isLoading = false, errorMessage = error.message ?: "Couldn't load timeline.") }
        }
    }

    fun goToPreviousWeek() {
        _state.value = _state.value.copy(weekOffset = _state.value.weekOffset - 1)
        refresh()
    }

    fun goToNextWeek() {
        _state.value = _state.value.copy(weekOffset = _state.value.weekOffset + 1)
        refresh()
    }

    fun goToToday() {
        _state.value = _state.value.copy(weekOffset = 0)
        refresh()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val api = ApiClient.create(TokenStore(context))
            @Suppress("UNCHECKED_CAST")
            return TimelineViewModel(TimelineRepository(api)) as T
        }
    }
}
