package com.kmp.setplay.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
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
import com.kmp.setplay.presentation.auth.AuthAction
import com.kmp.setplay.presentation.auth.AuthUiState
import com.kmp.setplay.presentation.browse.BrowseScreen
import com.kmp.setplay.presentation.history.HistoryScreen
import com.kmp.setplay.presentation.home.HomeScreen
import com.kmp.setplay.presentation.profile.ProfileScreen
import com.kmp.setplay.presentation.settings.SettingsScreen
import com.kmp.setplay.presentation.tournament.create.CreateTournamentScreen
import com.kmp.setplay.presentation.tournament.create.CreateTournamentViewModel
import com.kmp.setplay.presentation.tournament.create.createTournamentTopBarTitle
import com.kmp.setplay.presentation.tournament.create.onCreateTournamentBack
import com.kmp.setplay.presentation.tournament.detail.TournamentDetailScreen
import com.kmp.setplay.presentation.tournament.detail.TournamentDetailTopBarActions
import com.kmp.setplay.presentation.tournament.detail.TournamentDetailViewModel
import com.kmp.setplay.presentation.tournament.join.JoinTournamentScreen
import com.kmp.setplay.presentation.tournament.join.JoinTournamentViewModel
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private data class TopBarSpec(
    val title: String,
    val showBackButton: Boolean = true,
    val onBackClick: () -> Unit,
    val actions: @Composable RowScope.() -> Unit = {}
)

@OptIn(ExperimentalSerializationApi::class)
private val mainAppSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Route.MainApp.Tabs::class, Route.MainApp.Tabs.serializer())
            subclass(Route.MainApp.Tabs.Home::class, Route.MainApp.Tabs.Home.serializer())
            subclass(Route.MainApp.Tabs.Browse::class, Route.MainApp.Tabs.Browse.serializer())
            subclass(Route.MainApp.Tabs.History::class, Route.MainApp.Tabs.History.serializer())
            subclass(Route.MainApp.Tabs.Profile::class, Route.MainApp.Tabs.Profile.serializer())
            subclass(Route.MainApp.CreateTournament::class, Route.MainApp.CreateTournament.serializer())
            subclass(Route.MainApp.TournamentDetail::class, Route.MainApp.TournamentDetail.serializer())
            subclass(Route.MainApp.JoinTournament::class, Route.MainApp.JoinTournament.serializer())
            subclass(Route.MainApp.Settings::class, Route.MainApp.Settings.serializer())
        }
    }
}

private enum class BottomNavBarTabs(
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

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalComposeUiApi::class)
@Composable
fun MainAppNavigation(
    authState: AuthUiState,
    onAuthAction: (AuthAction) -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(BottomNavBarTabs.HOME) }
    var tabDirection by remember { mutableStateOf(1) }
    var topBarSpec by remember { mutableStateOf<TopBarSpec?>(null) }

    val homeBackStack = rememberNavBackStack(configuration = mainAppSavedStateConfiguration, Route.MainApp.Tabs.Home)
    val browseBackStack = rememberNavBackStack(configuration = mainAppSavedStateConfiguration, Route.MainApp.Tabs.Browse)
    val historyBackStack = rememberNavBackStack(configuration = mainAppSavedStateConfiguration, Route.MainApp.Tabs.History)
    val profileBackStack = rememberNavBackStack(configuration = mainAppSavedStateConfiguration, Route.MainApp.Tabs.Profile)

    val activeBackStack = when (selectedTab) {
        BottomNavBarTabs.HOME -> homeBackStack
        BottomNavBarTabs.BROWSE -> browseBackStack
        BottomNavBarTabs.HISTORY -> historyBackStack
        BottomNavBarTabs.PROFILE -> profileBackStack
    }

    fun selectTab(tab: BottomNavBarTabs) {
        if (tab != selectedTab) {
            tabDirection = if (tab.ordinal > selectedTab.ordinal) 1 else -1
            selectedTab = tab
        }
    }

    fun push(route: Route) {
        tabDirection = 1
        val topRoute = activeBackStack.lastOrNull()
        if (topRoute?.let { it::class } == route::class) {
            activeBackStack[activeBackStack.lastIndex] = route
        } else {
            activeBackStack.add(route)
        }
    }

    fun pop() {
        tabDirection = -1
        activeBackStack.removeLastOrNull()
    }

    fun handleBack() {
        when {
            activeBackStack.size > 1 -> pop()
            selectedTab != BottomNavBarTabs.HOME -> selectTab(BottomNavBarTabs.HOME)
        }
    }

    val canHandleBack = activeBackStack.size > 1 || selectedTab != BottomNavBarTabs.HOME
    @Suppress("DEPRECATION")
    BackHandler(enabled = canHandleBack) { handleBack() }

    Scaffold(
        topBar = {
            if (activeBackStack.size <= 1) {
                TopAppBar(
                    title = {
                        Text(
                            selectedTab.topBarTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            } else {
                val spec = topBarSpec
                TopAppBar(
                    title = {
                        Text(spec?.title.orEmpty(), fontWeight = FontWeight.SemiBold)
                    },
                    navigationIcon = {
                        if (spec?.showBackButton != false) {
                            IconButton(onClick = { spec?.onBackClick?.invoke() ?: pop() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = { spec?.actions?.invoke(this) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar {
                BottomNavBarTabs.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectTab(tab) },
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
        val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

        NavDisplay(
            modifier = Modifier.fillMaxSize().consumeWindowInsets(innerPadding),
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            backStack = activeBackStack,
            sceneStrategies = remember(listDetailStrategy) { listOf(listDetailStrategy) },
            onBack = { pop() },
            transitionSpec = {
                if (tabDirection >= 0) {
                    slideInHorizontally(initialOffsetX = { it })
                        .togetherWith(slideOutHorizontally(targetOffsetX = { -it }))
                } else {
                    slideInHorizontally(initialOffsetX = { -it })
                        .togetherWith(slideOutHorizontally(targetOffsetX = { it }))
                }
            },
            popTransitionSpec = {
                if (tabDirection >= 0) {
                    slideInHorizontally(initialOffsetX = { it })
                        .togetherWith(slideOutHorizontally(targetOffsetX = { -it }))
                } else {
                    slideInHorizontally(initialOffsetX = { -it })
                        .togetherWith(slideOutHorizontally(targetOffsetX = { it }))
                }
            },
            predictivePopTransitionSpec = {
                slideInHorizontally(initialOffsetX = { -it })
                    .togetherWith(slideOutHorizontally(targetOffsetX = { it }))
            },
            entryProvider = entryProvider {

                // Home Tab
                entry<Route.MainApp.Tabs.Home>(
                    metadata = ListDetailSceneStrategy.listPane()
                ) {
                    HomeScreen(
                        onFormatSelected = { format ->
                            push(Route.MainApp.CreateTournament(format))
                        },
                        isAnonymous = authState.isAnonymous,
                        onLinkGoogle = { onAuthAction(AuthAction.LinkGoogle) },
                        contentPadding = innerPadding
                    )
                }

                // Browse tab
                entry<Route.MainApp.Tabs.Browse>(
                    metadata = ListDetailSceneStrategy.listPane(
                        detailPlaceholder = {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    "Select a tournament to see its details",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                ) {
                    BrowseScreen(
                        contentPadding = innerPadding,
                        onTournamentSelected = { tournamentId ->
                            push(Route.MainApp.TournamentDetail(tournamentId))
                        },
                        onJoinTournament = { push(Route.MainApp.JoinTournament()) }
                    )
                }

                // History Tab
                entry<Route.MainApp.Tabs.History>(
                    metadata = ListDetailSceneStrategy.listPane()
                ) {
                    HistoryScreen(
                        contentPadding = innerPadding,
                        onTournamentSelected = { tournamentId ->
                            push(Route.MainApp.TournamentDetail(tournamentId))
                        }
                    )
                }

                // Profile Tab
                entry<Route.MainApp.Tabs.Profile> {
                    ProfileScreen(
                        onAuthAction = onAuthAction,
                        onOpenSettings = { push(Route.MainApp.Settings) },
                        contentPadding = innerPadding
                    )
                }

                entry<Route.MainApp.Settings> {
                    SideEffect {
                        topBarSpec = TopBarSpec(
                            title = "Settings",
                            onBackClick = { pop() }
                        )
                    }

                    SettingsScreen(contentPadding = innerPadding)
                }

                // Additional Routes accessible through Tab Screens
                entry<Route.MainApp.TournamentDetail>(
                    metadata = ListDetailSceneStrategy.detailPane() +
                            ListDetailSceneStrategy.paneAnimation(
                                slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                                slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                            )
                ) { route ->
                    val vm: TournamentDetailViewModel = koinViewModel(
                        parameters = { parametersOf(route.tournamentId) }
                    )
                    val state by vm.uiState.collectAsStateWithLifecycle()

                    SideEffect {
                        topBarSpec = TopBarSpec(
                            title = state.tournament?.name ?: "Tournament",
                            onBackClick = { pop() },
                            actions = { TournamentDetailTopBarActions(state, vm::onAction) }
                        )
                    }

                    TournamentDetailScreen(
                        state = state,
                        onAction = vm::onAction,
                        onBack = { pop() },
                        contentPadding = innerPadding
                    )
                }

                entry<Route.MainApp.CreateTournament>(
                    metadata = ListDetailSceneStrategy.detailPane() +
                            ListDetailSceneStrategy.paneAnimation(
                                slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                                slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                            )
                ) { route ->
                    val vm: CreateTournamentViewModel = koinViewModel()

                    LaunchedEffect(route.format) {
                        vm.initFormat(route.format)
                    }

                    val state by vm.uiState.collectAsStateWithLifecycle()

                    SideEffect {
                        topBarSpec = TopBarSpec(
                            title = createTournamentTopBarTitle(state),
                            onBackClick = {
                                onCreateTournamentBack(state, vm::onAction) { pop() }
                            }
                        )
                    }

                    CreateTournamentScreen(
                        state = state,
                        onAction = vm::onAction,
                        onCreated = { tournament ->
                            tabDirection = 1
                            activeBackStack.removeLastOrNull()
                            activeBackStack.add(Route.MainApp.TournamentDetail(tournament.id))
                        },
                        onBack = { pop() },
                        contentPadding = innerPadding
                    )
                }

                entry<Route.MainApp.JoinTournament>(
                    metadata = ListDetailSceneStrategy.detailPane() +
                            ListDetailSceneStrategy.paneAnimation(
                                slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                                slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                            )
                ) { route ->
                    val vm: JoinTournamentViewModel = koinViewModel(
                        parameters = { parametersOf(route.inviteCode) }
                    )
                    val state by vm.uiState.collectAsStateWithLifecycle()

                    SideEffect {
                        topBarSpec = TopBarSpec(
                            title = "Join Tournament",
                            onBackClick = { pop() }
                        )
                    }

                    JoinTournamentScreen(
                        state = state,
                        onAction = vm::onAction,
                        onTournamentFound = { tournament ->
                            tabDirection = 1
                            activeBackStack.removeLastOrNull()
                            activeBackStack.add(Route.MainApp.TournamentDetail(tournament.id))
                        },
                        onBack = { pop() },
                        contentPadding = innerPadding
                    )
                }
            }
        )
    }
}