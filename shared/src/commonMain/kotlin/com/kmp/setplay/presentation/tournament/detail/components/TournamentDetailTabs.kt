package com.kmp.setplay.presentation.tournament.detail.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kmp.setplay.domain.model.OrganizerRole
import com.kmp.setplay.domain.model.ShareViewer
import com.kmp.setplay.presentation.common.formatMatchSchedule
import com.kmp.setplay.presentation.tournament.detail.TournamentDetailAction
import com.kmp.setplay.presentation.tournament.detail.TournamentDetailUiState

@Composable
fun StandingsTab(
    state: TournamentDetailUiState,
    onAction: (TournamentDetailAction) -> Unit
) {
    if (state.standings.isEmpty()) {
        EmptyTabState("No standings yet")
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StandingsHeaderCell("#", Modifier.width(32.dp))
                StandingsHeaderCell("Team", Modifier.weight(1f))
                StandingsHeaderCell("W", Modifier.width(32.dp), TextAlign.Center)
                StandingsHeaderCell("L", Modifier.width(32.dp), TextAlign.Center)
                StandingsHeaderCell("Pts", Modifier.width(40.dp), TextAlign.End)
            }
            HorizontalDivider()
        }
        items(state.standings.sortedByDescending { it.points }.mapIndexed { index, standing -> index + 1 to standing }) { (rank, standing) ->
            val team = state.teams.find { it.id == standing.teamId }
            Card(
                onClick = {
                    if (state.isOrganizer && team != null) {
                        onAction(TournamentDetailAction.ShowRenameTeamDialog(team))
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (rank == 1) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    }
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp)
                ) {
                    Text(
                        rankLabel(rank),
                        modifier = Modifier.width(32.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = if (rank <= 3) TextAlign.Center else TextAlign.Start
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            team?.name ?: "—",
                            fontWeight = if (rank == 1) FontWeight.Bold else FontWeight.Normal
                        )
                        if (state.isOrganizer && team != null) {
                            Text(
                                "Tap to rename",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text("${standing.wins}", modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                    Text("${standing.losses}", modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                    Text(
                        "${standing.points}",
                        modifier = Modifier.width(40.dp),
                        textAlign = TextAlign.End,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun NoticeTab(state: TournamentDetailUiState) {
    if (state.announcements.isEmpty()) {
        EmptyTabState("No announcements")
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(state.announcements.sortedByDescending { it.createdAt }) { announcement ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(announcement.message, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        announcement.createdAt.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ParticipantsTab(state: TournamentDetailUiState) {
    if (state.organizers.isEmpty() && state.teams.isEmpty()) {
        EmptyTabState("No participants yet")
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (state.organizers.isNotEmpty()) {
            item { SectionHeader("Organizers") }
            itemsIndexed(state.organizers, key = { _, organizer -> "org_${organizer.id}" }) { index, organizer ->
                ParticipantRow(
                    name = if (organizer.role == OrganizerRole.OWNER) "Owner" else "Organizer",
                    subtitle = organizer.userId,
                    index = index,
                    count = state.organizers.size
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

        item { SectionHeader("Players / Teams") }
        if (state.teams.isEmpty()) {
            item {
                Text(
                    "No one has joined yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            itemsIndexed(state.teams, key = { _, team -> "team_${team.id}" }) { index, team ->
                ParticipantRow(
                    name = team.name,
                    subtitle = team.seed?.let { "Seed $it" },
                    index = index,
                    count = state.teams.size
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AccessListRow(
    viewer: ShareViewer,
    index: Int,
    count: Int,
    onToggle: () -> Unit
) {
    SegmentedListItem(
        onClick = {},
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        supportingContent = { Text("Last viewed ${formatMatchSchedule(viewer.lastViewedAt)}") },
        trailingContent = {
            TextButton(onClick = onToggle) {
                Text(
                    if (viewer.revoked) "Restore" else "Revoke",
                    color = if (viewer.revoked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    ) {
        Text("Viewer · ${viewer.userId.take(8)}", fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EmptyTabState(message: String) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StandingsHeaderCell(
    text: String,
    modifier: Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign
    )
}

private fun rankLabel(rank: Int): String = when (rank) {
    1 -> "🥇"
    2 -> "🥈"
    3 -> "🥉"
    else -> "$rank"
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
private fun ParticipantRow(name: String, subtitle: String?, index: Int, count: Int) {
    SegmentedListItem(
        onClick = {},
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        supportingContent = subtitle?.let { { Text(it) } }
    ) {
        Text(name, fontWeight = FontWeight.Medium)
    }
}
