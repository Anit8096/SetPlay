package com.kmp.setplay.presentation.tournament.detail

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kmp.setplay.domain.model.Match
import com.kmp.setplay.domain.model.MatchStatus
import com.kmp.setplay.domain.model.Team

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentDetailScreen(
    state: TournamentDetailUiState,
    onAction: (TournamentDetailAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            onAction(TournamentDetailAction.DismissError)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.tournament?.name ?: "Tournament") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (state.isLoading && state.tournament == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            PrimaryTabRow(selectedTabIndex = state.selectedTab.ordinal) {
                DetailTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { onAction(TournamentDetailAction.TabSelected(tab)) },
                        text = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            when (state.selectedTab) {
                DetailTab.BRACKET -> BracketTab(state, onAction)
                DetailTab.STANDINGS -> StandingsTab(state)
                DetailTab.ANNOUNCEMENTS -> AnnouncementsTab(state)
            }
        }

        // Score entry dialog
        state.scoringMatch?.let { match ->
            ScoreEntryDialog(
                match = match,
                team1Name = state.teams.find { it.id == match.team1Id }?.name ?: "Team 1",
                team2Name = state.teams.find { it.id == match.team2Id }?.name ?: "Team 2",
                score1 = state.score1Input,
                score2 = state.score2Input,
                onScore1Changed = { onAction(TournamentDetailAction.Score1Changed(it)) },
                onScore2Changed = { onAction(TournamentDetailAction.Score2Changed(it)) },
                onSubmit = { onAction(TournamentDetailAction.SubmitScore) },
                onDismiss = { onAction(TournamentDetailAction.DismissScoreDialog) }
            )
        }
    }
}

// ── Bracket tab ───────────────────────────────────────────────────────────────
@Composable
private fun BracketTab(
    state: TournamentDetailUiState,
    onAction: (TournamentDetailAction) -> Unit
) {
    val matchesByRound = state.matches.groupBy { it.roundId }
    val rounds = matchesByRound.keys.toList()

    if (rounds.isEmpty()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text("No bracket generated yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        rounds.forEach { roundId ->
            val roundMatches = matchesByRound[roundId] ?: emptyList()
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.width(160.dp)
            ) {
                roundMatches.forEach { match ->
                    MatchCard(
                        match = match,
                        team1Name = state.teams.find { it.id == match.team1Id }?.name,
                        team2Name = state.teams.find { it.id == match.team2Id }?.name,
                        onClick = {
                            if (match.status == MatchStatus.SCHEDULED &&
                                match.team1Id != null && match.team2Id != null
                            ) {
                                onAction(TournamentDetailAction.MatchClicked(match))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchCard(
    match: Match,
    team1Name: String?,
    team2Name: String?,
    onClick: () -> Unit
) {
    val containerColor = when (match.status) {
        MatchStatus.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer
        MatchStatus.BYE       -> MaterialTheme.colorScheme.surfaceVariant
        else                  -> MaterialTheme.colorScheme.surface
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            TeamRow(
                name = team1Name ?: "TBD",
                score = match.score1,
                isWinner = match.winnerId == match.team1Id && match.winnerId != null
            )
            Spacer(Modifier.height(4.dp))
            TeamRow(
                name = team2Name ?: if (match.status == MatchStatus.BYE) "BYE" else "TBD",
                score = match.score2,
                isWinner = match.winnerId == match.team2Id && match.winnerId != null
            )
        }
    }
}

@Composable
private fun TeamRow(name: String, score: Int?, isWinner: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        if (score != null) {
            Text(
                score.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                color = if (isWinner) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ── Standings tab ─────────────────────────────────────────────────────────────
@Composable
private fun StandingsTab(state: TournamentDetailUiState) {
    if (state.standings.isEmpty()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text("No standings yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("#", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelMedium)
                Text("Team", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Text("W", modifier = Modifier.width(32.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
                Text("L", modifier = Modifier.width(32.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
                Text("Pts", modifier = Modifier.width(40.dp), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium)
            }
        }
        items(state.standings.sortedByDescending { it.points }.mapIndexed { i, s -> i + 1 to s }) { (rank, standing) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("$rank", modifier = Modifier.width(32.dp))
                Text(
                    state.teams.find { it.id == standing.teamId }?.name ?: "—",
                    modifier = Modifier.weight(1f)
                )
                Text("${standing.wins}", modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                Text("${standing.losses}", modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                Text("${standing.points}", modifier = Modifier.width(40.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Announcements tab ─────────────────────────────────────────────────────────
@Composable
private fun AnnouncementsTab(state: TournamentDetailUiState) {
    if (state.announcements.isEmpty()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text("No announcements", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(state.announcements) { announcement ->
            Card(modifier = Modifier.fillMaxWidth()) {
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

// ── Score entry dialog ────────────────────────────────────────────────────────
@Composable
private fun ScoreEntryDialog(
    match: Match,
    team1Name: String,
    team2Name: String,
    score1: String,
    score2: String,
    onScore1Changed: (String) -> Unit,
    onScore2Changed: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Score") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = score1,
                    onValueChange = onScore1Changed,
                    label = { Text(team1Name) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = score2,
                    onValueChange = onScore2Changed,
                    label = { Text(team2Name) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = score1.toIntOrNull() != null && score2.toIntOrNull() != null
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}