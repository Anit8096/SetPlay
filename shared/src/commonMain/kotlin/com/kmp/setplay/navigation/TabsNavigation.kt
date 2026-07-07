package com.kmp.setplay.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.kmp.setplay.domain.model.BracketFormat
import com.kmp.setplay.presentation.browse.BrowseScreen
import com.kmp.setplay.presentation.history.HistoryScreen
import com.kmp.setplay.presentation.home.HomeScreen
import com.kmp.setplay.presentation.profile.ProfileScreen
import com.kmp.setplay.presentation.tournament.detail.TournamentDetailScreen
import com.kmp.setplay.presentation.tournament.detail.TournamentDetailViewModel
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf


@OptIn(ExperimentalSerializationApi::class)
private val tabsSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Route.MainApp.Tabs.Home::class, Route.MainApp.Tabs.Home.serializer())
            subclass(Route.MainApp.Tabs.Browse::class, Route.MainApp.Tabs.Browse.serializer())
            subclass(Route.MainApp.Tabs.History::class, Route.MainApp.Tabs.History.serializer())
            subclass(Route.MainApp.Tabs.Profile::class, Route.MainApp.Tabs.Profile.serializer())
            subclass(Route.MainApp.TournamentDetail::class, Route.MainApp.TournamentDetail.serializer())
        }
    }
}

private enum class Tab(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String,
    val topBarTitle: String
) {
    HOME(Icons.Filled.Home, Icons.Outlined.Home, "Home", "SetPlay"),
    BROWSE(Icons.Filled.Search, Icons.Outlined.Search, "Browse", "Browse"),
    HISTORY(Icons.Filled.History, Icons.Outlined.History, "History", "History"),
    PROFILE(Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle, "Profile", "Profile"),
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun TabsNavigation(
    onFormatSelected: (BracketFormat) -> Unit,
    onJoinTournament: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(Tab.HOME) }

    val homeBackStack = rememberNavBackStack(configuration = tabsSavedStateConfiguration, Route.MainApp.Tabs.Home)
    val browseBackStack = rememberNavBackStack(configuration = tabsSavedStateConfiguration, Route.MainApp.Tabs.Browse)
    val historyBackStack = rememberNavBackStack(configuration = tabsSavedStateConfiguration, Route.MainApp.Tabs.History)
    val profileBackStack = rememberNavBackStack(configuration = tabsSavedStateConfiguration, Route.MainApp.Tabs.Profile)

    val activeBackStack = when (selectedTab) {
        Tab.HOME -> homeBackStack
        Tab.BROWSE -> browseBackStack
        Tab.HISTORY -> historyBackStack
        Tab.PROFILE -> profileBackStack
    }

    val isShowingTournamentDetail = activeBackStack.lastOrNull() is Route.MainApp.TournamentDetail

    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

    Scaffold(
        topBar = {
            if (!isShowingTournamentDetail) {
                TopAppBar(
                    title = {
                        Text(
                            selectedTab.topBarTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == tab) tab.selectedIcon
                                else tab.unselectedIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavDisplay(
            backStack = activeBackStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            sceneStrategies = listOf(listDetailStrategy),
            onBack = {
                if (activeBackStack.size > 1) {
                    activeBackStack.removeLastOrNull()
                } else if (selectedTab != Tab.HOME) {
                    selectedTab = Tab.HOME
                }
            },
            entryProvider = entryProvider {
                entry<Route.MainApp.Tabs.Home> {
                    HomeScreen(
                        onFormatSelected = onFormatSelected,
                        contentPadding = innerPadding
                    )
                }
                entry<Route.MainApp.Tabs.Browse>(
                    metadata = ListDetailSceneStrategy.listPane()
                ) {
                    BrowseScreen(
                        contentPadding = innerPadding,
                        onTournamentSelected = { tournamentId ->
                            browseBackStack.add(Route.MainApp.TournamentDetail(tournamentId))
                        },
                        onJoinTournament = onJoinTournament
                    )
                }
                entry<Route.MainApp.Tabs.History>(
                    metadata = ListDetailSceneStrategy.listPane()
                ) {
                    HistoryScreen(
                        contentPadding = innerPadding,
                        onTournamentSelected = { tournamentId ->
                            historyBackStack.add(Route.MainApp.TournamentDetail(tournamentId))
                        }
                    )
                }
                entry<Route.MainApp.Tabs.Profile>() {
                    ProfileScreen(contentPadding = innerPadding)
                }
                entry<Route.MainApp.TournamentDetail>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) { route ->
                    val vm: TournamentDetailViewModel = koinViewModel(
                        parameters = { parametersOf(route.tournamentId) }
                    )
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    TournamentDetailScreen(
                        state = state,
                        onAction = vm::onAction,
                        onBack = { activeBackStack.removeLastOrNull() }
                    )
                }
            }
        )
    }
}
