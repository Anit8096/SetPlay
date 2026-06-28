package com.kmp.setplay.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kmp.setplay.domain.model.BracketFormat
import com.kmp.setplay.domain.model.Tournament
import com.kmp.setplay.domain.model.TournamentStatus
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
                title = {
                    Text(
                        "SetPlay",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    TextButton(onClick = onJoin) { Text("Join") }
                    TextButton(onClick = onSignOut) { Text("Sign out") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            // Only show FAB when there are tournaments (empty state has its own CTA)
            AnimatedVisibility(
                visible = state.tournaments.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = onCreateTournament,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Tournament") },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                "My Tournaments",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        items(tournaments, key = { it.id }) { tournament ->
            TournamentCard(
                tournament = tournament,
                onClick = { onTournamentClick(tournament) }
            )
        }
        item { Spacer(Modifier.height(80.dp)) } // FAB clearance
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TournamentCard(
    tournament: Tournament,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    tournament.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(tournament.status)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                tournament.format.displayName(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (tournament.inviteCode != null) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Code: ${tournament.inviteCode}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: TournamentStatus) {
    val (label, containerColor, contentColor) = when (status) {
        TournamentStatus.DRAFT        -> Triple("Draft",        MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        TournamentStatus.REGISTRATION -> Triple("Registration", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        TournamentStatus.IN_PROGRESS  -> Triple("Live",         MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
        TournamentStatus.COMPLETED    -> Triple("Done",         MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
    }
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = CircleShape
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Trophy icon placeholder using text emoji — no image dependency needed
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("🏆", style = MaterialTheme.typography.headlineLarge)
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "No tournaments yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Create your first bracket or join one with an invite code.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(16.dp))

            androidx.compose.material3.Button(
                onClick = onCreateTournament,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Create Tournament")
            }
            androidx.compose.material3.OutlinedButton(
                onClick = onJoin,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
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
