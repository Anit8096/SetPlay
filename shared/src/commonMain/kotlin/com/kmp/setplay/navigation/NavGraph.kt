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
import kotlin.time.Duration.Companion.milliseconds

private const val MIN_SPLASH_DURATION_MS = 2000

@Composable
fun NavGraph() {
    val authViewModel: AuthViewModel = koinInject()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    var minSplashElapsed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(MIN_SPLASH_DURATION_MS.milliseconds)
        minSplashElapsed = true
    }
    val splashDone = !authState.isInitializing && minSplashElapsed
    val backStack = rememberNavBackStack(
        configuration = routeSavedStateConfiguration,
        Route.PostSplash
    )

    LaunchedEffect(splashDone, authState.isAuthenticated) {
        if (!splashDone) return@LaunchedEffect
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

            entry<Route.PostSplash> {
                AuthSplashScreen()
            }

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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AuthSplashScreen() {
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
}