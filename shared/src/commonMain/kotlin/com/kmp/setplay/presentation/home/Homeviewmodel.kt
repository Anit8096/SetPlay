package com.kmp.setplay.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmp.setplay.domain.model.Tournament
import com.kmp.setplay.domain.repository.AuthRepository
import com.kmp.setplay.domain.repository.TournamentRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val tournaments: List<Tournament> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed interface HomeAction {
    data object DismissError : HomeAction
    data object Refresh : HomeAction
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val authRepository: AuthRepository,
    private val tournamentRepository: TournamentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Whenever the logged-in user changes, re-subscribe to their tournaments.
            authRepository.currentUserId
                .flatMapLatest { userId ->
                    if (userId == null) flowOf(emptyList())
                    else tournamentRepository.observeMyTournaments(userId)
                }
                .collect { tournaments ->
                    _uiState.update {
                        it.copy(tournaments = tournaments, isLoading = false)
                    }
                }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.DismissError -> _uiState.update { it.copy(error = null) }
            HomeAction.Refresh -> _uiState.update { it.copy(isLoading = true) }
        }
    }
}