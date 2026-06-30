package com.kmp.setplay.presentation.tournament.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kmp.setplay.domain.model.Match
import com.kmp.setplay.domain.model.MatchStatus
import com.kmp.setplay.domain.model.Team

// ── Match card dimensions (used for connector math) ──────────────────────────
private val MATCH_CARD_WIDTH  = 180.dp
private val MATCH_CARD_HEIGHT = 72.dp   // two team rows
private val ROUND_GAP         = 40.dp   // horizontal gap between rounds
private val MATCH_V_GAP       = 20.dp   // vertical gap between match cards in same round

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

    LaunchedEffect(state.tournamentDeleted) {
        if (state.tournamentDeleted) onBack()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.tournament?.name ?: "Tournament",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isOrganizer) {
                        IconButton(onClick = { onAction(TournamentDetailAction.ShowShareCode) }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share code")
                        }
                        OrganizerOverflowMenu(
                            isOwner = state.organizerRole != null,
                            onRename = { onAction(TournamentDetailAction.ShowRenameDialog) },
                            onEnd = { onAction(TournamentDetailAction.RequestEndTournament) },
                            onDelete = { onAction(TournamentDetailAction.RequestDeleteTournament) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                        text = { Text(tab.label()) }
                    )
                }
            }

            when (state.selectedTab) {
                DetailTab.BRACKET      -> BracketTab(state, onAction)
                DetailTab.STANDINGS    -> StandingsTab(state, onAction)
                DetailTab.ANNOUNCEMENTS -> AnnouncementsTab(state)
            }
        }

        if (state.isOrganizer) {
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

        // ── Rename tournament dialog ──────────────────────────────────────────
        if (state.showRenameDialog) {
            AlertDialog(
                onDismissRequest = { onAction(TournamentDetailAction.DismissRenameDialog) },
                title = { Text("Rename tournament") },
                text = {
                    OutlinedTextField(
                        value = state.renameInput,
                        onValueChange = { onAction(TournamentDetailAction.RenameInputChanged(it)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { onAction(TournamentDetailAction.ConfirmRename) },
                        enabled = state.renameInput.isNotBlank()
                    ) { Text("Rename") }
                },
                dismissButton = {
                    TextButton(onClick = { onAction(TournamentDetailAction.DismissRenameDialog) }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // ── Rename team dialog ────────────────────────────────────────────────
        state.renamingTeam?.let { team ->
            AlertDialog(
                onDismissRequest = { onAction(TournamentDetailAction.DismissRenameTeamDialog) },
                title = { Text("Rename \"${team.name}\"") },
                text = {
                    OutlinedTextField(
                        value = state.renameTeamInput,
                        onValueChange = { onAction(TournamentDetailAction.RenameTeamInputChanged(it)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { onAction(TournamentDetailAction.ConfirmRenameTeam) },
                        enabled = state.renameTeamInput.isNotBlank()
                    ) { Text("Rename") }
                },
                dismissButton = {
                    TextButton(onClick = { onAction(TournamentDetailAction.DismissRenameTeamDialog) }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // ── Share code bottom sheet ───────────────────────────────────────────
        if (state.showShareCode) {
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
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Share this code so others can view the tournament",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    val code = state.tournament?.inviteCode
                    if (code != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                code,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified,
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = {
                            clipboardManager.setText(AnnotatedString(code))
                            copied = true
                        }) {
                            Text(if (copied) "Copied!" else "Copy code")
                        }
                    } else {
                        Text(
                            "No share code yet — one is generated when you create the tournament.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }

        // ── End tournament confirmation ────────────────────────────────────────
        if (state.confirmEndTournament) {
            AlertDialog(
                onDismissRequest = { onAction(TournamentDetailAction.DismissEndDialog) },
                title = { Text("End tournament?") },
                text = { Text("This marks the tournament as completed. Scores will be locked.") },
                confirmButton = {
                    TextButton(onClick = { onAction(TournamentDetailAction.ConfirmEndTournament) }) {
                        Text("End", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onAction(TournamentDetailAction.DismissEndDialog) }) { Text("Cancel") }
                }
            )
        }

        // ── Delete tournament confirmation ─────────────────────────────────────
        if (state.confirmDeleteTournament) {
            AlertDialog(
                onDismissRequest = { onAction(TournamentDetailAction.DismissDeleteDialog) },
                title = { Text("Delete tournament?") },
                text = { Text("This permanently deletes the tournament and all its data. This can't be undone.") },
                confirmButton = {
                    TextButton(onClick = { onAction(TournamentDetailAction.ConfirmDeleteTournament) }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onAction(TournamentDetailAction.DismissDeleteDialog) }) { Text("Cancel") }
                }
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
    val matchesByRound = state.matches
        .groupBy { it.roundId }
        .entries
        .sortedBy { entry ->
            // Sort rounds by the minimum match number to preserve bracket order
            entry.value.minOf { it.matchNumber }
        }

    if (matchesByRound.isEmpty()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                "No bracket yet",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    val connectorColor = MaterialTheme.colorScheme.outlineVariant
    val totalRounds    = matchesByRound.size

    // Horizontal + vertical scroll for large brackets
    val hScroll = rememberScrollState()
    val vScroll = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(hScroll)
            .verticalScroll(vScroll)
    ) {
        // We lay enum class OrganizerRole {out rounds as a Row; connectors are drawn over/between them via Canvas
        Row(
            horizontalArrangement = Arrangement.spacedBy(ROUND_GAP),
            verticalAlignment    = Alignment.CenterVertically,
            modifier = Modifier.padding(24.dp)
        ) {
            matchesByRound.forEachIndexed { roundIndex, (_, roundMatches) ->
                val sortedMatches = roundMatches.sortedBy { it.matchNumber }
                val isLastRound   = roundIndex == totalRounds - 1

                // Round column
                Column(
                    verticalArrangement = Arrangement.spacedBy(MATCH_V_GAP),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Round label
                    Text(
                        roundLabel(roundIndex, totalRounds, sortedMatches.size),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .width(MATCH_CARD_WIDTH)
                    )

                    sortedMatches.forEach { match ->
                        val team1 = state.teams.find { it.id == match.team1Id }
                        val team2 = state.teams.find { it.id == match.team2Id }
                        val clickable = match.status == MatchStatus.SCHEDULED &&
                                match.team1Id != null && match.team2Id != null &&
                                state.isOrganizer

                        BracketMatchCard(
                            match    = match,
                            team1    = team1,
                            team2    = team2,
                            onClick  = if (clickable) {
                                { onAction(TournamentDetailAction.MatchClicked(match)) }
                            } else null
                        )
                    }
                }

                // Draw horizontal connector lines between this round and the next
                if (!isLastRound) {
                    val nextRoundMatchCount = matchesByRound.getOrNull(roundIndex + 1)?.value?.size ?: 0
                    BracketConnector(
                        fromMatchCount = sortedMatches.size,
                        toMatchCount   = nextRoundMatchCount,
                        color          = connectorColor
                    )
                }
            }
        }
    }
}

/**
 * Draws the bracket connector lines between two rounds.
 * Each pair of matches feeds into one match in the next round.
 */
@Composable
private fun BracketConnector(
    fromMatchCount: Int,
    toMatchCount: Int,
    color: Color
) {
    val cardHeightPx   = with(androidx.compose.ui.platform.LocalDensity.current) { MATCH_CARD_HEIGHT.toPx() }
    val matchVGapPx    = with(androidx.compose.ui.platform.LocalDensity.current) { MATCH_V_GAP.toPx() }
    val labelOffsetPx  = with(androidx.compose.ui.platform.LocalDensity.current) { 32.dp.toPx() } // label + padding above
    val connectorWidth = with(androidx.compose.ui.platform.LocalDensity.current) { (ROUND_GAP / 2).toPx() }

    // Total height = label + all cards + gaps between cards
    val totalH = labelOffsetPx +
            fromMatchCount * cardHeightPx +
            (fromMatchCount - 1) * matchVGapPx

    Canvas(
        modifier = Modifier
            .width(ROUND_GAP)
            .height(with(androidx.compose.ui.platform.LocalDensity.current) { totalH.toDp() })
            .padding(top = with(androidx.compose.ui.platform.LocalDensity.current) { labelOffsetPx.toDp() })
    ) {
        // Each pair of "from" cards connects to one "to" card
        for (i in 0 until toMatchCount) {
            val top    = i * 2       // top card in from-round
            val bottom = i * 2 + 1  // bottom card in from-round

            val topCenterY = top * (cardHeightPx + matchVGapPx) + cardHeightPx / 2f
            val botCenterY = bottom * (cardHeightPx + matchVGapPx) + cardHeightPx / 2f
            val midY       = (topCenterY + botCenterY) / 2f

            // Horizontal stub from top card
            drawLine(
                color       = color,
                start       = Offset(0f, topCenterY),
                end         = Offset(connectorWidth, topCenterY),
                strokeWidth = 2f,
                cap         = StrokeCap.Round
            )
            // Horizontal stub from bottom card
            drawLine(
                color       = color,
                start       = Offset(0f, botCenterY),
                end         = Offset(connectorWidth, botCenterY),
                strokeWidth = 2f,
                cap         = StrokeCap.Round
            )
            // Vertical bar joining them
            drawLine(
                color       = color,
                start       = Offset(connectorWidth, topCenterY),
                end         = Offset(connectorWidth, botCenterY),
                strokeWidth = 2f,
                cap         = StrokeCap.Round
            )
            // Horizontal line from mid point to next round
            drawLine(
                color       = color,
                start       = Offset(connectorWidth, midY),
                end         = Offset(size.width, midY),
                strokeWidth = 2f,
                cap         = StrokeCap.Round
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BracketMatchCard(
    match:   Match,
    team1:   Team?,
    team2:   Team?,
    onClick: (() -> Unit)?
) {
    val isBye       = match.status == MatchStatus.BYE
    val isCompleted = match.status == MatchStatus.COMPLETED

    val cardColor = when {
        isCompleted -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        isBye       -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        else        -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        isCompleted -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
        else        -> MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(10.dp),
        color = cardColor,
        modifier = Modifier
            .width(MATCH_CARD_WIDTH)
            .height(MATCH_CARD_HEIGHT)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val team1Name = team1?.name ?: "TBD"
            val team2Name = when {
                team2 != null  -> team2.name
                isBye          -> "BYE"
                else           -> "TBD"
            }

            BracketTeamRow(
                name     = team1Name,
                score    = match.score1,
                isWinner = match.winnerId != null && match.winnerId == match.team1Id,
                modifier = Modifier.weight(1f)
            )
            HorizontalDivider(color = borderColor)
            BracketTeamRow(
                name     = team2Name,
                score    = match.score2,
                isWinner = match.winnerId != null && match.winnerId == match.team2Id,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BracketTeamRow(
    name:     String,
    score:    Int?,
    isWinner: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(horizontal = 10.dp)
    ) {
        if (isWinner) {
            Surface(
                shape  = CircleShape,
                color  = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(6.dp)
            ) {}
            Spacer(Modifier.width(6.dp))
        } else {
            Spacer(Modifier.width(12.dp))
        }

        Text(
            name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
            color  = if (isWinner) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (score != null) {
            Spacer(Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isWinner)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    score.toString(),
                    style      = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isWinner) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

private fun roundLabel(index: Int, total: Int, matchCount: Int): String {
    if (matchCount == 1) return "Final"
    if (matchCount == 2) return "Semi-Finals"
    if (matchCount == 4) return "Quarter-Finals"
    return "Round ${index + 1}"
}

// ── Standings tab ─────────────────────────────────────────────────────────────

@Composable
private fun StandingsTab(
    state: TournamentDetailUiState,
    onAction: (TournamentDetailAction) -> Unit
) {
    if (state.standings.isEmpty()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text("No standings yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
                Text("#", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Team", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("W", modifier = Modifier.width(32.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("L", modifier = Modifier.width(32.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Pts", modifier = Modifier.width(40.dp), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()
        }
        items(
            state.standings.sortedByDescending { it.points }.mapIndexed { i, s -> i + 1 to s }
        ) { (rank, standing) ->
            val team = state.teams.find { it.id == standing.teamId }
            Card(
                onClick = {
                    if (state.isOrganizer && team != null) {
                        onAction(TournamentDetailAction.ShowRenameTeamDialog(team))
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (rank == 1)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp)
                ) {
                    Text(
                        if (rank == 1) "🥇" else if (rank == 2) "🥈" else if (rank == 3) "🥉" else "$rank",
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
        title = { Text("Enter Score", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = score1,
                    onValueChange = onScore1Changed,
                    label = { Text(team1Name) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = score2,
                    onValueChange = onScore2Changed,
                    label = { Text(team2Name) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = score1.toIntOrNull() != null && score2.toIntOrNull() != null,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun DetailTab.label() = when (this) {
    DetailTab.BRACKET       -> "Bracket"
    DetailTab.STANDINGS     -> "Standings"
    DetailTab.ANNOUNCEMENTS -> "Announcements"
}

// ── Organizer overflow menu ───────────────────────────────────────────────────

@Composable
private fun OrganizerOverflowMenu(
    isOwner: Boolean,
    onRename: () -> Unit,
    onEnd: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Rename tournament") },
                leadingIcon = { Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = null) },
                onClick = { expanded = false; onRename() }
            )
            if (isOwner) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("End tournament", color = MaterialTheme.colorScheme.error) },
                    onClick = { expanded = false; onEnd() }
                )
                DropdownMenuItem(
                    text = { Text("Delete tournament", color = MaterialTheme.colorScheme.error) },
                    onClick = { expanded = false; onDelete() }
                )
            }
        }
    }
}