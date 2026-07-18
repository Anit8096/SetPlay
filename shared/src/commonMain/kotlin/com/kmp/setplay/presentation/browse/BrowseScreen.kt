package com.kmp.setplay.presentation.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.kmp.setplay.domain.model.BracketFormat
import com.kmp.setplay.domain.model.Tournament
import com.kmp.setplay.domain.model.TournamentStatus
import com.kmp.setplay.domain.repository.ParticipationSummary
import com.kmp.setplay.presentation.common.ContentContainer
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

expect fun isAndroidPlatform(): Boolean


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BrowseScreen(
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
    onTournamentSelected: (String) -> Unit = {},
    onJoinTournament: () -> Unit = {},
    viewModel: BrowseViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onAction(BrowseAction.ErrorShown)
        }
    }

    // Large / extra-large windows get a bigger FAB and wider (24dp) margins per M3.
    val isExpandedWindow = currentWindowAdaptiveInfoV2().windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    Scaffold(
        modifier = modifier.fillMaxSize().padding(contentPadding),
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState.selectedSubTab == BrowseSubTab.DISCOVER) {
                DiscoverFabMenu(
                    expandedWindow = isExpandedWindow,
                    onScanQr = {
                        scope.launch {
                            snackbarHostState.showSnackbar("QR scanning is coming soon")
                        }
                    },
                    onRefresh = { viewModel.onAction(BrowseAction.Refresh) },
                    onJoinWithCode = onJoinTournament
                )
            }
        }
    ) { fabPadding ->
        ContentContainer(
            modifier = Modifier.fillMaxSize().padding(fabPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PrimaryTabRow(
                    selectedTabIndex = uiState.selectedSubTab.ordinal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BrowseSubTab.entries.forEach { tab ->
                        Tab(
                            selected = uiState.selectedSubTab == tab,
                            onClick = { viewModel.onAction(BrowseAction.SubTabSelected(tab)) },
                            text = {
                                Text(tab.label(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        )
                    }
                }

                when (uiState.selectedSubTab) {
                    BrowseSubTab.DISCOVER -> DiscoverContent(
                        uiState = uiState,
                        onFormatFilter = { viewModel.onAction(BrowseAction.FormatFilterChanged(it)) },
                        onStatusFilter = { viewModel.onAction(BrowseAction.StatusFilterChanged(it)) },
                        onClearFilters = { viewModel.onAction(BrowseAction.ClearFilters) },
                        filtered = viewModel.filteredTournaments,
                        onRefresh = { viewModel.onAction(BrowseAction.Refresh) },
                        onJoinClicked = { viewModel.onAction(BrowseAction.JoinClicked(it)) }
                    )
                    BrowseSubTab.JOINED_LIVE -> ComingSoon("Tournaments you've joined will show up here")
                    BrowseSubTab.ORGANIZING_LIVE -> OrganizingContent(
                        isLoading = uiState.isOrganizingLoading,
                        tournaments = uiState.organizingTournaments,
                        onTournamentSelected = onTournamentSelected
                    )
                }
            }

            // ── Join dialog ──────────────────────────────────────────────────────────
            uiState.joinDialogFor?.let { tournament ->
                AlertDialog(
                    onDismissRequest = { viewModel.onAction(BrowseAction.DismissJoinDialog) },
                    title = { Text("Join ${tournament.name}") },
                    text = {
                        OutlinedTextField(
                            value = uiState.joinNameInput,
                            onValueChange = { viewModel.onAction(BrowseAction.JoinNameChanged(it)) },
                            placeholder = { Text("Your player or team name") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.onAction(BrowseAction.ConfirmJoin) },
                            enabled = uiState.joinNameInput.isNotBlank() && uiState.joiningTournamentId != tournament.id
                        ) { Text("Join") }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.onAction(BrowseAction.DismissJoinDialog) }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DiscoverFabMenu(
    expandedWindow: Boolean,
    onScanQr: () -> Unit,
    onRefresh: () -> Unit,
    onJoinWithCode: () -> Unit
) {
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    // Android surfaces QR scanning; other platforms fall back to a manual refresh.
    val secondaryIsScan = isAndroidPlatform()

    FloatingActionButtonMenu(
        expanded = menuExpanded,
        // Bumps the trailing/bottom margin from the Scaffold's 16dp to 24dp on large windows.
        modifier = if (expandedWindow) Modifier.padding(8.dp) else Modifier,
        button = {
            ToggleFloatingActionButton(
                checked = menuExpanded,
                onCheckedChange = { menuExpanded = it },
                containerSize = if (expandedWindow) {
                    ToggleFloatingActionButtonDefaults.containerSizeMedium()
                } else {
                    ToggleFloatingActionButtonDefaults.containerSize()
                }
            ) {
                // The FAB icon morphs from "add" into a close button as the menu opens.
                val fabIcon by remember {
                    derivedStateOf {
                        if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.Add
                    }
                }
                Icon(
                    painter = rememberVectorPainter(fabIcon),
                    contentDescription = if (menuExpanded) "Close actions" else "More actions",
                    modifier = Modifier.animateIcon({ checkedProgress })
                )
            }
        }
    ) {
        FloatingActionButtonMenuItem(
            onClick = {
                menuExpanded = false
                if (secondaryIsScan) onScanQr() else onRefresh()
            },
            icon = {
                Icon(
                    if (secondaryIsScan) Icons.Default.QrCodeScanner else Icons.Default.Refresh,
                    contentDescription = null
                )
            },
            text = { Text(if (secondaryIsScan) "Scan QR" else "Refresh") }
        )
        FloatingActionButtonMenuItem(
            onClick = {
                menuExpanded = false
                onJoinWithCode()
            },
            icon = { Icon(Icons.Default.Key, contentDescription = null) },
            text = { Text("Join with code") }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DiscoverContent(
    uiState: BrowseUiState,
    onFormatFilter: (BracketFormat?) -> Unit,
    onStatusFilter: (TournamentStatus?) -> Unit,
    onClearFilters: () -> Unit,
    filtered: List<Tournament>,
    onRefresh: () -> Unit,
    onJoinClicked: (Tournament) -> Unit
) {
    val pullState = rememberPullToRefreshState()
    PullToRefreshBox(
        state = pullState,
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullState,
                isRefreshing = uiState.isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.statusFilter == null && uiState.formatFilter == null,
                        onClick = onClearFilters,
                        label = { Text("All") }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.statusFilter == TournamentStatus.REGISTRATION,
                        onClick = {
                            onStatusFilter(
                                if (uiState.statusFilter == TournamentStatus.REGISTRATION) null
                                else TournamentStatus.REGISTRATION
                            )
                        },
                        label = { Text("Open") }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.statusFilter == TournamentStatus.IN_PROGRESS,
                        onClick = {
                            onStatusFilter(
                                if (uiState.statusFilter == TournamentStatus.IN_PROGRESS) null
                                else TournamentStatus.IN_PROGRESS
                            )
                        },
                        label = { Text("In Progress") }
                    )
                }
                items(BracketFormat.entries.toList()) { format ->
                    FilterChip(
                        selected = uiState.formatFilter == format,
                        onClick = { onFormatFilter(if (uiState.formatFilter == format) null else format) },
                        label = { Text(format.shortLabel()) }
                    )
                }
            }

            when {
                uiState.isLoading -> Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) { LoadingIndicator() }

                filtered.isEmpty() -> Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize().padding(24.dp)
                ) {
                    Text(
                        "No public tournaments right now — check back soon",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.id }) { tournament ->
                        PublicJoinCard(
                            tournament = tournament,
                            summary = uiState.participation[tournament.id],
                            isJoining = uiState.joiningTournamentId == tournament.id,
                            onJoinClick = { onJoinClicked(tournament) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OrganizingContent(
    isLoading: Boolean,
    tournaments: List<Tournament>,
    onTournamentSelected: (String) -> Unit
) {
    when {
        isLoading -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) { LoadingIndicator() }

        tournaments.isEmpty() -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().padding(24.dp)
        ) {
            Text(
                "You're not organizing any live public tournaments",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        else -> LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(tournaments, key = { it.id }) { tournament ->
                DiscoverCard(
                    tournament = tournament,
                    onClick = { onTournamentSelected(tournament.id) }
                )
            }
        }
    }
}

@Composable
private fun PublicJoinCard(
    tournament: Tournament,
    summary: ParticipationSummary?,
    isJoining: Boolean,
    onJoinClick: () -> Unit
) {
    val hasJoined = summary?.hasJoined == true
    val count = summary?.participantCount ?: 0

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        tournament.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        tournament.format.shortLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(tournament.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (tournament.inviteCode != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            tournament.inviteCode,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    // maxTeams is 0 for a public "No Limit" tournament — the explicit
                    // no-cap sentinel set in CreateTournamentViewModel — so fall back to
                    // a plain count rather than showing "n/0".
                    if (tournament.maxTeams > 0) "$count/${tournament.maxTeams} joined" else "$count joined",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onJoinClick,
                    enabled = !hasJoined && !isJoining &&
                        tournament.status == TournamentStatus.REGISTRATION
                ) {
                    Text(if (hasJoined) "Joined" else "Join")
                }
            }
        }
    }
}

@Composable
private fun DiscoverCard(tournament: Tournament, onClick: () -> Unit = {}) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tournament.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tournament.format.shortLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "  •  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (tournament.maxTeams > 0) "${tournament.maxTeams} max" else "No limit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            StatusBadge(tournament.status)
        }
    }
}

@Composable
private fun StatusBadge(status: TournamentStatus) {
    val (label, color) = when (status) {
        TournamentStatus.REGISTRATION -> "OPEN" to MaterialTheme.colorScheme.primary
        TournamentStatus.IN_PROGRESS  -> "LIVE" to MaterialTheme.colorScheme.error
        TournamentStatus.COMPLETED    -> "DONE" to MaterialTheme.colorScheme.onSurfaceVariant
        TournamentStatus.DRAFT        -> "DRAFT" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ComingSoon(message: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun BrowseSubTab.label() = when (this) {
    BrowseSubTab.DISCOVER        -> "Discover"
    BrowseSubTab.JOINED_LIVE     -> "Joined"
    BrowseSubTab.ORGANIZING_LIVE -> "Organizing"
}

private fun BracketFormat.shortLabel() = when (this) {
    BracketFormat.SINGLE_ELIMINATION   -> "Single Elim"
    BracketFormat.DOUBLE_ELIMINATION   -> "Double Elim"
    BracketFormat.ROUND_ROBIN          -> "Round Robin"
    BracketFormat.SWISS                -> "Swiss"
    BracketFormat.LEAGUE               -> "League"
    BracketFormat.THREE_GAME_GUARANTEE -> "3-Game"
}
