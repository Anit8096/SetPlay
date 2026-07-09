package com.kmp.setplay.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.kmp.setplay.presentation.auth.AuthViewModel
import org.koin.compose.koinInject


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NavGraph() {
    val authViewModel: AuthViewModel = koinInject()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    if (authState.isInitializing) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background)
        {
            CircularWavyProgressIndicator()
        }
        return
    }

    val backStack = rememberNavBackStack(
        configuration = routeSavedStateConfiguration,
        if (authState.isAuthenticated) Route.MainApp else Route.Auth
    )

    LaunchedEffect(authState.isAuthenticated) {
        if (authState.isAuthenticated && backStack.lastOrNull() !is Route.MainApp) {
            backStack.clear()
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
                MainAppNavigation(
                    authState = authState,
                    onAuthAction = authViewModel::onAction
                )
            }
        }
    )
}
