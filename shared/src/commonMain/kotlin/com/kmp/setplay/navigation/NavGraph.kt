package com.kmp.setplay.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.kmp.setplay.presentation.auth.LinkAccountBanner
import com.kmp.setplay.presentation.auth.SignInScreen
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.viewmodel.koinViewModel

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

    // Auth gate — push Home once a session exists, pop back to SignIn on sign-out.
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
            entryProvider = entryProvider {
                entry<Route.SignIn> {
                    SignInScreen(
                        state = authState,
                        onAction = authViewModel::onAction
                    )
                }

                entry<Route.Home> {
                    HomeScreenPlaceholder(
                        isAnonymous = authState.isAnonymous,
                        onCreateTournament = { backStack.add(Route.CreateTournament) },
                        onJoin = { backStack.add(Route.JoinTournament()) },
                        onSettings = { backStack.add(Route.Settings) },
                        onLinkGoogle = { authViewModel.onAction(AuthAction.LinkGoogle) },
                        onSignOut = { authViewModel.onAction(AuthAction.SignOut) }
                    )
                }

                entry<Route.CreateTournament> {
                    // Phase 1 — wired up when CreateTournamentScreen is built
                    PlaceholderScreen("Create Tournament") { backStack.removeLastOrNull() }
                }

                entry<Route.TournamentDetail> { route ->
                    // route.tournamentId available here
                    PlaceholderScreen("Tournament: ${route.tournamentId}") {
                        backStack.removeLastOrNull()
                    }
                }

                entry<Route.JoinTournament> { route ->
                    // route.inviteCode pre-filled if launched from deep link
                    PlaceholderScreen("Join Tournament") { backStack.removeLastOrNull() }
                }

                entry<Route.Settings> {
                    PlaceholderScreen("Settings") { backStack.removeLastOrNull() }
                }
            },
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
            }
        )
    }
}

// Placeholder screens (removed once real screens are built in Phase 1) ──────
@Composable
private fun HomeScreenPlaceholder(
    isAnonymous: Boolean,
    onCreateTournament: () -> Unit,
    onJoin: () -> Unit,
    onSettings: () -> Unit,
    onLinkGoogle: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (isAnonymous) {
            LinkAccountBanner(onLinkGoogle = onLinkGoogle)
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.weight(1f).fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("SetPlay", style = MaterialTheme.typography.displaySmall)
                Button(onClick = onCreateTournament) { Text("Create tournament") }
                OutlinedButton(onClick = onJoin) { Text("Join tournament") }
                OutlinedButton(onClick = onSettings) { Text("Settings") }
                OutlinedButton(onClick = onSignOut) { Text("Sign out") }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, onBack: () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            OutlinedButton(onClick = onBack) { Text("← Back") }
        }
    }
}