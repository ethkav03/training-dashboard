package com.momentum.android.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.momentum.android.network.ApiClient
import com.momentum.android.network.dto.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val token: String? = null,
    val user: UserDto? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class AuthViewModel(
    private val googleSignInManager: GoogleSignInManager,
    private val repository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState(token = repository.storedToken))
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        // A stored token from a previous session still needs the user
        // re-fetched (and needs validating -- see refreshCurrentUser).
        if (repository.storedToken != null) refreshCurrentUser()
    }

    fun signIn() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            when (val result = googleSignInManager.signIn()) {
                is GoogleSignInResult.Failure -> {
                    _state.value = _state.value.copy(isLoading = false, errorMessage = result.message)
                }
                is GoogleSignInResult.Success -> {
                    repository.exchangeIdToken(result.idToken)
                        .onSuccess { token ->
                            _state.value = _state.value.copy(token = token, isLoading = false)
                            refreshCurrentUser()
                        }
                        .onFailure { error ->
                            _state.value = _state.value.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Couldn't sign in to Momentum.",
                            )
                        }
                }
            }
        }
    }

    fun signOut() {
        repository.signOut()
        _state.value = AuthUiState()
    }

    private fun refreshCurrentUser() {
        viewModelScope.launch {
            repository.fetchCurrentUser()
                .onSuccess { user -> _state.value = _state.value.copy(user = user) }
                .onFailure {
                    // Stored token is invalid/expired server-side -- fall
                    // back to a clean signed-out state rather than getting
                    // stuck showing a token with no matching user.
                    repository.signOut()
                    _state.value = AuthUiState()
                }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val tokenStore = TokenStore(context)
            val api = ApiClient.create(tokenStore)
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(
                googleSignInManager = GoogleSignInManager(context),
                repository = AuthRepository(api, tokenStore),
            ) as T
        }
    }
}
