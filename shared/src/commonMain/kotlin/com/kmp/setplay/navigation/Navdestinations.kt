package com.kmp.setplay.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import com.kmp.setplay.domain.model.BracketFormat
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic


@OptIn(ExperimentalSerializationApi::class)
val routeSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Route.Auth::class, Route.Auth.serializer())
            subclass(Route.MainApp::class, Route.MainApp.serializer())
        }
    }
}

@Serializable
sealed interface Route : NavKey {

    // ── Auth graph ────────────────────────────────────────────────────────────
    @Serializable
    data object Auth : Route {
        @Serializable data object SignIn : Route
    }

    // ── Main app graph ───────────────────────────────────────────────────────
    @Serializable
    data object MainApp : Route {
        @Serializable
        data object Tabs : Route {
            @Serializable data object Home : Route
            @Serializable data object Browse : Route
            @Serializable data object History : Route
            @Serializable data object Profile : Route
        }

        @Serializable data class CreateTournament(
            val format: BracketFormat
        ) : Route

        @Serializable data class TournamentDetail(
            val tournamentId: String
        ) : Route

        @Serializable data class JoinTournament(
            val inviteCode: String? = null
        ) : Route

        @Serializable data object Settings : Route
    }
}
