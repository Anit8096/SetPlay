package com.kmp.setplay.presentation.tournament.create

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kmp.setplay.domain.model.BracketFormat
import com.kmp.setplay.domain.model.Tournament
import com.kmp.setplay.presentation.common.ContentContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTournamentScreen(
    state: CreateTournamentUiState,
    onAction: (CreateTournamentAction) -> Unit,
    onCreated: (Tournament) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            onAction(CreateTournamentAction.DismissError)
        }
    }

    LaunchedEffect(state.createdTournament) {
        state.createdTournament?.let { onCreated(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            when (state.step) {
                                CreateStep.DETAILS -> "New Tournament"
                                CreateStep.TEAMS   -> "Add Teams"
                                CreateStep.REVIEW  -> "Review"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        TextButton(onClick = {
                            if (state.step == CreateStep.DETAILS) onBack()
                            else onAction(CreateTournamentAction.PreviousStep)
                        }) {
                            Text(if (state.step == CreateStep.DETAILS) "Cancel" else "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                LinearProgressIndicator(
                    progress = { (state.step.ordinal + 1) / 3f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        ContentContainer(modifier = Modifier.padding(innerPadding).imePadding()) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (state.step) {
                    CreateStep.DETAILS -> DetailsStep(state, onAction)
                    CreateStep.TEAMS   -> TeamsStep(state, onAction)
                    CreateStep.REVIEW  -> ReviewStep(state, onAction)
                }

                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

// ── Step 1: Details ───────────────────────────────────────────────────────────

@Composable
private fun DetailsStep(
    state: CreateTournamentUiState,
    onAction: (CreateTournamentAction) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            SectionLabel("Tournament Name")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.name,
                onValueChange = { onAction(CreateTournamentAction.NameChanged(it)) },
                placeholder = { Text("e.g. Spring Championship 2025") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            SectionLabel("Format")
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BracketFormat.entries.forEach { format ->
                    FormatOption(
                        format = format,
                        selected = state.format == format,
                        onClick = { onAction(CreateTournamentAction.FormatChanged(format)) }
                    )
                }
            }
        }

        item {
            SectionLabel("Max Teams")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(4, 8, 16, 32).forEach { n ->
                    TeamSizeChip(
                        count = n,
                        selected = state.maxTeams == n,
                        onClick = { onAction(CreateTournamentAction.MaxTeamsChanged(n)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Public Tournament",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Visible to everyone on the Browse page",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.isPublic,
                        onCheckedChange = { onAction(CreateTournamentAction.IsPublicChanged(it)) }
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { onAction(CreateTournamentAction.NextStep) },
                enabled = state.name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Next: Add Teams", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun FormatOption(
    format: BracketFormat,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outlineVariant
    val bgColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                  else MaterialTheme.colorScheme.surface

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = borderColor
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                format.emoji(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    format.displayName(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    format.description(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun TeamSizeChip(
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (selected) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        modifier = modifier.height(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                "$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
        }
    }
}

// ── Step 2: Teams ─────────────────────────────────────────────────────────────

@Composable
private fun TeamsStep(
    state: CreateTournamentUiState,
    onAction: (CreateTournamentAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        // Header with count
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Teams",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (state.teams.size >= state.maxTeams)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    "${state.teams.size} / ${state.maxTeams}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (state.teams.size >= state.maxTeams)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Input row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = state.teamNameInput,
                onValueChange = { onAction(CreateTournamentAction.TeamNameInputChanged(it)) },
                placeholder = { Text("Team name") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { onAction(CreateTournamentAction.AddTeam) },
                enabled = state.teamNameInput.isNotBlank() && state.teams.size < state.maxTeams,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add team", modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        // Teams list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(state.teams, key = { it.id }) { team ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "#${team.seed}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            team.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { onAction(CreateTournamentAction.RemoveTeam(team.id)) },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Remove", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { onAction(CreateTournamentAction.NextStep) },
            enabled = state.teams.size >= 2,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Next: Review", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// ── Step 3: Review ────────────────────────────────────────────────────────────

@Composable
private fun ReviewStep(
    state: CreateTournamentUiState,
    onAction: (CreateTournamentAction) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                "Ready to create?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Review your tournament details before generating the bracket.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ReviewRow("Tournament", state.name)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ReviewRow("Format", "${state.format.emoji()} ${state.format.displayName()}")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ReviewRow("Max teams", "${state.maxTeams} teams")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ReviewRow("Visibility", if (state.isPublic) "🌐 Public" else "🔒 Private")
                }
            }
        }

        item {
            Text(
                "Teams (${state.teams.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(state.teams) { team ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "#${team.seed}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp)
                )
                Text(team.name, style = MaterialTheme.typography.bodyMedium)
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onAction(CreateTournamentAction.CreateAndGenerate) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create & Generate Bracket", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
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

private fun BracketFormat.emoji() = when (this) {
    BracketFormat.SINGLE_ELIMINATION   -> "⚡"
    BracketFormat.DOUBLE_ELIMINATION   -> "🔄"
    BracketFormat.ROUND_ROBIN          -> "🔁"
    BracketFormat.SWISS                -> "🎯"
    BracketFormat.LEAGUE               -> "🏅"
    BracketFormat.THREE_GAME_GUARANTEE -> "🎮"
}

private fun BracketFormat.description() = when (this) {
    BracketFormat.SINGLE_ELIMINATION   -> "One loss and you're out"
    BracketFormat.DOUBLE_ELIMINATION   -> "Two losses before you're eliminated"
    BracketFormat.ROUND_ROBIN          -> "Everyone plays everyone once"
    BracketFormat.SWISS                -> "Matched by similar win-loss record"
    BracketFormat.LEAGUE               -> "Full season with recurring matches"
    BracketFormat.THREE_GAME_GUARANTEE -> "Every team plays at least 3 games"
}
