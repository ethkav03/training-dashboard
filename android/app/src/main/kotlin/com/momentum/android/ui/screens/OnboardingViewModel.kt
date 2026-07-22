package com.momentum.android.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.momentum.android.auth.TokenStore
import com.momentum.android.data.UserRepository
import com.momentum.android.network.ApiClient
import com.momentum.android.network.dto.OnboardingRequest
import com.momentum.android.network.dto.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

/** Mirrors frontend/src/pages/OnboardingPage.tsx's submit/skip actions. */
class OnboardingViewModel(private val repository: UserRepository) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun complete(request: OnboardingRequest, onSuccess: (UserDto) -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            runCatching { repository.completeOnboarding(request) }
                .onSuccess { user ->
                    _state.value = _state.value.copy(isSaving = false)
                    onSuccess(user)
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Couldn't save your details.",
                    )
                }
        }
    }

    fun skip(onSuccess: (UserDto) -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            runCatching { repository.skipOnboarding() }
                .onSuccess { user ->
                    _state.value = _state.value.copy(isSaving = false)
                    onSuccess(user)
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Couldn't skip onboarding.",
                    )
                }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val api = ApiClient.create(TokenStore(context))
            @Suppress("UNCHECKED_CAST")
            return OnboardingViewModel(UserRepository(api)) as T
        }
    }
}
