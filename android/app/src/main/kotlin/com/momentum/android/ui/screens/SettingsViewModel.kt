package com.momentum.android.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.momentum.android.auth.TokenStore
import com.momentum.android.data.UserRepository
import com.momentum.android.network.ApiClient
import com.momentum.android.network.dto.UnitSystem
import com.momentum.android.network.dto.UpdateProfileRequest
import com.momentum.android.network.dto.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.ResponseBody

data class SettingsUiState(
    val isSavingUnits: Boolean = false,
    val isSavingBurnBaseline: Boolean = false,
    val isExporting: Boolean = false,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * Wraps the Profile-adjacent mutations SettingsScreen needs (units, energy
 * baseline, export, delete-account). AuthViewModel still owns the
 * *displayed* user -- every mutation here already returns the fresh UserDto,
 * so `onUserUpdated` pushes it straight into AuthViewModel instead of paying
 * for a second GET /users/me round-trip.
 */
class SettingsViewModel(private val repository: UserRepository) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun updateUnitSystem(unitSystem: UnitSystem, onUserUpdated: (UserDto) -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSavingUnits = true, errorMessage = null)
            runCatching { repository.updateProfile(UpdateProfileRequest(unitSystem = unitSystem)) }
                .onSuccess { user ->
                    _state.value = _state.value.copy(isSavingUnits = false)
                    onUserUpdated(user)
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isSavingUnits = false,
                        errorMessage = error.message ?: "Couldn't save units.",
                    )
                }
        }
    }

    fun updateBurnBaseline(kcal: Int, onUserUpdated: (UserDto) -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSavingBurnBaseline = true, errorMessage = null)
            runCatching { repository.updateProfile(UpdateProfileRequest(estimatedDailyBurnKcal = kcal)) }
                .onSuccess { user ->
                    _state.value = _state.value.copy(isSavingBurnBaseline = false)
                    onUserUpdated(user)
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isSavingBurnBaseline = false,
                        errorMessage = error.message ?: "Couldn't save energy baseline.",
                    )
                }
        }
    }

    fun exportData(onResult: (Result<ResponseBody>) -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isExporting = true, errorMessage = null)
            val result = runCatching { repository.exportData() }
            _state.value = _state.value.copy(
                isExporting = false,
                errorMessage = result.exceptionOrNull()?.let { it.message ?: "Couldn't export your data." },
            )
            onResult(result)
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isDeleting = true, errorMessage = null)
            runCatching { repository.deleteAccount() }
                .onSuccess { onSuccess() }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isDeleting = false,
                        errorMessage = error.message ?: "Couldn't delete account.",
                    )
                }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val api = ApiClient.create(TokenStore(context))
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(UserRepository(api)) as T
        }
    }
}
