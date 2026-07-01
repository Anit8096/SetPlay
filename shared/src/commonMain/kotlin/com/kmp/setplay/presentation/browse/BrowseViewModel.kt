package com.kmp.setplay.presentation.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmp.setplay.domain.model.BracketFormat
import com.kmp.setplay.domain.model.Tournament
import com.kmp.setplay.domain.model.TournamentStatus
import com.kmp.setplay.domain.repository.AuthRepository
import com.kmp.setplay.domain.repository.ParticipationSummary
import com.kmp.setplay.domain.repository.TournamentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BrowseSubTab { DISCOVER, JOINED_LIVE, ORGANIZING_LIVE }

data class BrowseUiState(
    val selectedSubTab: BrowseSubTab = BrowseSubTab.DISCOVER,
    val tournaments: List<Tournament> = emptyList(),
    val formatFilter: BracketFormat? = null,
    val statusFilter: TournamentStatus? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val organizingTournaments: List<Tournament> = emptyList(),
    val isOrganizingLoading: Boolean = false,
    // Join status + participant count per tournament id, for Discover cards
    val participation: Map<String, ParticipationSummary> = emptyMap(),
    val joiningTournamentId: String? = null,
    val joinDialogFor: Tournament? = null,
    val joinNameInput: String = ""
)

sealed interface BrowseAction {
    data class SubTabSelected(val tab: BrowseSubTab) : BrowseAction
    data class FormatFilterChanged(val format: BracketFormat?) : BrowseAction
    data class StatusFilterChanged(val status: TournamentStatus?) : BrowseAction
    data object Refresh : BrowseAction

    // Join flow (Discover card)
    data class JoinClicked(val tournament: Tournament) : BrowseAction
    data class JoinNameChanged(val value: String) : BrowseAction
    data object ConfirmJoin : BrowseAction
    data object DismissJoinDialog : BrowseAction
}

class BrowseViewModel(
    private val tournamentRepository: TournamentRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowseUiState())
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    init {
        loadDiscover()
        observeOrganizing()
    }

    fun onAction(action: BrowseAction) {
        when (action) {
            is BrowseAction.SubTabSelected -> {
                _uiState.update { it.copy(selectedSubTab = action.tab) }
                if (action.tab == BrowseSubTab.DISCOVER && _uiState.value.tournaments.isEmpty()) {
                    loadDiscover()
                }
            }
            is BrowseAction.FormatFilterChanged ->
                _uiState.update { it.copy(formatFilter = action.format) }
            is BrowseAction.StatusFilterChanged ->
                _uiState.update { it.copy(statusFilter = action.status) }
            BrowseAction.Refresh -> loadDiscover()

            is BrowseAction.JoinClicked ->
                _uiState.update { it.copy(joinDialogFor = action.tournament, joinNameInput = "") }
            is BrowseAction.JoinNameChanged ->
                _uiState.update { it.copy(joinNameInput = action.value) }
            BrowseAction.ConfirmJoin -> confirmJoin()
            BrowseAction.DismissJoinDialog ->
                _uiState.update { it.copy(joinDialogFor = null, joinNameInput = "") }
        }
    }

    /** Tournaments after applying the optional format/status filters (Discover sub-tab). */
    val filteredTournaments: List<Tournament>
        get() = _uiState.value.let { state ->
            state.tournaments.filter { t ->
                (state.formatFilter == null || t.format == state.formatFilter) &&
                    (state.statusFilter == null || t.status == state.statusFilter)
            }
        }

    private fun loadDiscover() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            tournamentRepository.getPublicTournaments()
                .onSuccess { tournaments ->
                    _uiState.update { it.copy(isLoading = false, tournaments = tournaments) }
                    loadParticipation(tournaments)
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Couldn't load tournaments — pull to retry")
                    }
                }
        }
    }

    /** Join status + participant count for every Discover card, fetched once per load. */
    private fun loadParticipation(tournaments: List<Tournament>) {
        viewModelScope.launch {
            val userId = authRepository.currentUserId.first()
            val summaries = mutableMapOf<String, ParticipationSummary>()
            tournaments.forEach { t ->
                tournamentRepository.getParticipationSummary(t.id, userId)
                    .onSuccess { summaries[t.id] = it }
            }
            _uiState.update { it.copy(participation = it.participation + summaries) }
        }
    }

    private fun confirmJoin() {
        val tournament = _uiState.value.joinDialogFor ?: return
        val name = _uiState.value.joinNameInput.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "Enter a name to join") }
            return
        }
        viewModelScope.launch {
            val userId = authRepository.currentUserId.first() ?: run {
                _uiState.update { it.copy(error = "Sign in required to join") }
                return@launch
            }
            _uiState.update { it.copy(joiningTournamentId = tournament.id) }
            tournamentRepository.registerForTournament(tournament.id, userId, name)
                .onSuccess {
                    tournamentRepository.getParticipationSummary(tournament.id, userId)
                        .onSuccess { summary ->
                            _uiState.update {
                                it.copy(
                                    participation = it.participation + (tournament.id to summary),
                                    joiningTournamentId = null,
                                    joinDialogFor = null,
                                    joinNameInput = ""
                                )
                            }
                        }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(joiningTournamentId = null, error = e.message ?: "Couldn't join tournament")
                    }
                }
        }
    }

    /** Public tournaments owned by the current user that are still REGISTRATION or IN_PROGRESS. */
    private fun observeOrganizing() {
        viewModelScope.launch {
            _uiState.update { it.copy(isOrganizingLoading = true) }
            authRepository.currentUserId
                .filterNotNull()
                .collectLatest { userId ->
                    tournamentRepository.observeMyTournaments(userId).collectLatest { tournaments ->
                        val live = tournaments.filter {
                            it.isPublic &&
                                (it.status == TournamentStatus.REGISTRATION || it.status == TournamentStatus.IN_PROGRESS)
                        }
                        _uiState.update { it.copy(organizingTournaments = live, isOrganizingLoading = false) }
                    }
                }
        }
    }
}
