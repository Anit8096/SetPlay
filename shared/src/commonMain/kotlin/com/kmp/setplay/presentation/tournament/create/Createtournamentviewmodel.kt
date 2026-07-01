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
import kotlinx.datetime.LocalDate

// ── Enums ─────────────────────────────────────────────────────────────────────

enum class CreateStep { PARTICIPANTS, DETAILS }

enum class ParticipantMode { PLAYERS, TEAMS }

enum class SeedingMode { SEEDED, BLIND_DRAW, MANUAL }

// Null = No Limit
typealias MaxSize = Int?

// ── Participant entry ─────────────────────────────────────────────────────────

data class ParticipantEntry(
    val id: String,          // local only during creation
    val displayName: String, // name (private) or ID (public)
    val seed: Int
)

// ── State ─────────────────────────────────────────────────────────────────────

data class CreateTournamentUiState(
    val format: BracketFormat = BracketFormat.SINGLE_ELIMINATION,

    // ── Step 1: Participants ──
    val isPublic: Boolean = false,
    val participantMode: ParticipantMode = ParticipantMode.PLAYERS,
    val maxSize: MaxSize = null,           // null = No Limit
    val participants: List<ParticipantEntry> = emptyList(),
    val participantInput: String = "",
    val seeding: SeedingMode = SeedingMode.SEEDED,
    val thirdPlaceGame: Boolean = true,

    // ── Step 2: Details ──
    val name: String = "",
    val sport: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val description: String = "",

    // ── Navigation ──
    val step: CreateStep = CreateStep.PARTICIPANTS,

    // ── Async ──
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdTournament: Tournament? = null
)

// ── Actions ───────────────────────────────────────────────────────────────────

sealed interface CreateTournamentAction {
    // Step 1
    data class IsPublicChanged(val isPublic: Boolean) : CreateTournamentAction
    data class ParticipantModeChanged(val mode: ParticipantMode) : CreateTournamentAction
    data class MaxSizeChanged(val size: MaxSize) : CreateTournamentAction
    data class ParticipantInputChanged(val value: String) : CreateTournamentAction
    data object AddParticipant : CreateTournamentAction
    data class RemoveParticipant(val id: String) : CreateTournamentAction
    data class SeedingChanged(val mode: SeedingMode) : CreateTournamentAction
    data class ThirdPlaceGameChanged(val enabled: Boolean) : CreateTournamentAction

    // Step 2
    data class NameChanged(val name: String) : CreateTournamentAction
    data class SportChanged(val sport: String) : CreateTournamentAction
    data class StartDateChanged(val date: LocalDate?) : CreateTournamentAction
    data class EndDateChanged(val date: LocalDate?) : CreateTournamentAction
    data class DescriptionChanged(val value: String) : CreateTournamentAction

    // Navigation
    data object NextStep : CreateTournamentAction
    data object PreviousStep : CreateTournamentAction

    // Submit
    data object CreateAndGenerate : CreateTournamentAction

    // Misc
    data object DismissError : CreateTournamentAction
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class CreateTournamentViewModel(
    private val tournamentRepository: TournamentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateTournamentUiState())
    val uiState: StateFlow<CreateTournamentUiState> = _uiState.asStateFlow()

    fun initFormat(format: BracketFormat) {
        // Only set if not already initialized (VM survives recomposition)
        if (_uiState.value.format != format) {
            _uiState.update { CreateTournamentUiState(format = format) }
        }
    }

    fun onAction(action: CreateTournamentAction) {
        when (action) {
            // ── Step 1 ────────────────────────────────────────────────────────
            is CreateTournamentAction.IsPublicChanged ->
                _uiState.update { it.copy(isPublic = action.isPublic, participants = emptyList(), participantInput = "") }

            is CreateTournamentAction.ParticipantModeChanged ->
                _uiState.update { it.copy(participantMode = action.mode, participants = emptyList(), participantInput = "") }

            is CreateTournamentAction.MaxSizeChanged ->
                _uiState.update { it.copy(maxSize = action.size) }

            is CreateTournamentAction.ParticipantInputChanged ->
                _uiState.update { it.copy(participantInput = action.value) }

            CreateTournamentAction.AddParticipant -> addParticipant()

            is CreateTournamentAction.RemoveParticipant ->
                _uiState.update { state ->
                    val updated = state.participants
                        .filter { it.id != action.id }
                        .mapIndexed { i, p -> p.copy(seed = i + 1) }
                    state.copy(participants = updated)
                }

            is CreateTournamentAction.SeedingChanged ->
                _uiState.update { it.copy(seeding = action.mode) }

            is CreateTournamentAction.ThirdPlaceGameChanged ->
                _uiState.update { it.copy(thirdPlaceGame = action.enabled) }

            // ── Step 2 ────────────────────────────────────────────────────────
            is CreateTournamentAction.NameChanged ->
                _uiState.update { it.copy(name = action.name) }

            is CreateTournamentAction.SportChanged ->
                _uiState.update { it.copy(sport = action.sport) }

            is CreateTournamentAction.StartDateChanged ->
                _uiState.update { state ->
                    val newEnd = state.endDate?.takeIf { end -> action.date == null || end >= action.date }
                    state.copy(
                        startDate = action.date,
                        endDate = newEnd,
                        error = if (newEnd == null && state.endDate != null)
                            "End date cleared — it was before the new start date"
                        else state.error
                    )
                }

            is CreateTournamentAction.EndDateChanged ->
                _uiState.update { state ->
                    if (action.date != null && state.startDate != null && action.date < state.startDate) {
                        state.copy(error = "End date can't be before the start date")
                    } else {
                        state.copy(endDate = action.date)
                    }
                }

            is CreateTournamentAction.DescriptionChanged ->
                _uiState.update { it.copy(description = action.value) }

            // ── Navigation ────────────────────────────────────────────────────
            CreateTournamentAction.NextStep -> {
                val state = _uiState.value
                if (!state.isPublic && state.participants.size < 2) {
                    _uiState.update { it.copy(error = "Add at least 2 participants") }
                    return
                }
                _uiState.update { it.copy(step = CreateStep.DETAILS) }
            }

            CreateTournamentAction.PreviousStep ->
                _uiState.update { it.copy(step = CreateStep.PARTICIPANTS) }

            // ── Submit ────────────────────────────────────────────────────────
            CreateTournamentAction.CreateAndGenerate -> createAndGenerate()

            CreateTournamentAction.DismissError ->
                _uiState.update { it.copy(error = null) }
        }
    }

    private fun addParticipant() {
        val state = _uiState.value
        val input = state.participantInput.trim()
        if (input.isBlank()) return
        if (state.maxSize != null && state.participants.size >= state.maxSize) {
            _uiState.update { it.copy(error = "Maximum participants reached") }
            return
        }
        val entry = ParticipantEntry(
            id = "local_${state.participants.size}",
            displayName = input,
            seed = state.participants.size + 1
        )
        _uiState.update { it.copy(participants = it.participants + entry, participantInput = "") }
    }

    private fun createAndGenerate() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Tournament name is required") }
            return
        }
        if (!state.isPublic && state.participants.size < 2) {
            _uiState.update { it.copy(error = "Add at least 2 participants") }
            return
        }
        val start = state.startDate
        val end = state.endDate
        if (start != null && end != null && end < start) {
            _uiState.update { it.copy(error = "End date can't be before the start date") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // maxTeams: use actual count when No Limit, else the chosen cap
            val maxTeams = state.maxSize ?: state.participants.size

            val tournamentResult = tournamentRepository.createTournament(
                name = state.name,
                format = state.format,
                maxTeams = maxTeams,
                isPublic = state.isPublic
            )
            tournamentResult.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
                return@launch
            }
            val tournament = tournamentResult.getOrThrow()

            state.participants.forEach { participant ->
                tournamentRepository.addTeam(
                    tournamentId = tournament.id,
                    name = participant.displayName,
                    seed = participant.seed
                ).onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                    return@launch
                }
            }

            tournamentRepository.generateBracket(tournament.id).onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = false, createdTournament = tournament) }
        }
    }
}