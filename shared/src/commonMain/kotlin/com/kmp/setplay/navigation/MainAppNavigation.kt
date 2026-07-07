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

// Local to this graph — only Route.MainApp's own children are registered here. The root
// NavGraph doesn't know CreateTournament/TournamentDetail/JoinTournament/Tabs/Settings
// exist; it only knows about Route.MainApp as a whole.
@OptIn(ExperimentalSerializationApi::class)
private val mainAppSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Route.MainApp.Tabs::class, Route.MainApp.Tabs.serializer())
            subclass(Route.MainApp.CreateTournament::class, Route.MainApp.CreateTournament.serializer())
            subclass(Route.MainApp.TournamentDetail::class, Route.MainApp.TournamentDetail.serializer())
            subclass(Route.MainApp.JoinTournament::class, Route.MainApp.JoinTournament.serializer())
            subclass(Route.MainApp.Settings::class, Route.MainApp.Settings.serializer())
        }
    }
}

/**
 * Self-contained "everything after sign-in" graph. Owns the push/pop flows that used to
 * live directly in the root NavGraph (CreateTournament, TournamentDetail, JoinTournament)
 * plus the Tabs entry point. Root NavGraph only needs to know this composable exists
 * behind Route.MainApp — it has no knowledge of any of these individually.
 */
@Composable
fun MainAppNavigation() {
    val mainAppBackStack = rememberNavBackStack(configuration = mainAppSavedStateConfiguration, Route.MainApp.Tabs)

    NavDisplay(
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        backStack = mainAppBackStack,
        onBack = { mainAppBackStack.removeLastOrNull() },
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

            entry<Route.MainApp.Tabs> {
                TabsNavigation(
                    onFormatSelected = { format ->
                        mainAppBackStack.add(Route.MainApp.CreateTournament(format))
                    },
                    onJoinTournament = {
                        mainAppBackStack.add(Route.MainApp.JoinTournament())
                    }
                )
            }

            entry<Route.MainApp.CreateTournament> { route ->
                val vm: CreateTournamentViewModel = koinViewModel()
                // Initialize format once — VM is scoped to this backstack entry
                LaunchedEffect(route.format) { vm.initFormat(route.format) }
                val state by vm.uiState.collectAsStateWithLifecycle()
                CreateTournamentScreen(
                    state = state,
                    onAction = vm::onAction,
                    onCreated = { tournament ->
                        mainAppBackStack.removeLastOrNull()
                        mainAppBackStack.add(Route.MainApp.TournamentDetail(tournament.id))
                    },
                    onBack = { mainAppBackStack.removeLastOrNull() }
                )
            }

            entry<Route.MainApp.TournamentDetail> { route ->
                val vm: TournamentDetailViewModel = koinViewModel(
                    parameters = { parametersOf(route.tournamentId) }
                )
                val state by vm.uiState.collectAsStateWithLifecycle()
                TournamentDetailScreen(
                    state = state,
                    onAction = vm::onAction,
                    onBack = { mainAppBackStack.removeLastOrNull() }
                )
            }

            entry<Route.MainApp.JoinTournament> { route ->
                val vm: JoinTournamentViewModel = koinViewModel(
                    parameters = { parametersOf(route.inviteCode) }
                )
                val state by vm.uiState.collectAsStateWithLifecycle()
                JoinTournamentScreen(
                    state = state,
                    onAction = vm::onAction,
                    onTournamentFound = { tournament ->
                        mainAppBackStack.removeLastOrNull()
                        mainAppBackStack.add(Route.MainApp.TournamentDetail(tournament.id))
                    },
                    onBack = { mainAppBackStack.removeLastOrNull() }
                )
            }

            // Route.MainApp.Settings — still a known dead route, no entry block yet
            // (unchanged from before this restructure; needs its own screen + ViewModel).
        }
    )
}
