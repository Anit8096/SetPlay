package com.kmp.setplay.navigation

import androidx.navigation3.runtime.NavKey
import com.kmp.setplay.domain.model.BracketFormat
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {

    // ── Auth ──────────────────────────────────────────────────────────────────
    @Serializable
    data object SignIn : Route

    // ── App shell ─────────────────────────────────────────────────────────────
    @Serializable
    data object AppShell : Route

    // ── Tab destinations (internal to AppShell) ───────────────────────────────
    @Serializable
    data object Home : Route

    @Serializable
    data object Browse : Route

    @Serializable
    data object History : Route

    @Serializable
    data object Profile : Route

    // ── Full-screen destinations ───────────────────────────────────────────────
    @Serializable
    data class CreateTournament(val format: BracketFormat) : Route

    @Serializable
    data class TournamentDetail(val tournamentId: String) : Route

    @Serializable
    data class JoinTournament(val inviteCode: String? = null) : Route

    @Serializable
    data object Settings : Route
}