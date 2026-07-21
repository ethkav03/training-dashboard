package com.momentum.android.healthconnect

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.momentum.android.auth.TokenStore
import com.momentum.android.network.ApiClient
import com.momentum.android.network.HealthConnectSyncResultDto
import com.momentum.android.network.MomentumApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HealthConnectUiState(
    val availability: HealthConnectManager.Availability? = null,
    val permissionsGranted: Boolean = false,
    val isSyncing: Boolean = false,
    val lastResult: HealthConnectSyncResultDto? = null,
    val errorMessage: String? = null,
)

class HealthConnectViewModel(
    private val manager: HealthConnectManager,
    private val repository: HealthConnectRepository,
    private val api: MomentumApi,
) : ViewModel() {

    private val _state = MutableStateFlow(HealthConnectUiState(availability = manager.availability))
    val state: StateFlow<HealthConnectUiState> = _state.asStateFlow()

    val permissions: Set<String> get() = manager.permissions

    fun requestPermissionsContract() = manager.requestPermissionsContract()

    fun installOrUpdateIntent() = manager.installOrUpdateIntent()

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                availability = manager.availability,
                permissionsGranted = runCatching { manager.hasAllPermissions() }.getOrDefault(false),
            )
        }
    }

    fun onPermissionsResult(granted: Set<String>) {
        _state.value = _state.value.copy(permissionsGranted = granted.containsAll(permissions))
    }

    fun syncNow() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSyncing = true, errorMessage = null)
            runCatching {
                val since = HealthConnectRepository.defaultSince()
                val weight = repository.readWeightRecords(since)
                val exercise = repository.readExerciseSessions(since)
                val sleep = repository.readSleepSessions(since)
                api.syncHealthConnect(HealthConnectMapper.toSyncRequest(weight, exercise, sleep))
            }.onSuccess { result ->
                _state.value = _state.value.copy(isSyncing = false, lastResult = result)
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    isSyncing = false,
                    errorMessage = error.message ?: "Sync failed.",
                )
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val manager = HealthConnectManager(context)
            val tokenStore = TokenStore(context)
            @Suppress("UNCHECKED_CAST")
            return HealthConnectViewModel(
                manager = manager,
                repository = HealthConnectRepository(manager),
                api = ApiClient.create(tokenStore),
            ) as T
        }
    }
}
