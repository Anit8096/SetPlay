package com.kmp.setplay.presentation.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmp.setplay.domain.model.BracketFormat
import com.kmp.setplay.domain.model.Tournament
import com.kmp.setplay.domain.model.TournamentStatus
import com.kmp.setplay.domain.repository.AuthRepository
import com.kmp.setplay.domain.repository.TournamentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
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
    val isOrganizingLoading: Boolean = false
)

sealed interface BrowseAction {
    data class SubTabSelected(val tab: BrowseSubTab) : BrowseAction
    data class FormatFilterChanged(val format: BracketFormat?) : BrowseAction
    data class StatusFilterChanged(val status: TournamentStatus?) : BrowseAction
    data object Refresh : BrowseAction
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
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Couldn't load tournaments — pull to retry")
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
