package com.kmp.setplay.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmp.setplay.domain.repository.AuthRepository
import com.kmp.setplay.domain.repository.CurrentUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: CurrentUser? = null,
    val isLoading: Boolean = true,
    /** Sign-out is destructive for guests (their data is unrecoverable), so it's confirmed. */
    val showSignOutConfirm: Boolean = false,
    val message: String? = null
)

sealed interface ProfileAction {
    data object SignOutClicked : ProfileAction
    data object ConfirmSignOut : ProfileAction
    data object DismissSignOutConfirm : ProfileAction
    data object CopyPlayerId : ProfileAction
    data object MessageShown : ProfileAction
}

/**
 * Backs the Profile tab. Reads identity straight off the auth session — there's no
 * `profiles` table, so there is nothing to fetch and nothing to write back.
 *
 * Sign-out and account-linking are *not* handled here: they mutate root-level auth state
 * that `NavGraph` gates on, so they're delegated up to the shared root-scoped
 * `AuthViewModel` via the `onAuthAction` callback the screen already receives. This VM
 * only owns the screen-local bits (confirm dialog, snackbar).
 */
class ProfileViewModel(
    authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(user = user, isLoading = false) }
            }
        }
    }

    /**
     * Returns true when the caller should actually perform the sign-out
     * (i.e. the user confirmed), letting the screen forward it to AuthViewModel.
     */
    fun onAction(action: ProfileAction): Boolean {
        when (action) {
            ProfileAction.SignOutClicked ->
                _uiState.update { it.copy(showSignOutConfirm = true) }

            ProfileAction.DismissSignOutConfirm ->
                _uiState.update { it.copy(showSignOutConfirm = false) }

            ProfileAction.ConfirmSignOut -> {
                _uiState.update { it.copy(showSignOutConfirm = false) }
                return true
            }

            ProfileAction.CopyPlayerId ->
                _uiState.update { it.copy(message = "Player ID copied") }

            ProfileAction.MessageShown ->
                _uiState.update { it.copy(message = null) }
        }
        return false
    }
}
