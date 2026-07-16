package com.kmp.setplay.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmp.setplay.domain.model.Tournament
import com.kmp.setplay.domain.model.TournamentStatus
import com.kmp.setplay.domain.repository.AuthRepository
import com.kmp.setplay.domain.repository.TournamentRepository
import com.kmp.setplay.presentation.common.SCREEN_ENTER_SETTLE_DELAY_MS
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HistorySubTab { PRIVATE, JOINED }

data class HistoryUiState(
    val selectedSubTab: HistorySubTab = HistorySubTab.PRIVATE,
    // Private tournaments started by me, split by lifecycle. "Active" includes
    // DRAFT/REGISTRATION/IN_PROGRESS — private tournaments have no other list
    // surface in the app (Browse → Organizing is public-only), so History is
    // where you come back to a bracket you started earlier.
    val active: List<Tournament> = emptyList(),
    val completed: List<Tournament> = emptyList(),
    val isLoading: Boolean = true
)

sealed interface HistoryAction {
    data class SubTabSelected(val tab: HistorySubTab) : HistoryAction
}

class HistoryViewModel(
    private val tournamentRepository: TournamentRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            delay(SCREEN_ENTER_SETTLE_DELAY_MS)
            observeMyPrivateTournaments()
        }
    }

    fun onAction(action: HistoryAction) {
        when (action) {
            is HistoryAction.SubTabSelected ->
                _uiState.update { it.copy(selectedSubTab = action.tab) }
        }
    }

    private fun observeMyPrivateTournaments() {
        viewModelScope.launch {
            authRepository.currentUserId
                .filterNotNull()
                .collectLatest { userId ->
                    tournamentRepository.observeMyTournaments(userId).collectLatest { all ->
                        val mine = all
                            .filter { !it.isPublic }
                            .sortedByDescending { it.createdAt }
                        _uiState.update { state ->
                            state.copy(
                                active = mine.filter { it.status != TournamentStatus.COMPLETED },
                                completed = mine.filter { it.status == TournamentStatus.COMPLETED },
                                isLoading = false
                            )
                        }
                    }
                }
        }
    }
}
