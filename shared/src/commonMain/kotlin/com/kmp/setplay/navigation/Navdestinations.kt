package com.kmp.setplay.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {

    @Serializable
    data object SignIn : Route

    @Serializable
    data object Home : Route

    @Serializable
    data object CreateTournament : Route

    @Serializable
    data class TournamentDetail(val tournamentId: String) : Route

    @Serializable
    data class JoinTournament(val inviteCode: String? = null) : Route

    @Serializable
    data object Settings : Route
}
