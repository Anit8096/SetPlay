package com.kmp.setplay.presentation.tournament.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kmp.setplay.presentation.tournament.detail.components.BracketTab
import com.kmp.setplay.presentation.tournament.detail.components.NoticeTab
import com.kmp.setplay.presentation.tournament.detail.components.ParticipantsTab
import com.kmp.setplay.presentation.tournament.detail.components.StandingsTab
import com.kmp.setplay.presentation.tournament.detail.components.TournamentDetailOverlays

// Title and top-bar actions are rendered by MainAppNavigation's shared Scaffold topBar.
// This composable renders only the screen body and screen-owned overlays.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
        TournamentDetailContent(
            state = state,
            onAction = onAction,
            contentPadding = contentPadding
        )
        TournamentDetailOverlays(state = state, onAction = onAction)
        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun TournamentDetailContent(
    state: TournamentDetailUiState,
    onAction: (TournamentDetailAction) -> Unit,
    contentPadding: PaddingValues
) {
    when {
        state.isLoading && state.tournament == null -> LoadingState(contentPadding)
        state.accessRevoked -> AccessRevokedState(contentPadding)
        else -> TournamentDetailTabs(state, onAction, contentPadding)
    }
}

@Composable
private fun LoadingState(contentPadding: PaddingValues) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().padding(contentPadding)
    ) {
        LoadingIndicator()
    }
}

@Composable
private fun AccessRevokedState(contentPadding: PaddingValues) {
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

@Composable
private fun TournamentDetailTabs(
    state: TournamentDetailUiState,
    onAction: (TournamentDetailAction) -> Unit,
    contentPadding: PaddingValues
) {
    val availableTabs = state.availableTabs
    val activeTab = state.selectedTab.takeIf { it in availableTabs } ?: availableTabs.first()

    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PrimaryTabRow(
            selectedTabIndex = availableTabs.indexOf(activeTab),
            modifier = Modifier.fillMaxWidth()
        ) {
            availableTabs.forEach { tab ->
                Tab(
                    selected = activeTab == tab,
                    onClick = { onAction(TournamentDetailAction.TabSelected(tab)) },
                    text = {
                        Text(tab.label(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
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
                DetailTab.BRACKET -> BracketTab(state, onAction)
                DetailTab.STANDINGS -> StandingsTab(state, onAction)
                DetailTab.NOTICE -> NoticeTab(state)
                DetailTab.PARTICIPANTS -> ParticipantsTab(state)
            }
        }
    }
}

private fun DetailTab.label() = when (this) {
    DetailTab.BRACKET -> "Bracket"
    DetailTab.STANDINGS -> "Standings"
    DetailTab.NOTICE -> "Notice"
    DetailTab.PARTICIPANTS -> "Participants"
}
