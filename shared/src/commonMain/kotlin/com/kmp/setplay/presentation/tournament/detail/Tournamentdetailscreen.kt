package com.kmp.setplay.presentation.tournament.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.EmojiEvents
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.sp
import com.kmp.setplay.domain.model.Match
import com.kmp.setplay.domain.model.MatchStatus
import com.kmp.setplay.domain.model.OrganizerRole
import com.kmp.setplay.domain.model.Team
import com.kmp.setplay.presentation.common.formatMatchSchedule

// ── Match card dimensions (used for connector math) ──────────────────────────
private val MATCH_CARD_WIDTH   = 200.dp
private val MATCH_CARD_HEIGHT  = 72.dp   // two team rows
private val SCORE_COL_WIDTH    = 44.dp   // dark score chip column on the right
private val VS_BADGE_SIZE      = 22.dp   // "vs" circle overlapping the divider
private val BLOCK_LABEL_HEIGHT = 22.dp   // "QF · Game 1" label above the card
private val BLOCK_CAPTION_HEIGHT = 18.dp // "Set date & time" caption below the card
private val ROUND_GAP          = 40.dp   // horizontal gap between rounds
private val MATCH_V_GAP        = 20.dp   // vertical gap between match blocks in same round

// Solid near-black score chip so it reads consistently across light/dark themes
private val SCORE_CHIP_DARK = Color(0xFF1E1E24)

// Title and back/actions for this screen are rendered by MainAppNavigation's shared
// Scaffold topBar (see TournamentDetailTopBarActions below) rather than by this
// composable, which only renders body content.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentDetailScreen(
    state: TournamentDetailUiState,
    onAction: (TournamentDetailAction) -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
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

    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading && state.tournament == null -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize().padding(contentPadding)
                ) {
                    CircularProgressIndicator()
                }
            }

            state.accessRevoked -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize().padding(contentPadding).padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Access revoked",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "The organizer has revoked your access to this tournament via share code.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
                    val tabs = state.availableTabs
                    val activeTab = if (state.selectedTab in tabs) state.selectedTab else tabs.first()

                    PrimaryTabRow(selectedTabIndex = tabs.indexOf(activeTab)) {
                        tabs.forEach { tab ->
                            Tab(
                                selected = activeTab == tab,
                                onClick = { onAction(TournamentDetailAction.TabSelected(tab)) },
                                text = { Text(tab.label()) }
                            )
                        }
                    }

                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = {
                            if (targetState.ordinal >= initialState.ordinal) {
                                slideInHorizontally(initialOffsetX = { it })
                                    .togetherWith(slideOutHorizontally(targetOffsetX = { -it }))
                            } else {
                                slideInHorizontally(initialOffsetX = { -it })
                                    .togetherWith(slideOutHorizontally(targetOffsetX = { it }))
                            }
                        },
                        label = "detailTabTransition"
                    ) { tab ->
                        when (tab) {
                            DetailTab.BRACKET       -> BracketTab(state, onAction)
                            DetailTab.STANDINGS     -> StandingsTab(state, onAction)
                            DetailTab.ANNOUNCEMENTS -> AnnouncementsTab(state)
                            DetailTab.PARTICIPANTS  -> ParticipantsTab(state)
                        }
                    }
                }
            }
        }

        // ── Match schedule dialog ───────────────────────────────────────────────
        state.schedulingMatch?.let { match ->
            com.kmp.setplay.presentation.common.MatchDateTimePickerDialog(
                initial = match.scheduledAt,
                onConfirm = { onAction(TournamentDetailAction.ConfirmSchedule(it)) },
                onClear = if (match.scheduledAt != null) {
                    { onAction(TournamentDetailAction.ClearSchedule) }
                } else null,
                onDismiss = { onAction(TournamentDetailAction.DismissScheduleDialog) }
            )
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

        // ── Access list bottom sheet (private tournaments, organizer only) ──────
        if (state.showAccessList) {
            val sheetState = rememberModalBottomSheetState()

            ModalBottomSheet(
                onDismissRequest = { onAction(TournamentDetailAction.DismissAccessList) },
                sheetState = sheetState
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                    Text("Access list", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Everyone who has opened this tournament with the share code",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))

                    when {
                        state.isLoadingViewers -> Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                        ) { CircularProgressIndicator() }

                        state.viewers.isEmpty() -> Text(
                            "No one has viewed this tournament via share code yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )

                        else -> Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(bottom = 24.dp)
                        ) {
                            state.viewers.forEach { viewer ->
                                AccessListRow(
                                    viewer = viewer,
                                    onToggle = { onAction(TournamentDetailAction.ToggleViewerAccess(viewer)) }
                                )
                            }
                        }
                    }
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

        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

/**
 * Top app bar actions (Share code / organizer overflow menu) for the tournament detail
 * screen. Rendered by MainAppNavigation's shared Scaffold topBar `actions` slot rather
 * than by [TournamentDetailScreen] itself, since titles and navigation/back handling for
 * the whole main app are centralized there.
 */
@Composable
fun TournamentDetailTopBarActions(
    state: TournamentDetailUiState,
    onAction: (TournamentDetailAction) -> Unit
) {
    if (state.isOrganizer) {
        IconButton(onClick = { onAction(TournamentDetailAction.ShowShareCode) }) {
            Icon(Icons.Filled.Share, contentDescription = "Share code")
        }
        OrganizerOverflowMenu(
            isOwner = state.organizerRole != null,
            isPrivate = state.tournament?.isPublic == false,
            onRename = { onAction(TournamentDetailAction.ShowRenameDialog) },
            onShowAccessList = { onAction(TournamentDetailAction.ShowAccessList) },
            onEnd = { onAction(TournamentDetailAction.RequestEndTournament) },
            onDelete = { onAction(TournamentDetailAction.RequestDeleteTournament) }
        )
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
        // Rounds laid out as a Row; connectors are drawn between them via Canvas
        Row(
            horizontalArrangement = Arrangement.spacedBy(ROUND_GAP),
            verticalAlignment    = Alignment.CenterVertically,
            modifier = Modifier.padding(24.dp)
        ) {
            matchesByRound.forEachIndexed { roundIndex, (_, roundMatches) ->
                val sortedMatches   = roundMatches.sortedBy { it.matchNumber }
                val isLastRound     = roundIndex == totalRounds - 1
                val roundShort      = roundShortLabel(sortedMatches.size)
                val isFinalRound    = sortedMatches.size == 1

                // Round column — each match is its own label + card + caption block
                Column(verticalArrangement = Arrangement.spacedBy(MATCH_V_GAP)) {
                    sortedMatches.forEachIndexed { matchIndex, match ->
                        val team1 = state.teams.find { it.id == match.team1Id }
                        val team2 = state.teams.find { it.id == match.team2Id }
                        val clickable = match.status == MatchStatus.SCHEDULED &&
                                match.team1Id != null && match.team2Id != null &&
                                state.isOrganizer

                        Column(horizontalAlignment = Alignment.Start) {
                            MatchLabel(
                                text = matchLabel(roundShort, isFinalRound, matchIndex),
                                isFinal = isFinalRound
                            )
                            Spacer(Modifier.height(6.dp))
                            BracketMatchCard(
                                match    = match,
                                team1    = team1,
                                team2    = team2,
                                onClick  = if (clickable) {
                                    { onAction(TournamentDetailAction.MatchClicked(match)) }
                                } else null,
                                onNameClick = if (state.isOrganizer) { team ->
                                    onAction(TournamentDetailAction.ShowRenameTeamDialog(team))
                                } else null
                            )
                            Spacer(Modifier.height(4.dp))
                            TextButton(
                                onClick = { onAction(TournamentDetailAction.ShowScheduleDialog(match)) },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                modifier = Modifier.height(BLOCK_CAPTION_HEIGHT)
                            ) {
                                Text(
                                    match.scheduledAt?.let {
                                        formatMatchSchedule(it)
                                    } ?: "Set date & time",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
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

@Composable
private fun MatchLabel(text: String, isFinal: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isFinal) {
            Icon(
                Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
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
    val density = androidx.compose.ui.platform.LocalDensity.current
    val cardHeightPx    = with(density) { MATCH_CARD_HEIGHT.toPx() }
    val matchVGapPx     = with(density) { MATCH_V_GAP.toPx() }
    val labelHeightPx   = with(density) { BLOCK_LABEL_HEIGHT.toPx() }
    val captionHeightPx = with(density) { BLOCK_CAPTION_HEIGHT.toPx() }
    val labelGapPx      = with(density) { 6.dp.toPx() }
    val captionGapPx    = with(density) { 4.dp.toPx() }
    val connectorWidth  = with(density) { (ROUND_GAP / 2).toPx() }

    // Each match occupies a "block": label + spacing, the card itself, then a caption
    val blockHeightPx  = labelHeightPx + labelGapPx + cardHeightPx + captionGapPx + captionHeightPx
    // The card's vertical center sits this far down from the top of its block
    val cardCenterOffsetPx = labelHeightPx + labelGapPx + cardHeightPx / 2f

    val totalH = fromMatchCount * blockHeightPx + (fromMatchCount - 1) * matchVGapPx

    Canvas(
        modifier = Modifier
            .width(ROUND_GAP)
            .height(with(density) { totalH.toDp() })
    ) {
        // Each pair of "from" cards connects to one "to" card
        for (i in 0 until toMatchCount) {
            val top    = i * 2       // top card in from-round
            val bottom = i * 2 + 1  // bottom card in from-round

            val topCenterY = top * (blockHeightPx + matchVGapPx) + cardCenterOffsetPx
            val botCenterY = bottom * (blockHeightPx + matchVGapPx) + cardCenterOffsetPx
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
    onClick: (() -> Unit)?,
    onNameClick: ((Team) -> Unit)? = null
) {
    val isBye       = match.status == MatchStatus.BYE
    val isCompleted = match.status == MatchStatus.COMPLETED

    val team1Name = team1?.name ?: "TBD"
    val team2Name = when {
        team2 != null -> team2.name
        isBye         -> "—"
        else          -> "TBD"
    }

    val team1IsWinner = match.winnerId != null && match.winnerId == match.team1Id
    val team2IsWinner = match.winnerId != null && match.winnerId == match.team2Id

    // When a slot auto-advances due to BYE, the known team's row is highlighted
    val team1AutoAdvance = isBye && team1 != null
    val team2AutoAdvance = isBye && team2 != null

    val borderColor = if (isCompleted)
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
    else MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = Modifier
            .width(MATCH_CARD_WIDTH)
            .height(MATCH_CARD_HEIGHT)
    ) {
        Surface(
            onClick = onClick ?: {},
            enabled = onClick != null,
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // ── Name column ─────────────────────────────────────────────
                Column(modifier = Modifier.weight(1f)) {
                    BracketNameRow(
                        name = team1Name,
                        isWinner = team1IsWinner,
                        isAutoAdvance = team1AutoAdvance,
                        onClick = if (onNameClick != null && team1 != null) { { onNameClick(team1) } } else null,
                        modifier = Modifier.weight(1f)
                    )
                    HorizontalDivider(color = borderColor)
                    BracketNameRow(
                        name = team2Name,
                        isWinner = team2IsWinner,
                        isAutoAdvance = team2AutoAdvance,
                        onClick = if (onNameClick != null && team2 != null) { { onNameClick(team2) } } else null,
                        modifier = Modifier.weight(1f)
                    )
                }

                // ── Score column ────────────────────────────────────────────
                if (isBye) {
                    ByeChip(modifier = Modifier.width(SCORE_COL_WIDTH).fillMaxHeight())
                } else {
                    Column(modifier = Modifier.width(SCORE_COL_WIDTH)) {
                        ScoreChip(
                            score = match.score1,
                            isWinner = team1IsWinner,
                            isCompleted = isCompleted,
                            modifier = Modifier.weight(1f)
                        )
                        ScoreChip(
                            score = match.score2,
                            isWinner = team2IsWinner,
                            isCompleted = isCompleted,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ── "vs" badge overlapping the divider between the two rows ─────────
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .size(VS_BADGE_SIZE)
                .align(Alignment.TopStart)
                .offset(
                    x = MATCH_CARD_WIDTH - SCORE_COL_WIDTH - (VS_BADGE_SIZE / 2),
                    y = (MATCH_CARD_HEIGHT / 2) - (VS_BADGE_SIZE / 2)
                )
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    "vs",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BracketNameRow(
    name: String,
    isWinner: Boolean,
    isAutoAdvance: Boolean,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isWinner      -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        isAutoAdvance -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
        else          -> Color.Transparent
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(horizontal = 10.dp)
    ) {
        Text(
            name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isWinner || isAutoAdvance) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isWinner      -> MaterialTheme.colorScheme.primary
                isAutoAdvance -> MaterialTheme.colorScheme.onTertiaryContainer
                else          -> MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ScoreChip(
    score: Int?,
    isWinner: Boolean,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isWinner && isCompleted) MaterialTheme.colorScheme.primary else SCORE_CHIP_DARK
            )
    ) {
        Text(
            score?.toString() ?: "–",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isWinner && isCompleted)
                MaterialTheme.colorScheme.onPrimary
            else Color.White.copy(alpha = if (score != null) 0.85f else 0.5f)
        )
    }
}

@Composable
private fun ByeChip(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            "BYE".forEach { letter ->
                Text(
                    letter.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

/** Short round code used to build per-match labels, e.g. "QF" -> "QF · Game 2" */
private fun roundShortLabel(matchCountInRound: Int): String = when (matchCountInRound) {
    1    -> "Final"
    2    -> "SF"
    4    -> "QF"
    else -> "R"
}

private fun matchLabel(roundShort: String, isFinal: Boolean, matchIndexInRound: Int): String =
    if (isFinal) "Final" else "$roundShort · Game ${matchIndexInRound + 1}"

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
    DetailTab.PARTICIPANTS  -> "Participants"
}

// ── Participants tab (public tournaments only) ─────────────────────────────────

@Composable
private fun ParticipantsTab(state: TournamentDetailUiState) {
    if (state.organizers.isEmpty() && state.teams.isEmpty()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text("No participants yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (state.organizers.isNotEmpty()) {
            item {
                Text(
                    "Organizers",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(state.organizers, key = { "org_${it.id}" }) { organizer ->
                ParticipantRow(
                    name = if (organizer.role == OrganizerRole.OWNER) "Owner" else "Organizer",
                    subtitle = organizer.userId
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        item {
            Text(
                "Players / Teams",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        if (state.teams.isEmpty()) {
            item {
                Text(
                    "No one has joined yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(state.teams, key = { "team_${it.id}" }) { team ->
                ParticipantRow(name = team.name, subtitle = if (team.seed != null) "Seed ${team.seed}" else null)
            }
        }
    }
}

@Composable
private fun ParticipantRow(name: String, subtitle: String?) {
    Card(shape = RoundedCornerShape(8.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AccessListRow(
    viewer: com.kmp.setplay.domain.model.ShareViewer,
    onToggle: () -> Unit
) {
    Card(shape = RoundedCornerShape(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    // No display-name profile yet — show a short, readable id fragment.
                    "Viewer · ${viewer.userId.take(8)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Last viewed ${formatMatchSchedule(viewer.lastViewedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onToggle) {
                Text(
                    if (viewer.revoked) "Restore" else "Revoke",
                    color = if (viewer.revoked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ── Organizer overflow menu ───────────────────────────────────────────────────

@Composable
private fun OrganizerOverflowMenu(
    isOwner: Boolean,
    isPrivate: Boolean,
    onRename: () -> Unit,
    onShowAccessList: () -> Unit,
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
            if (isPrivate) {
                DropdownMenuItem(
                    text = { Text("Access list") },
                    leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                    onClick = { expanded = false; onShowAccessList() }
                )
            }
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