package com.kmp.setplay.presentation.tournament.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmp.setplay.domain.model.Match
import com.kmp.setplay.domain.model.MatchStatus
import com.kmp.setplay.domain.model.Team
import com.kmp.setplay.presentation.common.formatMatchSchedule
import com.kmp.setplay.presentation.tournament.detail.TournamentDetailAction
import com.kmp.setplay.presentation.tournament.detail.TournamentDetailUiState

private val MATCH_CARD_WIDTH = 200.dp
private val MATCH_CARD_HEIGHT = 72.dp
private val SCORE_COL_WIDTH = 44.dp
private val VS_BADGE_SIZE = 22.dp
private val BLOCK_LABEL_HEIGHT = 22.dp
private val BLOCK_CAPTION_HEIGHT = 18.dp
private val ROUND_GAP = 40.dp
private val MATCH_VERTICAL_GAP = 20.dp

@Composable
fun BracketTab(
    state: TournamentDetailUiState,
    onAction: (TournamentDetailAction) -> Unit
) {
    val matchesByRound = state.matches.groupBy { it.roundId }
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

    val loserTargets = state.matches.mapNotNull { it.nextLoserMatchId }.toSet()
    val thirdPlaceRoundIds = matchesByRound
        .filterValues { roundMatches -> roundMatches.size == 1 && roundMatches.first().id in loserTargets }
        .keys
    val thirdPlaceMatch = thirdPlaceRoundIds.firstOrNull()?.let { matchesByRound.getValue(it).first() }
    val mainRounds = matchesByRound
        .filterKeys { it !in thirdPlaceRoundIds }
        .entries
        .sortedByDescending { it.value.size }
        .map { entry -> entry.value.sortedBy { it.matchNumber } }

    val unitHeight = BLOCK_LABEL_HEIGHT + 6.dp + MATCH_CARD_HEIGHT + 4.dp + BLOCK_CAPTION_HEIGHT + MATCH_VERTICAL_GAP
    val totalHeight = unitHeight * mainRounds.first().size
    val connectorColor = MaterialTheme.colorScheme.outlineVariant
    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(horizontalScroll)
            .verticalScroll(verticalScroll)
    ) {
        Row(modifier = Modifier.padding(24.dp)) {
            mainRounds.forEachIndexed { roundIndex, roundMatches ->
                val slotHeight = unitHeight * (1 shl roundIndex)
                val isFinalRound = roundIndex == mainRounds.lastIndex && roundMatches.size == 1
                val roundCode = roundShortLabel(roundMatches.size)

                Column {
                    roundMatches.forEachIndexed { matchIndex, match ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.height(slotHeight)
                        ) {
                            MatchBlock(
                                match = match,
                                label = matchLabel(roundCode, isFinalRound, matchIndex),
                                isFinal = isFinalRound,
                                state = state,
                                onAction = onAction
                            )
                        }
                    }
                }

                if (roundIndex != mainRounds.lastIndex) {
                    BracketConnector(
                        pairCount = mainRounds[roundIndex + 1].size,
                        fromSlotHeight = slotHeight,
                        totalHeight = totalHeight,
                        color = connectorColor
                    )
                }
            }

            thirdPlaceMatch?.let { match ->
                Spacer(Modifier.width(ROUND_GAP))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.height(totalHeight)
                ) {
                    MatchBlock(
                        match = match,
                        label = "3rd Place",
                        isFinal = false,
                        state = state,
                        onAction = onAction
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchBlock(
    match: Match,
    label: String,
    isFinal: Boolean,
    state: TournamentDetailUiState,
    onAction: (TournamentDetailAction) -> Unit
) {
    val team1 = state.teams.find { it.id == match.team1Id }
    val team2 = state.teams.find { it.id == match.team2Id }
    val canEnterScore =
        match.status == MatchStatus.SCHEDULED &&
            match.team1Id != null &&
            match.team2Id != null &&
            state.isOrganizer

    Column(horizontalAlignment = Alignment.Start) {
        MatchLabel(text = label, isFinal = isFinal)
        Spacer(Modifier.height(6.dp))
        BracketMatchCard(
            match = match,
            team1 = team1,
            team2 = team2,
            onClick = if (canEnterScore) { { onAction(TournamentDetailAction.MatchClicked(match)) } } else null,
            onNameClick = if (state.isOrganizer) {
                { team -> onAction(TournamentDetailAction.ShowRenameTeamDialog(team)) }
            } else {
                null
            }
        )
        Spacer(Modifier.height(4.dp))
        TextButton(
            onClick = { onAction(TournamentDetailAction.ShowScheduleDialog(match)) },
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            modifier = Modifier.height(BLOCK_CAPTION_HEIGHT)
        ) {
            Text(
                match.scheduledAt?.let(::formatMatchSchedule) ?: "Set date & time",
                style = MaterialTheme.typography.labelSmall
            )
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

@Composable
private fun BracketConnector(
    pairCount: Int,
    fromSlotHeight: androidx.compose.ui.unit.Dp,
    totalHeight: androidx.compose.ui.unit.Dp,
    color: Color
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val slotHeightPx = with(density) { fromSlotHeight.toPx() }

    Canvas(
        modifier = Modifier
            .width(ROUND_GAP)
            .height(totalHeight)
    ) {
        val midpointX = size.width / 2f
        repeat(pairCount) { index ->
            val topCenterY = (2 * index) * slotHeightPx + slotHeightPx / 2f
            val bottomCenterY = (2 * index + 1) * slotHeightPx + slotHeightPx / 2f
            val midpointY = (topCenterY + bottomCenterY) / 2f

            drawLine(color, Offset(0f, topCenterY), Offset(midpointX, topCenterY), 2f, StrokeCap.Round)
            drawLine(color, Offset(0f, bottomCenterY), Offset(midpointX, bottomCenterY), 2f, StrokeCap.Round)
            drawLine(color, Offset(midpointX, topCenterY), Offset(midpointX, bottomCenterY), 2f, StrokeCap.Round)
            drawLine(color, Offset(midpointX, midpointY), Offset(size.width, midpointY), 2f, StrokeCap.Round)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BracketMatchCard(
    match: Match,
    team1: Team?,
    team2: Team?,
    onClick: (() -> Unit)?,
    onNameClick: ((Team) -> Unit)? = null
) {
    val isBye = match.status == MatchStatus.BYE
    val isCompleted = match.status == MatchStatus.COMPLETED
    val team1IsWinner = match.winnerId != null && match.winnerId == match.team1Id
    val team2IsWinner = match.winnerId != null && match.winnerId == match.team2Id
    val borderColor = if (isCompleted) {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Box(modifier = Modifier.width(MATCH_CARD_WIDTH).height(MATCH_CARD_HEIGHT)) {
        Surface(
            onClick = onClick ?: {},
            enabled = onClick != null,
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxSize().border(1.dp, borderColor, RoundedCornerShape(10.dp))
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.weight(1f)) {
                    BracketNameRow(
                        name = team1?.name ?: "TBD",
                        isWinner = team1IsWinner,
                        isAutoAdvance = isBye && team1 != null,
                        onClick = team1?.takeIf { onNameClick != null }?.let { { onNameClick?.invoke(it) } },
                        modifier = Modifier.weight(1f)
                    )
                    HorizontalDivider(color = borderColor)
                    BracketNameRow(
                        name = when {
                            team2 != null -> team2.name
                            isBye -> "—"
                            else -> "TBD"
                        },
                        isWinner = team2IsWinner,
                        isAutoAdvance = isBye && team2 != null,
                        onClick = team2?.takeIf { onNameClick != null }?.let { { onNameClick?.invoke(it) } },
                        modifier = Modifier.weight(1f)
                    )
                }

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
    val backgroundColor = when {
        isWinner -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        isAutoAdvance -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
        else -> Color.Transparent
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp)
    ) {
        Text(
            name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isWinner || isAutoAdvance) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isWinner -> MaterialTheme.colorScheme.primary
                isAutoAdvance -> MaterialTheme.colorScheme.onTertiaryContainer
                else -> MaterialTheme.colorScheme.onSurface
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
                if (isWinner && isCompleted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.inverseSurface
            )
    ) {
        Text(
            score?.toString() ?: "–",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isWinner && isCompleted) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = if (score != null) 0.85f else 0.5f)
            }
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

private fun roundShortLabel(matchCountInRound: Int): String = when (matchCountInRound) {
    1 -> "Final"
    2 -> "SF"
    4 -> "QF"
    else -> "R"
}

private fun matchLabel(roundCode: String, isFinal: Boolean, matchIndexInRound: Int): String =
    if (isFinal) "Final" else "$roundCode · Game ${matchIndexInRound + 1}"
