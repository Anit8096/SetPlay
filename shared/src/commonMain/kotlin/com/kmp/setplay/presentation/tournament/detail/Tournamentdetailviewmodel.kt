package com.kmp.setplay.presentation.tournament.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmp.setplay.domain.model.Announcement
import com.kmp.setplay.domain.model.Match
import com.kmp.setplay.domain.model.OrganizerRole
import com.kmp.setplay.domain.model.Standing
import com.kmp.setplay.domain.model.Team
import com.kmp.setplay.domain.model.Tournament
import com.kmp.setplay.domain.model.TournamentStatus
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
    val score2Input: String = "",
    // Rename tournament dialog
    val showRenameDialog: Boolean = false,
    val renameInput: String = "",
    // Rename team dialog
    val renamingTeam: Team? = null,
    val renameTeamInput: String = "",
    // Share code
    val showShareCode: Boolean = false,
    // Confirmation dialogs (owner only)
    val confirmEndTournament: Boolean = false,
    val confirmDeleteTournament: Boolean = false,
    // Pop screen after delete
    val tournamentDeleted: Boolean = false
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

    // Rename tournament
    data object ShowRenameDialog : TournamentDetailAction
    data class RenameInputChanged(val value: String) : TournamentDetailAction
    data object ConfirmRename : TournamentDetailAction
    data object DismissRenameDialog : TournamentDetailAction

    // Rename team
    data class ShowRenameTeamDialog(val team: Team) : TournamentDetailAction
    data class RenameTeamInputChanged(val value: String) : TournamentDetailAction
    data object ConfirmRenameTeam : TournamentDetailAction
    data object DismissRenameTeamDialog : TournamentDetailAction

    // Share code
    data object ShowShareCode : TournamentDetailAction
    data object DismissShareCode : TournamentDetailAction

    // End / Delete (owner only)
    data object RequestEndTournament : TournamentDetailAction
    data object ConfirmEndTournament : TournamentDetailAction
    data object DismissEndDialog : TournamentDetailAction
    data object RequestDeleteTournament : TournamentDetailAction
    data object ConfirmDeleteTournament : TournamentDetailAction
    data object DismissDeleteDialog : TournamentDetailAction
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
                        it.copy(organizerRole = role, isOrganizer = role != null)
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
                        score2Input = current.score2Input,
                        organizerRole = current.organizerRole,
                        isOrganizer = current.isOrganizer,
                        showRenameDialog = current.showRenameDialog,
                        renameInput = current.renameInput,
                        renamingTeam = current.renamingTeam,
                        renameTeamInput = current.renameTeamInput,
                        showShareCode = current.showShareCode,
                        confirmEndTournament = current.confirmEndTournament,
                        confirmDeleteTournament = current.confirmDeleteTournament,
                        tournament = if (current.tournamentDeleted) current.tournament else newState.tournament,
                        tournamentDeleted = current.tournamentDeleted
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

            // ── Rename tournament ──────────────────────────────────────────────
            TournamentDetailAction.ShowRenameDialog ->
                _uiState.update { it.copy(showRenameDialog = true, renameInput = it.tournament?.name ?: "") }

            is TournamentDetailAction.RenameInputChanged ->
                _uiState.update { it.copy(renameInput = action.value) }

            TournamentDetailAction.ConfirmRename -> renameTournament()

            TournamentDetailAction.DismissRenameDialog ->
                _uiState.update { it.copy(showRenameDialog = false, renameInput = "") }

            // ── Rename team ────────────────────────────────────────────────────
            is TournamentDetailAction.ShowRenameTeamDialog ->
                _uiState.update { it.copy(renamingTeam = action.team, renameTeamInput = action.team.name) }

            is TournamentDetailAction.RenameTeamInputChanged ->
                _uiState.update { it.copy(renameTeamInput = action.value) }

            TournamentDetailAction.ConfirmRenameTeam -> renameTeam()

            TournamentDetailAction.DismissRenameTeamDialog ->
                _uiState.update { it.copy(renamingTeam = null, renameTeamInput = "") }

            // ── Share code ─────────────────────────────────────────────────────
            TournamentDetailAction.ShowShareCode ->
                _uiState.update { it.copy(showShareCode = true) }

            TournamentDetailAction.DismissShareCode ->
                _uiState.update { it.copy(showShareCode = false) }

            // ── End ────────────────────────────────────────────────────────────
            TournamentDetailAction.RequestEndTournament ->
                _uiState.update { it.copy(confirmEndTournament = true) }

            TournamentDetailAction.ConfirmEndTournament -> endTournament()

            TournamentDetailAction.DismissEndDialog ->
                _uiState.update { it.copy(confirmEndTournament = false) }

            // ── Delete ─────────────────────────────────────────────────────────
            TournamentDetailAction.RequestDeleteTournament ->
                _uiState.update { it.copy(confirmDeleteTournament = true) }

            TournamentDetailAction.ConfirmDeleteTournament -> deleteTournament()

            TournamentDetailAction.DismissDeleteDialog ->
                _uiState.update { it.copy(confirmDeleteTournament = false) }
        }
    }

    private fun renameTournament() {
        val tournament = _uiState.value.tournament ?: return
        val newName = _uiState.value.renameInput.trim()
        if (newName.isBlank()) {
            _uiState.update { it.copy(error = "Name can't be empty") }
            return
        }
        viewModelScope.launch {
            tournamentRepository.updateTournament(tournament.copy(name = newName))
                .onSuccess { _uiState.update { it.copy(showRenameDialog = false, renameInput = "") } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message ?: "Couldn't rename tournament") } }
        }
    }

    private fun renameTeam() {
        val team = _uiState.value.renamingTeam ?: return
        val newName = _uiState.value.renameTeamInput.trim()
        if (newName.isBlank()) {
            _uiState.update { it.copy(error = "Team name can't be empty") }
            return
        }
        viewModelScope.launch {
            tournamentRepository.renameTeam(team.id, newName)
                .onSuccess { _uiState.update { it.copy(renamingTeam = null, renameTeamInput = "") } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message ?: "Couldn't rename team") } }
        }
    }

    private fun endTournament() {
        val tournament = _uiState.value.tournament ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, confirmEndTournament = false) }
            tournamentRepository.updateTournament(tournament.copy(status = TournamentStatus.COMPLETED))
                .onSuccess { _uiState.update { it.copy(isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "Couldn't end tournament") } }
        }
    }

    private fun deleteTournament() {
        val tournament = _uiState.value.tournament ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, confirmDeleteTournament = false) }
            tournamentRepository.deleteTournament(tournament.id)
                .onSuccess { _uiState.update { it.copy(isLoading = false, tournamentDeleted = true) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "Couldn't delete tournament") } }
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
