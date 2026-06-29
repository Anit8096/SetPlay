package com.kmp.setplay.presentation.tournament.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmp.setplay.domain.model.Announcement
import com.kmp.setplay.domain.model.Match
import com.kmp.setplay.domain.model.OrganizerRole
import com.kmp.setplay.domain.model.Round
import com.kmp.setplay.domain.model.Standing
import com.kmp.setplay.domain.model.Team
import com.kmp.setplay.domain.model.Tournament
import com.kmp.setplay.domain.repository.AuthRepository
import com.kmp.setplay.domain.repository.TournamentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── State ─────────────────────────────────────────────────────────────────────

data class TournamentDetailUiState(
    val tournament: Tournament? = null,
    val teams: List<Team> = emptyList(),
    val matches: List<Match> = emptyList(),
    val standings: List<Standing> = emptyList(),
    val announcements: List<Announcement> = emptyList(),
    val selectedTab: DetailTab = DetailTab.BRACKET,
    val isLoading: Boolean = true,
    val error: String? = null,
    // null = still loading, non-null = resolved
    val organizerRole: OrganizerRole? = null,
    val isOrganizer: Boolean = false,
    // Score entry dialog
    val scoringMatch: Match? = null,
    val score1Input: String = "",
    val score2Input: String = ""
)

enum class DetailTab { BRACKET, STANDINGS, ANNOUNCEMENTS }

// ── Actions ───────────────────────────────────────────────────────────────────
sealed interface TournamentDetailAction {
    data class TabSelected(val tab: DetailTab) : TournamentDetailAction
    data class MatchClicked(val match: Match) : TournamentDetailAction
    data class Score1Changed(val score: String) : TournamentDetailAction
    data class Score2Changed(val score: String) : TournamentDetailAction
    data object SubmitScore : TournamentDetailAction
    data object DismissScoreDialog : TournamentDetailAction
    data object DismissError : TournamentDetailAction
}

// ── ViewModel ─────────────────────────────────────────────────────────────────
class TournamentDetailViewModel(
    private val tournamentId: String,
    private val authRepository: AuthRepository,
    private val tournamentRepository: TournamentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TournamentDetailUiState())
    val uiState: StateFlow<TournamentDetailUiState> = _uiState.asStateFlow()

    init {
        observeAll()
        fetchOrganizerRole()
    }

    private fun fetchOrganizerRole() {
        viewModelScope.launch {
            val userId = authRepository.currentUserId.first() ?: return@launch
            tournamentRepository.getOrganizerRole(tournamentId, userId)
                .onSuccess { role ->
                    _uiState.update {
                        it.copy(
                            organizerRole = role,
                            isOrganizer = role != null
                        )
                    }
                }
        }
    }

    private fun observeAll() {
        viewModelScope.launch {
            combine(
                tournamentRepository.observeTournament(tournamentId),
                tournamentRepository.observeTeams(tournamentId),
                tournamentRepository.observeMatches(tournamentId),
                tournamentRepository.observeStandings(tournamentId),
                tournamentRepository.observeAnnouncements(tournamentId)
            ) { tournament, teams, matches, standings, announcements ->
                TournamentDetailUiState(
                    tournament = tournament,
                    teams = teams,
                    matches = matches,
                    standings = standings,
                    announcements = announcements,
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.update { current ->
                    newState.copy(
                        selectedTab = current.selectedTab,
                        scoringMatch = current.scoringMatch,
                        score1Input = current.score1Input,
                        score2Input = current.score2Input
                    )
                }
            }
        }
    }

    fun onAction(action: TournamentDetailAction) {
        when (action) {
            is TournamentDetailAction.TabSelected ->
                _uiState.update { it.copy(selectedTab = action.tab) }

            is TournamentDetailAction.MatchClicked ->
                _uiState.update { it.copy(
                    scoringMatch = action.match,
                    score1Input = action.match.score1?.toString() ?: "",
                    score2Input = action.match.score2?.toString() ?: ""
                ) }

            is TournamentDetailAction.Score1Changed ->
                _uiState.update { it.copy(score1Input = action.score) }

            is TournamentDetailAction.Score2Changed ->
                _uiState.update { it.copy(score2Input = action.score) }

            TournamentDetailAction.SubmitScore -> submitScore()

            TournamentDetailAction.DismissScoreDialog ->
                _uiState.update { it.copy(scoringMatch = null, score1Input = "", score2Input = "") }

            TournamentDetailAction.DismissError ->
                _uiState.update { it.copy(error = null) }
        }
    }

    private fun submitScore() {
        val match = _uiState.value.scoringMatch ?: return
        val score1 = _uiState.value.score1Input.toIntOrNull()
        val score2 = _uiState.value.score2Input.toIntOrNull()

        if (score1 == null || score2 == null) {
            _uiState.update { it.copy(error = "Enter valid scores") }
            return
        }
        if (score1 == score2) {
            _uiState.update { it.copy(error = "Scores must not be equal in elimination") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            tournamentRepository.updateMatch(match.id, score1, score2)
                .onSuccess {
                    _uiState.update { it.copy(
                        isLoading = false,
                        scoringMatch = null,
                        score1Input = "",
                        score2Input = ""
                    ) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
}