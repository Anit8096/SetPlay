package com.kmp.setplay.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.kmp.setplay.presentation.auth.AuthViewModel
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

private const val MIN_SPLASH_DURATION_MS = 750L

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NavGraph() {
    val authViewModel: AuthViewModel = koinInject()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    var minSplashElapsed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(MIN_SPLASH_DURATION_MS)
        minSplashElapsed = true
    }

    if (authState.isInitializing || !minSplashElapsed) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularWavyProgressIndicator(modifier = Modifier.size(48.dp))
            }
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