package com.kmp.setplay.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.kmp.setplay.presentation.auth.AuthUiState
import com.kmp.setplay.presentation.auth.AuthAction
import com.kmp.setplay.presentation.auth.SignInScreen
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@OptIn(ExperimentalSerializationApi::class)
private val authSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Route.Auth.SignIn::class, Route.Auth.SignIn.serializer())
        }
    }
}

@Composable
fun AuthNavigation(
    state: AuthUiState,
    onAction: (AuthAction) -> Unit,
) {
    val authBackStack = rememberNavBackStack(
        configuration = authSavedStateConfiguration,
        Route.Auth.SignIn
    )

    NavDisplay(
        backStack = authBackStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        onBack = { authBackStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Route.Auth.SignIn> {
                SignInScreen(
                    state = state,
                    onAction = onAction
                )
            }
        }
    )
}
