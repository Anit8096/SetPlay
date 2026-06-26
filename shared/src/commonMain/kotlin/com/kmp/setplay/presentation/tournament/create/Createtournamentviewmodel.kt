package com.kmp.setplay.presentation.tournament.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmp.setplay.domain.model.BracketFormat
import com.kmp.setplay.domain.model.Team
import com.kmp.setplay.domain.model.Tournament
import com.kmp.setplay.domain.repository.TournamentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── State ─────────────────────────────────────────────────────────────────────
data class CreateTournamentUiState(
    val name: String = "",
    val format: BracketFormat = BracketFormat.SINGLE_ELIMINATION,
    val maxTeams: Int = 8,
    val isPublic: Boolean = false,
    // Team entry
    val teams: List<Team> = emptyList(),
    val teamNameInput: String = "",
    // Steps: DETAILS -> TEAMS -> REVIEW
    val step: CreateStep = CreateStep.DETAILS,
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdTournament: Tournament? = null
)

enum class CreateStep { DETAILS, TEAMS, REVIEW }

// ── Actions ───────────────────────────────────────────────────────────────────
sealed interface CreateTournamentAction {
    data class NameChanged(val name: String) : CreateTournamentAction
    data class FormatChanged(val format: BracketFormat) : CreateTournamentAction
    data class MaxTeamsChanged(val max: Int) : CreateTournamentAction
    data class IsPublicChanged(val isPublic: Boolean) : CreateTournamentAction
    data class TeamNameInputChanged(val name: String) : CreateTournamentAction
    data object AddTeam : CreateTournamentAction
    data class RemoveTeam(val teamId: String) : CreateTournamentAction
    data object NextStep : CreateTournamentAction
    data object PreviousStep : CreateTournamentAction
    data object CreateAndGenerate : CreateTournamentAction
    data object DismissError : CreateTournamentAction
}

// ── ViewModel ─────────────────────────────────────────────────────────────────
class CreateTournamentViewModel(
    private val tournamentRepository: TournamentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateTournamentUiState())
    val uiState: StateFlow<CreateTournamentUiState> = _uiState.asStateFlow()

    fun onAction(action: CreateTournamentAction) {
        when (action) {
            is CreateTournamentAction.NameChanged ->
                _uiState.update { it.copy(name = action.name) }

            is CreateTournamentAction.FormatChanged ->
                _uiState.update { it.copy(format = action.format) }

            is CreateTournamentAction.MaxTeamsChanged ->
                _uiState.update { it.copy(maxTeams = action.max) }

            is CreateTournamentAction.IsPublicChanged ->
                _uiState.update { it.copy(isPublic = action.isPublic) }

            is CreateTournamentAction.TeamNameInputChanged ->
                _uiState.update { it.copy(teamNameInput = action.name) }

            CreateTournamentAction.AddTeam -> addTeam()

            is CreateTournamentAction.RemoveTeam ->
                _uiState.update { state ->
                    state.copy(teams = state.teams.filter { it.id != action.teamId })
                }

            CreateTournamentAction.NextStep ->
                _uiState.update { it.copy(step = it.step.next()) }

            CreateTournamentAction.PreviousStep ->
                _uiState.update { it.copy(step = it.step.previous()) }

            CreateTournamentAction.CreateAndGenerate -> createAndGenerate()

            CreateTournamentAction.DismissError ->
                _uiState.update { it.copy(error = null) }
        }
    }

    private fun addTeam() {
        val name = _uiState.value.teamNameInput.trim()
        if (name.isBlank()) return
        if (_uiState.value.teams.size >= _uiState.value.maxTeams) {
            _uiState.update { it.copy(error = "Maximum teams reached") }
            return
        }

        // Add local placeholder — real Team is created in Supabase after tournament creation
        val placeholder = Team(
            id = "local_${_uiState.value.teams.size}",
            tournamentId = "",
            name = name,
            seed = _uiState.value.teams.size + 1,
            logoUrl = null,
            createdAt = kotlin.time.Clock.System.now()
        )
        _uiState.update { it.copy(
            teams = it.teams + placeholder,
            teamNameInput = ""
        ) }
    }

    private fun createAndGenerate() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Tournament name is required") }
            return
        }
        if (state.teams.size < 2) {
            _uiState.update { it.copy(error = "Add at least 2 teams") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // 1. Create the tournament
            val tournamentResult = tournamentRepository.createTournament(
                name = state.name,
                format = state.format,
                maxTeams = state.maxTeams,
                isPublic = state.isPublic
            )

            tournamentResult.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
                return@launch
            }

            val tournament = tournamentResult.getOrThrow()

            // 2. Add all teams
            state.teams.forEach { localTeam ->
                tournamentRepository.addTeam(
                    tournamentId = tournament.id,
                    name = localTeam.name,
                    seed = localTeam.seed
                ).onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                    return@launch
                }
            }

            // 3. Generate bracket
            tournamentRepository.generateBracket(tournament.id).onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = false, createdTournament = tournament) }
        }
    }

    private fun CreateStep.next() = when (this) {
        CreateStep.DETAILS -> CreateStep.TEAMS
        CreateStep.TEAMS -> CreateStep.REVIEW
        CreateStep.REVIEW -> CreateStep.REVIEW
    }

    private fun CreateStep.previous() = when (this) {
        CreateStep.DETAILS -> CreateStep.DETAILS
        CreateStep.TEAMS -> CreateStep.DETAILS
        CreateStep.REVIEW -> CreateStep.TEAMS
    }
}