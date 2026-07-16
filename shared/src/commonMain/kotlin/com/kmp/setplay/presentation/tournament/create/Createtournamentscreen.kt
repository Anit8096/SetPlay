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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kmp.setplay.domain.model.BracketFormat
import com.kmp.setplay.domain.model.Tournament
import com.kmp.setplay.presentation.common.ContentContainer
import com.kmp.setplay.presentation.common.DatePickerField
import com.kmp.setplay.presentation.common.todayLocalDate

// Title and back-button handling for this screen are rendered by MainAppNavigation's
// shared Scaffold topBar (see createTournamentTopBarTitle/onCreateTournamentBack below)
// rather than by this composable, which only renders body content.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CreateTournamentScreen(
    state: CreateTournamentUiState,
    onAction: (CreateTournamentAction) -> Unit,
    onCreated: (Tournament) -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
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

    Box(modifier = modifier.fillMaxSize()) {
        ContentContainer(modifier = Modifier.padding(contentPadding).imePadding()) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (state.step) {
                    CreateStep.PARTICIPANTS -> ParticipantsStep(state, onAction)
                    CreateStep.DETAILS      -> DetailsStep(state, onAction)
                }
                if (state.isLoading) {
                    LoadingIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

/** Top app bar title for the create-tournament wizard, keyed off the current step. */
fun createTournamentTopBarTitle(state: CreateTournamentUiState): String = when (state.step) {
    CreateStep.PARTICIPANTS -> state.format.displayName()
    CreateStep.DETAILS      -> "Final Details"
}

/**
 * Back-button behavior for the create-tournament wizard: the first step pops the nav
 * back stack via [onBack], later steps step back within the wizard instead.
 */
fun onCreateTournamentBack(
    state: CreateTournamentUiState,
    onAction: (CreateTournamentAction) -> Unit,
    onBack: () -> Unit
) {
    if (state.step == CreateStep.PARTICIPANTS) onBack()
    else onAction(CreateTournamentAction.PreviousStep)
}

// ── Step 1: Participants ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParticipantsStep(
    state: CreateTournamentUiState,
    onAction: (CreateTournamentAction) -> Unit
) {
    val inputLabel = when (state.participantMode) {
        ParticipantMode.PLAYERS -> "Player name"
        ParticipantMode.TEAMS   -> "Team name"
    }

    val listLabel = when (state.participantMode) {
        ParticipantMode.PLAYERS -> "Player names"
        ParticipantMode.TEAMS   -> "Team names"
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Players / Teams toggle
        item {
            SectionLabel("Participants")
            Spacer(Modifier.height(10.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ParticipantMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = state.participantMode == mode,
                        onClick = { onAction(CreateTournamentAction.ParticipantModeChanged(mode)) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ParticipantMode.entries.size
                        ),
                        label = {
                            Text(if (mode == ParticipantMode.PLAYERS) "Players" else "Teams")
                        }
                    )
                }
            }
        }

        // Max size chips
        item {
            SectionLabel("Max size")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(4, 8, 16, 32).forEach { n ->
                    SizeChip(
                        label = "$n",
                        selected = state.maxSize == n,
                        onClick = { onAction(CreateTournamentAction.MaxSizeChanged(n)) },
                        modifier = Modifier.weight(1f)
                    )
                }
                SizeChip(
                    label = "∞",
                    selected = state.maxSize == null,
                    onClick = { onAction(CreateTournamentAction.MaxSizeChanged(null)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Name input + list (private only — public participants join via code/QR)
        if (!state.isPublic) {
            item {
                val count = state.participants.size
                val limitLabel = if (state.maxSize == null) "No limit" else "${state.maxSize}"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel(listLabel)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "$count / $limitLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.participantInput,
                        onValueChange = { onAction(CreateTournamentAction.ParticipantInputChanged(it)) },
                        placeholder = { Text(inputLabel) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { onAction(CreateTournamentAction.AddParticipant) },
                        enabled = state.participantInput.isNotBlank() &&
                                (state.maxSize == null || state.participants.size < state.maxSize),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Participant list — a single Material3 segmented list group (2dp gaps),
            // kept as one LazyColumn item so the wizard's 24dp section spacing doesn't
            // leak in between segments. Roster sizes here are small (≤32), so losing
            // per-row lazy recycling costs nothing.
            if (state.participants.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        state.participants.forEachIndexed { index, participant ->
                            ParticipantRow(
                                participant = participant,
                                index = index,
                                count = state.participants.size,
                                onRemove = { onAction(CreateTournamentAction.RemoveParticipant(participant.id)) }
                            )
                        }
                    }
                }
            }
        } else {
            // Public info message
            item {
                SectionCard {
                    Column {
                        Text(
                            "Participants join via invite code or QR",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "After creating the tournament, share the invite code or QR code. Players or team captains join themselves.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Seeding
        item {
            SectionLabel("Seeding")
            Spacer(Modifier.height(10.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SeedingMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = state.seeding == mode,
                        onClick = { onAction(CreateTournamentAction.SeedingChanged(mode)) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = SeedingMode.entries.size
                        ),
                        label = {
                            Text(
                                when (mode) {
                                    SeedingMode.SEEDED     -> "Seeded"
                                    SeedingMode.BLIND_DRAW -> "Blind Draw"
                                    SeedingMode.MANUAL     -> "Manual"
                                },
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                when (state.seeding) {
                    SeedingMode.SEEDED     -> "#1 vs last seed, #2 vs second-last — top seeds don't meet early"
                    SeedingMode.BLIND_DRAW -> "Participants are randomly assigned to bracket slots"
                    SeedingMode.MANUAL     -> "You decide the matchups after creation"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 3rd place game
        item {
            SectionCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "3rd place game",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Losers of semi-finals play for bronze",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (state.thirdPlaceGame) "Yes" else "No",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = state.thirdPlaceGame,
                            onCheckedChange = {
                                onAction(CreateTournamentAction.ThirdPlaceGameChanged(it))
                            }
                        )
                    }
                }
            }
        }

        // Make Public toggle (at the bottom)
        item {
            SectionCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Make Public",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (state.isPublic)
                                "Discoverable in Browse — participants join via code or QR"
                            else
                                "Private — synced to your account only, not discoverable",
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

        // Next button
        item {
            val canProceed = if (state.isPublic) true else state.participants.size >= 2
            Button(
                onClick = { onAction(CreateTournamentAction.NextStep) },
                enabled = canProceed,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Next: Final Details", style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Step 2: Details ───────────────────────────────────────────────────────────

@Composable
private fun DetailsStep(
    state: CreateTournamentUiState,
    onAction: (CreateTournamentAction) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Tournament name
        item {
            SectionLabel("Tournament name")
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

        // Sport
        item {
            SectionLabel("Sport")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.sport,
                onValueChange = { onAction(CreateTournamentAction.SportChanged(it)) },
                placeholder = { Text("e.g. Football, Basketball…") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Start / End date
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel("Start date")
                    Text(
                        "Optional",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    DatePickerField(
                        label = "start date",
                        date = state.startDate,
                        onDateChanged = { onAction(CreateTournamentAction.StartDateChanged(it)) },
                        minDate = todayLocalDate(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel("End date")
                    Text(
                        "Optional",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    DatePickerField(
                        label = "end date",
                        date = state.endDate,
                        onDateChanged = { onAction(CreateTournamentAction.EndDateChanged(it)) },
                        minDate = state.startDate ?: todayLocalDate(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Description
        item {
            SectionLabel("Description")
            Text(
                "Optional",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.description,
                onValueChange = { onAction(CreateTournamentAction.DescriptionChanged(it)) },
                placeholder = { Text("Rules, location, notes…") },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Create button
        item {
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { onAction(CreateTournamentAction.CreateAndGenerate) },
                enabled = state.name.isNotBlank() && !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create Tournament", style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Shared sub-composables ────────────────────────────────────────────────────

/**
 * One roster entry. Display row with a trailing remove button — the multiplatform
 * material3 build doesn't ship the non-interactive SegmentedListItem overload yet
 * (androidx added it in 1.5.0-alpha23), so this uses the onClick overload with an
 * inert lambda.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ParticipantRow(
    participant: ParticipantEntry,
    index: Int,
    count: Int,
    onRemove: () -> Unit
) {
    SegmentedListItem(
        onClick = {},
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        leadingContent = {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Text(
                    "#${participant.seed}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    ) {
        Text(participant.displayName, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SizeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.height(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
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
private fun SectionCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            content()
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