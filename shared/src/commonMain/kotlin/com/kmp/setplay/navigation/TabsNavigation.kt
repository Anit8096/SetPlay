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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

// Local to this graph — mirrors AuthNavigation/TodoNavigation in the Nav3 nested-graphs
// reference: only Route.MainApp.Tabs's own children are registered here, neither the
// root NavGraph nor MainAppNavigation need to know about them.
@OptIn(ExperimentalSerializationApi::class)
private val tabsSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Route.MainApp.Tabs.Home::class, Route.MainApp.Tabs.Home.serializer())
            subclass(Route.MainApp.Tabs.Browse::class, Route.MainApp.Tabs.Browse.serializer())
            subclass(Route.MainApp.Tabs.History::class, Route.MainApp.Tabs.History.serializer())
            subclass(Route.MainApp.Tabs.Profile::class, Route.MainApp.Tabs.Profile.serializer())
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsNavigation(
    onFormatSelected: (BracketFormat) -> Unit,
    onTournamentSelected: (String) -> Unit,
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

    Scaffold(
        topBar = {
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
            onBack = {
                if (activeBackStack.size > 1) {
                    activeBackStack.removeLastOrNull()
                } else if (selectedTab != Tab.HOME) {
                    selectedTab = Tab.HOME
                }
                // Already on Home's root — nothing left to pop here; MainAppNavigation's
                // own onBack takes over from this point (pops Route.MainApp.Tabs itself).
            },
            entryProvider = entryProvider {
                entry<Route.MainApp.Tabs.Home> {
                    HomeScreen(
                        onFormatSelected = onFormatSelected,
                        contentPadding = innerPadding
                    )
                }
                entry<Route.MainApp.Tabs.Browse> {
                    BrowseScreen(
                        contentPadding = innerPadding,
                        onTournamentSelected = onTournamentSelected,
                        onJoinTournament = onJoinTournament
                    )
                }
                entry<Route.MainApp.Tabs.History> {
                    HistoryScreen(
                        contentPadding = innerPadding,
                        onTournamentSelected = onTournamentSelected
                    )
                }
                entry<Route.MainApp.Tabs.Profile> {
                    ProfileScreen(contentPadding = innerPadding)
                }
            }
        )
    }
}
