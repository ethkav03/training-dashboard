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
        // re-fetched (and needs validating -- see refresh).
        if (repository.storedToken != null) refresh()
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
                            refresh()
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

    /**
     * Adopts a fresh UserDto a mutation endpoint already returned (profile
     * update, onboarding complete/skip) -- avoids a redundant GET /users/me
     * just to pick up the same data the mutation's response already has.
     */
    fun setUser(user: UserDto) {
        _state.value = _state.value.copy(user = user)
    }

    /**
     * Re-fetches the current user -- used on cold start (a stored token
     * needs revalidating) and right after sign-in. Profile/onboarding
     * mutations don't call this: they already get a fresh UserDto back from
     * the mutation itself and hand it to `setUser` instead, skipping a
     * redundant round-trip.
     */
    fun refresh() {
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
