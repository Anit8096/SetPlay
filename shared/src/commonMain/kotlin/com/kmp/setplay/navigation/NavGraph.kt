package com.kmp.setplay.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.kmp.setplay.presentation.auth.AuthViewModel
import com.kmp.setplay.presentation.auth.SignInScreen
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
                    subclass(Route.AppShell::class, Route.AppShell.serializer())
                    subclass(Route.Home::class, Route.Home.serializer())
                    subclass(Route.Browse::class, Route.Browse.serializer())
                    subclass(Route.History::class, Route.History.serializer())
                    subclass(Route.Profile::class, Route.Profile.serializer())
                    subclass(Route.CreateTournament::class, Route.CreateTournament.serializer())
                    subclass(Route.TournamentDetail::class, Route.TournamentDetail.serializer())
                    subclass(Route.JoinTournament::class, Route.JoinTournament.serializer())
                    subclass(Route.Settings::class, Route.Settings.serializer())
                }
            }
        },
        Route.SignIn
    )

    LaunchedEffect(authState.isAuthenticated) {
        if (authState.isAuthenticated && backStack.lastOrNull() is Route.SignIn) {
            backStack.removeLastOrNull()
            backStack.add(Route.AppShell)
        }
        if (!authState.isAuthenticated && backStack.lastOrNull() !is Route.SignIn) {
            backStack.clear()
            backStack.add(Route.SignIn)
        }
    }

    NavDisplay(
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

            entry<Route.AppShell> {
                AppShell(
                    onFormatSelected = { format ->
                        backStack.add(Route.CreateTournament(format))
                    },
                    onTournamentSelected = { tournamentId ->
                        backStack.add(Route.TournamentDetail(tournamentId))
                    },
                    onJoinTournament = {
                        backStack.add(Route.JoinTournament())
                    }
                )
            }

            entry<Route.CreateTournament> { route ->
                val vm: CreateTournamentViewModel = koinViewModel()
                // Initialize format once — VM is scoped to this backstack entry
                LaunchedEffect(route.format) { vm.initFormat(route.format) }
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
        }
    )
}