package com.momentum.android.healthconnect

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.momentum.android.auth.TokenStore
import com.momentum.android.network.ApiClient
import com.momentum.android.network.HealthConnectSyncResultDto
import com.momentum.android.network.MomentumApi
import com.momentum.android.sync.SyncScheduler
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
    private val appContext: Context,
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
            val granted = runCatching { manager.hasAllPermissions() }.getOrDefault(false)
            _state.value = _state.value.copy(availability = manager.availability, permissionsGranted = granted)
            // Idempotent (KEEP policy) -- safe to call every time this
            // screen confirms permissions are still granted, e.g. on every
            // app open, not just the first time they're granted.
            if (granted) SyncScheduler.schedule(appContext)
        }
    }

    fun onPermissionsResult(granted: Set<String>) {
        val allGranted = granted.containsAll(permissions)
        _state.value = _state.value.copy(permissionsGranted = allGranted)
        if (allGranted) SyncScheduler.schedule(appContext)
    }

    fun syncNow() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSyncing = true, errorMessage = null)
            runCatching {
                val bounded = repository.readBounded(HealthConnectRepository.defaultSince())
                api.syncHealthConnect(HealthConnectMapper.toSyncRequest(bounded))
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
                appContext = context.applicationContext,
                manager = manager,
                repository = HealthConnectRepository(manager),
                api = ApiClient.create(tokenStore),
            ) as T
        }
    }
}
