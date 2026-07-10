package com.kmp.setplay.presentation.tournament.join

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmp.setplay.domain.model.Tournament
import com.kmp.setplay.domain.repository.TournamentRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val SCREEN_ENTER_SETTLE_DELAY_MS = 300L

data class JoinTournamentUiState(
    val codeInput: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val foundTournament: Tournament? = null
)

sealed interface JoinTournamentAction {
    data class CodeChanged(val code: String) : JoinTournamentAction
    data object Search : JoinTournamentAction
    data object DismissError : JoinTournamentAction
}

// ── ViewModel ─────────────────────────────────────────────────────────────────
class JoinTournamentViewModel(
    initialCode: String?,
    private val tournamentRepository: TournamentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        // Uppercase here too — manual entry always uppercases via CodeChanged, so a
        // lowercase deep-link code would otherwise silently fail to match.
        JoinTournamentUiState(codeInput = initialCode?.uppercase() ?: "")
    )
    val uiState: StateFlow<JoinTournamentUiState> = _uiState.asStateFlow()

    init {
        // Auto-search if launched from a deep link with a code. Delayed so the push
        // transition into this screen settles before the network call starts; the
        // manual Search action (onAction below) stays immediate since it's user-initiated.
        if (!initialCode.isNullOrBlank()) {
            viewModelScope.launch {
                delay(SCREEN_ENTER_SETTLE_DELAY_MS)
                search()
            }
        }
    }

    fun onAction(action: JoinTournamentAction) {
        when (action) {
            is JoinTournamentAction.CodeChanged ->
                _uiState.update { it.copy(codeInput = action.code.uppercase()) }
            JoinTournamentAction.Search -> search()
            JoinTournamentAction.DismissError ->
                _uiState.update { it.copy(error = null) }
        }
    }

    private fun search() {
        val code = _uiState.value.codeInput.trim()
        if (code.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            tournamentRepository.getTournamentByInviteCode(code)
                .onSuccess { tournament ->
                    _uiState.update { it.copy(isLoading = false, foundTournament = tournament) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Tournament not found — check your code"
                    ) }
                }
        }
    }
}