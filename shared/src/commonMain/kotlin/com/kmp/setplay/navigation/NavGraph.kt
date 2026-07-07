package com.kmp.setplay.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.kmp.setplay.presentation.auth.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun NavGraph() {
    val authViewModel: AuthViewModel = koinViewModel()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    val backStack = rememberNavBackStack(
        configuration = routeSavedStateConfiguration,
        Route.Auth
    )

    LaunchedEffect(authState.isAuthenticated) {
        if (authState.isAuthenticated && backStack.lastOrNull() is Route.Auth) {
            backStack.removeLastOrNull()
            backStack.add(Route.MainApp)
        }
        if (!authState.isAuthenticated && backStack.lastOrNull() !is Route.Auth) {
            backStack.clear()
            backStack.add(Route.Auth)
        }
    }

    NavDisplay(
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {

            entry<Route.Auth> {
                AuthNavigation(
                    state = authState,
                    onAction = authViewModel::onAction
                )
            }

            entry<Route.MainApp> {
                MainAppNavigation()
            }
        }
    )
}
