package com.kmp.setplay.presentation.tournament.create

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
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
                    title = { Text("Create Tournament") },
                    navigationIcon = {
                        TextButton(onClick = {
                            if (state.step == CreateStep.DETAILS) onBack()
                            else onAction(CreateTournamentAction.PreviousStep)
                        }) {
                            Text(if (state.step == CreateStep.DETAILS) "Cancel" else "Back")
                        }
                    }
                )
                LinearProgressIndicator(
                    progress = { (state.step.ordinal + 1) / 3f },
                    modifier = Modifier.fillMaxWidth()
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = { onAction(CreateTournamentAction.NameChanged(it)) },
            label = { Text("Tournament name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Format", style = MaterialTheme.typography.titleSmall)

        Column(modifier = Modifier.selectableGroup()) {
            BracketFormat.entries.forEach { format ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = state.format == format,
                            onClick = { onAction(CreateTournamentAction.FormatChanged(format)) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = state.format == format, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Text(format.displayName())
                }
            }
        }

        Text("Max teams", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(4, 8, 16, 32).forEach { n ->
                FilterChip(
                    selected = state.maxTeams == n,
                    onClick = { onAction(CreateTournamentAction.MaxTeamsChanged(n)) },
                    label = { Text("$n") }
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = state.isPublic,
                onCheckedChange = { onAction(CreateTournamentAction.IsPublicChanged(it)) }
            )
            Spacer(Modifier.width(8.dp))
            Text("Public tournament (visible to everyone)")
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { onAction(CreateTournamentAction.NextStep) },
            enabled = state.name.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Next: Add Teams")
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
            .padding(16.dp)
    ) {
        Text(
            "${state.teams.size} / ${state.maxTeams} teams",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = state.teamNameInput,
                onValueChange = { onAction(CreateTournamentAction.TeamNameInputChanged(it)) },
                label = { Text("Team name") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { onAction(CreateTournamentAction.AddTeam) },
                enabled = state.teamNameInput.isNotBlank() && state.teams.size < state.maxTeams
            ) {
                Text("Add")
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(state.teams, key = { it.id }) { team ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "#${team.seed}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(32.dp)
                        )
                        Text(
                            team.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { onAction(CreateTournamentAction.RemoveTeam(team.id)) }
                        ) {
                            Text("Remove")
                        }
                    }
                }
            }
        }

        Button(
            onClick = { onAction(CreateTournamentAction.NextStep) },
            enabled = state.teams.size >= 2,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Next: Review")
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item { Text("Review", style = MaterialTheme.typography.headlineSmall) }
        item {
            ReviewRow("Name", state.name)
            ReviewRow("Format", state.format.displayName())
            ReviewRow("Max teams", state.maxTeams.toString())
            ReviewRow("Visibility", if (state.isPublic) "Public" else "Private")
        }
        item { Text("Teams (${state.teams.size})", style = MaterialTheme.typography.titleSmall) }
        items(state.teams) { team ->
            Text("• ${team.name}", style = MaterialTheme.typography.bodyMedium)
        }
        item {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onAction(CreateTournamentAction.CreateAndGenerate) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create & Generate Bracket")
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}

private fun BracketFormat.displayName() = when (this) {
    BracketFormat.SINGLE_ELIMINATION   -> "Single Elimination"
    BracketFormat.DOUBLE_ELIMINATION   -> "Double Elimination"
    BracketFormat.ROUND_ROBIN          -> "Round Robin"
    BracketFormat.SWISS                -> "Swiss"
    BracketFormat.LEAGUE               -> "League"
    BracketFormat.THREE_GAME_GUARANTEE -> "3-Game Guarantee"
}