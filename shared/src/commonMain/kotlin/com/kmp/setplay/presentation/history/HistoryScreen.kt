package com.kmp.setplay.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kmp.setplay.domain.model.BracketFormat
import com.kmp.setplay.domain.model.Tournament
import com.kmp.setplay.domain.model.TournamentStatus
import com.kmp.setplay.presentation.common.ContentContainer
import com.kmp.setplay.presentation.common.formatMatchSchedule
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryScreen(
    contentPadding: PaddingValues = PaddingValues(),
    onTournamentSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    ContentContainer(
        modifier = modifier.fillMaxSize().padding(contentPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PrimaryTabRow(
                selectedTabIndex = uiState.selectedSubTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                HistorySubTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedSubTab == tab,
                        onClick = { viewModel.onAction(HistoryAction.SubTabSelected(tab)) },
                        text = {
                            Text(tab.label(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    )
                }
            }

            when (uiState.selectedSubTab) {
                HistorySubTab.PRIVATE -> PrivateHistoryContent(
                    isLoading = uiState.isLoading,
                    active = uiState.active,
                    completed = uiState.completed,
                    onTournamentSelected = onTournamentSelected
                )
                // Needs the Step 8 participant-join data model — same reason
                // Browse's "Joined" sub-tab is still a placeholder.
                HistorySubTab.JOINED -> Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize().padding(24.dp)
                ) {
                    Text(
                        "Public tournaments you've joined will show up here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PrivateHistoryContent(
    isLoading: Boolean,
    active: List<Tournament>,
    completed: List<Tournament>,
    onTournamentSelected: (String) -> Unit
) {
    when {
        isLoading -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) { LoadingIndicator() }

        active.isEmpty() && completed.isEmpty() -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().padding(24.dp)
        ) {
            Text(
                "No private tournaments yet — create one from the Home tab",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        else -> LazyColumn(
            contentPadding = PaddingValues(16.dp),
            // 2dp base spacing is the segmented-list gap; headers carry their own.
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (active.isNotEmpty()) {
                item { SectionHeader("In progress") }
                itemsIndexed(active, key = { _, t -> "active_${t.id}" }) { index, tournament ->
                    HistoryRow(
                        tournament = tournament,
                        index = index,
                        count = active.size,
                        onClick = { onTournamentSelected(tournament.id) }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }

            if (completed.isNotEmpty()) {
                item { SectionHeader("Completed") }
                itemsIndexed(completed, key = { _, t -> "done_${t.id}" }) { index, tournament ->
                    HistoryRow(
                        tournament = tournament,
                        index = index,
                        count = completed.size,
                        onClick = { onTournamentSelected(tournament.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HistoryRow(
    tournament: Tournament,
    index: Int,
    count: Int,
    onClick: () -> Unit
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        supportingContent = {
            Text("${tournament.format.shortLabel()} · created ${formatMatchSchedule(tournament.createdAt)}")
        },
        trailingContent = { StatusBadge(tournament.status) }
    ) {
        Text(
            tournament.name,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusBadge(status: TournamentStatus) {
    val (label, color) = when (status) {
        TournamentStatus.REGISTRATION -> "OPEN" to MaterialTheme.colorScheme.primary
        TournamentStatus.IN_PROGRESS  -> "LIVE" to MaterialTheme.colorScheme.error
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

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun HistorySubTab.label() = when (this) {
    HistorySubTab.PRIVATE -> "My tournaments"
    HistorySubTab.JOINED  -> "Joined"
}

private fun BracketFormat.shortLabel() = when (this) {
    BracketFormat.SINGLE_ELIMINATION   -> "Single Elim"
    BracketFormat.DOUBLE_ELIMINATION   -> "Double Elim"
    BracketFormat.ROUND_ROBIN          -> "Round Robin"
    BracketFormat.SWISS                -> "Swiss"
    BracketFormat.LEAGUE               -> "League"
    BracketFormat.THREE_GAME_GUARANTEE -> "3-Game"
}
