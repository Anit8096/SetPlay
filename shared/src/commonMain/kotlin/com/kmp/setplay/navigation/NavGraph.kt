package com.kmp.setplay.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.kmp.setplay.presentation.auth.AuthAction
import com.kmp.setplay.presentation.auth.AuthViewModel
import com.kmp.setplay.presentation.auth.SignInScreen
import com.kmp.setplay.presentation.home.HomeScreen
import com.kmp.setplay.presentation.home.HomeViewModel
import com.kmp.setplay.presentation.tournament.create.CreateTournamentScreen
import com.kmp.setplay.presentation.tournament.create.CreateTournamentViewModel
import com.kmp.setplay.presentation.tournament.detail.TournamentDetailScreen
import com.kmp.setplay.presentation.tournament.detail.TournamentDetailViewModel
import com.kmp.setplay.presentation.tournament.join.JoinTournamentScreen
import com.kmp.setplay.presentation.tournament.join.JoinTournamentViewModel
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun NavGraph() {
    val authViewModel: AuthViewModel = koinViewModel()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    val backStack = rememberNavBackStack(
        configuration = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(Route.SignIn::class, Route.SignIn.serializer())
                    subclass(Route.Home::class, Route.Home.serializer())
                    subclass(Route.CreateTournament::class, Route.CreateTournament.serializer())
                    subclass(Route.TournamentDetail::class, Route.TournamentDetail.serializer())
                    subclass(Route.JoinTournament::class, Route.JoinTournament.serializer())
                    subclass(Route.Settings::class, Route.Settings.serializer())
                }
            }
        },
        Route.SignIn
    )

    // Auth gate
    LaunchedEffect(authState.isAuthenticated) {
        if (authState.isAuthenticated && backStack.lastOrNull() is Route.SignIn) {
            backStack.removeLastOrNull()
            backStack.add(Route.Home)
        }
        if (!authState.isAuthenticated && backStack.lastOrNull() !is Route.SignIn) {
            backStack.clear()
            backStack.add(Route.SignIn)
        }
    }

    Scaffold { innerPadding ->
        NavDisplay(
            modifier = Modifier.padding(innerPadding),
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            transitionSpec = {
                slideInHorizontally(initialOffsetX = { it })
                    .togetherWith(slideOutHorizontally(targetOffsetX = { -it }))
            },
            popTransitionSpec = {
                slideInHorizontally(initialOffsetX = { -it })
                    .togetherWith(slideOutHorizontally(targetOffsetX = { it }))
            },
            predictivePopTransitionSpec = {
                slideInHorizontally(initialOffsetX = { -it })
                    .togetherWith(slideOutHorizontally(targetOffsetX = { it }))
            },
            entryProvider = entryProvider {

                entry<Route.SignIn> {
                    SignInScreen(
                        state = authState,
                        onAction = authViewModel::onAction
                    )
                }

                entry<Route.Home> {
                    val vm: HomeViewModel = koinViewModel()
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    HomeScreen(
                        state = state,
                        authState = authState,
                        onAction = vm::onAction,
                        onCreateTournament = { backStack.add(Route.CreateTournament) },
                        onTournamentClick = { backStack.add(Route.TournamentDetail(it.id)) },
                        onJoin = { backStack.add(Route.JoinTournament()) },
                        onLinkGoogle = { authViewModel.onAction(AuthAction.LinkGoogle) },
                        onSignOut = { authViewModel.onAction(AuthAction.SignOut) }
                    )
                }

                entry<Route.CreateTournament> {
                    val vm: CreateTournamentViewModel = koinViewModel()
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    CreateTournamentScreen(
                        state = state,
                        onAction = vm::onAction,
                        onCreated = { tournament ->
                            backStack.removeLastOrNull()
                            backStack.add(Route.TournamentDetail(tournament.id))
                        },
                        onBack = { backStack.removeLastOrNull() }
                    )
                }

                entry<Route.TournamentDetail> { route ->
                    val vm: TournamentDetailViewModel = koinViewModel(
                        parameters = { parametersOf(route.tournamentId) }
                    )
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    TournamentDetailScreen(
                        state = state,
                        onAction = vm::onAction,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }

                entry<Route.JoinTournament> { route ->
                    val vm: JoinTournamentViewModel = koinViewModel(
                        parameters = { parametersOf(route.inviteCode) }
                    )
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    JoinTournamentScreen(
                        state = state,
                        onAction = vm::onAction,
                        onTournamentFound = { tournament ->
                            backStack.removeLastOrNull()
                            backStack.add(Route.TournamentDetail(tournament.id))
                        },
                        onBack = { backStack.removeLastOrNull() }
                    )
                }

                entry<Route.Settings> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text("Settings — coming soon")
                    }
                }
            }
        )
    }
}