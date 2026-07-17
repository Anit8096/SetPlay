package com.kmp.setplay.presentation.tournament.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.kmp.setplay.presentation.common.MatchDateTimePickerDialog
import com.kmp.setplay.presentation.tournament.detail.TournamentDetailAction
import com.kmp.setplay.presentation.tournament.detail.TournamentDetailUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentDetailOverlays(
    state: TournamentDetailUiState,
    onAction: (TournamentDetailAction) -> Unit
) {
    state.schedulingMatch?.let { match ->
        MatchDateTimePickerDialog(
            initial = match.scheduledAt,
            onConfirm = { onAction(TournamentDetailAction.ConfirmSchedule(it)) },
            onClear = if (match.scheduledAt != null) {
                { onAction(TournamentDetailAction.ClearSchedule) }
            } else {
                null
            },
            onDismiss = { onAction(TournamentDetailAction.DismissScheduleDialog) }
        )
    }

    if (state.isOrganizer) {
        state.scoringMatch?.let { match ->
            ScoreEntryDialog(
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

    if (state.showRenameDialog) {
        RenameDialog(
            title = "Rename tournament",
            value = state.renameInput,
            onValueChange = { onAction(TournamentDetailAction.RenameInputChanged(it)) },
            onConfirm = { onAction(TournamentDetailAction.ConfirmRename) },
            onDismiss = { onAction(TournamentDetailAction.DismissRenameDialog) }
        )
    }

    state.renamingTeam?.let { team ->
        RenameDialog(
            title = "Rename \"${team.name}\"",
            value = state.renameTeamInput,
            onValueChange = { onAction(TournamentDetailAction.RenameTeamInputChanged(it)) },
            onConfirm = { onAction(TournamentDetailAction.ConfirmRenameTeam) },
            onDismiss = { onAction(TournamentDetailAction.DismissRenameTeamDialog) }
        )
    }

    if (state.showShareCode) ShareCodeSheet(state, onAction)
    if (state.showAccessList) AccessListSheet(state, onAction)

    if (state.confirmEndTournament) {
        ConfirmationDialog(
            title = "End tournament?",
            message = "This marks the tournament as completed. Scores will be locked.",
            confirmLabel = "End",
            onConfirm = { onAction(TournamentDetailAction.ConfirmEndTournament) },
            onDismiss = { onAction(TournamentDetailAction.DismissEndDialog) }
        )
    }

    if (state.confirmDeleteTournament) {
        ConfirmationDialog(
            title = "Delete tournament?",
            message = "This permanently deletes the tournament and all its data. This can't be undone.",
            confirmLabel = "Delete",
            onConfirm = { onAction(TournamentDetailAction.ConfirmDeleteTournament) },
            onDismiss = { onAction(TournamentDetailAction.DismissDeleteDialog) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareCodeSheet(
    state: TournamentDetailUiState,
    onAction: (TournamentDetailAction) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val clipboardManager = LocalClipboardManager.current
    var copied by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = { onAction(TournamentDetailAction.DismissShareCode) },
        sheetState = sheetState
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text("Share code", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Share this code so others can view the tournament",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            val code = state.tournament?.inviteCode
            if (code != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(top = 20.dp)
                ) {
                    Text(
                        code,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = TextUnit.Unspecified,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                    )
                }
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(code))
                        copied = true
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(if (copied) "Copied!" else "Copy code")
                }
            } else {
                Text(
                    "No share code yet — one is generated when you create the tournament.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
            Box(modifier = Modifier.padding(top = 32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccessListSheet(
    state: TournamentDetailUiState,
    onAction: (TournamentDetailAction) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = { onAction(TournamentDetailAction.DismissAccessList) },
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text("Access list", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Everyone who has opened this tournament with the share code",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            when {
                state.isLoadingViewers -> Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                ) {
                    LoadingIndicator()
                }

                state.viewers.isEmpty() -> Text(
                    "No one has viewed this tournament via share code yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )

                else -> Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    state.viewers.forEachIndexed { index, viewer ->
                        AccessListRow(
                            viewer = viewer,
                            index = index,
                            count = state.viewers.size,
                            onToggle = { onAction(TournamentDetailAction.ToggleViewerAccess(viewer)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RenameDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = value.isNotBlank()
            ) {
                Text("Rename")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ScoreEntryDialog(
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
        title = { Text("Enter Score", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = score1,
                    onValueChange = onScore1Changed,
                    label = { Text(team1Name) },
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = score2,
                    onValueChange = onScore2Changed,
                    label = { Text(team2Name) },
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
