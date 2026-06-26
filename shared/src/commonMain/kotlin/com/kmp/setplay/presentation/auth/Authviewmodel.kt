package com.kmp.setplay.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmp.setplay.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    val isAnonymous: Boolean = false
)

sealed interface AuthAction {
    data object SignInAnonymously : AuthAction
    data object SignInWithGoogle  : AuthAction
    data object LinkGoogle        : AuthAction
    data object SignOut           : AuthAction
    data object DismissError      : AuthAction
    data class  SetError(val message: String) : AuthAction  // used by GoogleSignInButton.android.kt
}

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.isLoggedIn.collect { loggedIn ->
                _uiState.update { it.copy(isAuthenticated = loggedIn) }
            }
        }
        viewModelScope.launch {
            authRepository.isAnonymous.collect { anonymous ->
                _uiState.update { it.copy(isAnonymous = anonymous) }
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
            is AuthAction.SetError       -> _uiState.update { it.copy(error = action.message) }
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

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            block()
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}