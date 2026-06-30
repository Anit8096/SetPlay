package com.kmp.setplay.presentation.browse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kmp.setplay.domain.model.BracketFormat
import com.kmp.setplay.domain.model.Tournament
import com.kmp.setplay.domain.model.TournamentStatus
import com.kmp.setplay.presentation.common.ContentContainer
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BrowseScreen(
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
    viewModel: BrowseViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    ContentContainer(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SecondaryTabRow(selectedTabIndex = uiState.selectedSubTab.ordinal) {
                BrowseSubTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedSubTab == tab,
                        onClick = { viewModel.onAction(BrowseAction.SubTabSelected(tab)) },
                        text = { Text(tab.label()) }
                    )
                }
            }

            when (uiState.selectedSubTab) {
                BrowseSubTab.DISCOVER -> DiscoverContent(
                    uiState = uiState,
                    onFormatFilter = { viewModel.onAction(BrowseAction.FormatFilterChanged(it)) },
                    onStatusFilter = { viewModel.onAction(BrowseAction.StatusFilterChanged(it)) },
                    filtered = viewModel.filteredTournaments,
                    onRetry = { viewModel.onAction(BrowseAction.Refresh) }
                )
                BrowseSubTab.JOINED_LIVE -> ComingSoon("Tournaments you've joined will show up here")
                BrowseSubTab.ORGANIZING_LIVE -> OrganizingContent(
                    isLoading = uiState.isOrganizingLoading,
                    tournaments = uiState.organizingTournaments
                )
            }
        }
    }
}

@Composable
private fun DiscoverContent(
    uiState: BrowseUiState,
    onFormatFilter: (BracketFormat?) -> Unit,
    onStatusFilter: (TournamentStatus?) -> Unit,
    filtered: List<Tournament>,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // ── Filters ──────────────────────────────────────────────────────────
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = uiState.statusFilter == null,
                    onClick = { onStatusFilter(null) },
                    label = { Text("All") }
                )
            }
            item {
                FilterChip(
                    selected = uiState.statusFilter == TournamentStatus.REGISTRATION,
                    onClick = {
                        onStatusFilter(
                            if (uiState.statusFilter == TournamentStatus.REGISTRATION) null
                            else TournamentStatus.REGISTRATION
                        )
                    },
                    label = { Text("Open") }
                )
            }
            item {
                FilterChip(
                    selected = uiState.statusFilter == TournamentStatus.IN_PROGRESS,
                    onClick = {
                        onStatusFilter(
                            if (uiState.statusFilter == TournamentStatus.IN_PROGRESS) null
                            else TournamentStatus.IN_PROGRESS
                        )
                    },
                    label = { Text("In Progress") }
                )
            }
            items(BracketFormat.entries.toList()) { format ->
                FilterChip(
                    selected = uiState.formatFilter == format,
                    onClick = { onFormatFilter(if (uiState.formatFilter == format) null else format) },
                    label = { Text(format.shortLabel()) }
                )
            }
        }

        when {
            uiState.isLoading -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) { CircularProgressIndicator() }

            uiState.error != null -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(uiState.error, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onRetry) { Text("Retry") }
                }
            }

            filtered.isEmpty() -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().padding(24.dp)
            ) {
                Text(
                    "No public tournaments right now — check back soon",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { tournament ->
                    DiscoverCard(tournament)
                }
            }
        }
    }
}

@Composable
private fun OrganizingContent(
    isLoading: Boolean,
    tournaments: List<Tournament>
) {
    when {
        isLoading -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) { CircularProgressIndicator() }

        tournaments.isEmpty() -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().padding(24.dp)
        ) {
            Text(
                "You're not organizing any live public tournaments",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        else -> LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(tournaments, key = { it.id }) { tournament ->
                DiscoverCard(tournament)
            }
        }
    }
}

@Composable
private fun DiscoverCard(tournament: Tournament) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tournament.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tournament.format.shortLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "  •  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${tournament.maxTeams} max",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            StatusBadge(tournament.status)
        }
    }
}

@Composable
private fun StatusBadge(status: TournamentStatus) {
    val (label, color) = when (status) {
        TournamentStatus.REGISTRATION -> "OPEN" to MaterialTheme.colorScheme.primary
        TournamentStatus.IN_PROGRESS  -> "LIVE" to Color(0xFFE53935)
        TournamentStatus.COMPLETED    -> "DONE" to MaterialTheme.colorScheme.onSurfaceVariant
        TournamentStatus.DRAFT        -> "DRAFT" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ComingSoon(message: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun BrowseSubTab.label() = when (this) {
    BrowseSubTab.DISCOVER        -> "Discover"
    BrowseSubTab.JOINED_LIVE     -> "Joined"
    BrowseSubTab.ORGANIZING_LIVE -> "Organizing"
}

private fun BracketFormat.shortLabel() = when (this) {
    BracketFormat.SINGLE_ELIMINATION   -> "Single Elim"
    BracketFormat.DOUBLE_ELIMINATION   -> "Double Elim"
    BracketFormat.ROUND_ROBIN          -> "Round Robin"
    BracketFormat.SWISS                -> "Swiss"
    BracketFormat.LEAGUE               -> "League"
    BracketFormat.THREE_GAME_GUARANTEE -> "3-Game"
}
