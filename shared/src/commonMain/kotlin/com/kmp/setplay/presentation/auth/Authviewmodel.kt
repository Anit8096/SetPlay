package com.kmp.setplay.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmp.setplay.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── State ─────────────────────────────────────────────────────────────────────
data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    /** True once a session (any kind) has been established. */
    val isAuthenticated: Boolean = false,
    /** True when the current user is anonymous (can be upgraded). */
    val isAnonymous: Boolean = false
)

// ── Actions ───────────────────────────────────────────────────────────────────
sealed interface AuthAction {
    data object SignInAnonymously : AuthAction
    data object SignInWithGoogle  : AuthAction
    data object LinkGoogle        : AuthAction
    data object SignOut           : AuthAction
    data object DismissError      : AuthAction
}

// ── ViewModel ─────────────────────────────────────────────────────────────────
class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Keep authentication state in sync with the Supabase session.
        viewModelScope.launch {
            authRepository.isLoggedIn.collect { loggedIn ->
                _uiState.update { it.copy(isAuthenticated = loggedIn) }
            }
        }
    }

    fun onAction(action: AuthAction) {
        when (action) {
            AuthAction.SignInAnonymously -> signInAnonymously()
            AuthAction.SignInWithGoogle  -> signInWithGoogle()
            AuthAction.LinkGoogle        -> linkGoogle()
            AuthAction.SignOut           -> signOut()
            AuthAction.DismissError      -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun signInAnonymously() = launch {
        authRepository.signInAnonymously().onFailure { e ->
            _uiState.update { it.copy(error = e.message ?: "Sign-in failed") }
        }
    }

    private fun signInWithGoogle() = launch {
        authRepository.signInWithGoogle().onFailure { e ->
            _uiState.update { it.copy(error = e.message ?: "Google sign-in failed") }
        }
    }

    private fun linkGoogle() = launch {
        authRepository.linkGoogle().onFailure { e ->
            _uiState.update { it.copy(error = e.message ?: "Account linking failed") }
        }
    }

    private fun signOut() = launch {
        authRepository.signOut().onFailure { e ->
            _uiState.update { it.copy(error = e.message ?: "Sign-out failed") }
        }
    }

    /** Convenience wrapper that sets isLoading around the suspend block. */
    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            block()
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}