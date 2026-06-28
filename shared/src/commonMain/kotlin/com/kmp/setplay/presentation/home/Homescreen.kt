package com.kmp.setplay.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kmp.setplay.domain.model.BracketFormat
import com.kmp.setplay.domain.model.Tournament
import com.kmp.setplay.domain.model.TournamentStatus
import com.kmp.setplay.presentation.auth.AuthAction
import com.kmp.setplay.presentation.auth.AuthUiState
import com.kmp.setplay.presentation.auth.LinkAccountBanner
import com.kmp.setplay.presentation.common.ContentContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    authState: AuthUiState,
    onAction: (HomeAction) -> Unit,
    onCreateTournament: () -> Unit,
    onTournamentClick: (Tournament) -> Unit,
    onJoin: () -> Unit,
    onLinkGoogle: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            onAction(HomeAction.DismissError)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("SetPlay") },
                actions = {
                    TextButton(onClick = onJoin) { Text("Join") }
                    TextButton(onClick = onSignOut) { Text("Sign out") }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateTournament,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Tournament") }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        ContentContainer(modifier = Modifier.padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (authState.isAnonymous) {
                    LinkAccountBanner(onLinkGoogle = onLinkGoogle)
                }

                when {
                    state.isLoading -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    state.tournaments.isEmpty() -> {
                        EmptyState(
                            onCreateTournament = onCreateTournament,
                            onJoin = onJoin
                        )
                    }

                    else -> {
                        TournamentList(
                            tournaments = state.tournaments,
                            onTournamentClick = onTournamentClick
                        )
                    }
                }
            }
        }
    }
}

// ── Tournament list ───────────────────────────────────────────────────────────

@Composable
private fun TournamentList(
    tournaments: List<Tournament>,
    onTournamentClick: (Tournament) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                "My Tournaments",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(tournaments, key = { it.id }) { tournament ->
            TournamentCard(
                tournament = tournament,
                onClick = { onTournamentClick(tournament) }
            )
        }
    }
}

@Composable
private fun TournamentCard(
    tournament: Tournament,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    tournament.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(tournament.status)
            }
            Text(
                tournament.format.displayName(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (tournament.inviteCode != null) {
                Text(
                    "Code: ${tournament.inviteCode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: TournamentStatus) {
    val (label, color) = when (status) {
        TournamentStatus.DRAFT        -> "Draft"        to MaterialTheme.colorScheme.outline
        TournamentStatus.REGISTRATION -> "Registration" to MaterialTheme.colorScheme.tertiary
        TournamentStatus.IN_PROGRESS  -> "Live"         to MaterialTheme.colorScheme.primary
        TournamentStatus.COMPLETED    -> "Done"         to MaterialTheme.colorScheme.secondary
    }
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = color
    )
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(
    onCreateTournament: () -> Unit,
    onJoin: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().padding(32.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "No tournaments yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Create your first bracket or join one with an invite code.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            androidx.compose.material3.Button(
                onClick = onCreateTournament,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Tournament")
            }
            androidx.compose.material3.OutlinedButton(
                onClick = onJoin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Join with Code")
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun BracketFormat.displayName() = when (this) {
    BracketFormat.SINGLE_ELIMINATION   -> "Single Elimination"
    BracketFormat.DOUBLE_ELIMINATION   -> "Double Elimination"
    BracketFormat.ROUND_ROBIN          -> "Round Robin"
    BracketFormat.SWISS                -> "Swiss"
    BracketFormat.LEAGUE               -> "League"
    BracketFormat.THREE_GAME_GUARANTEE -> "3-Game Guarantee"
}