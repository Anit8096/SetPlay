package com.kmp.setplay.domain.model

import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Pure domain models — no Room or Supabase annotations.
 * These are what the UI and business logic work with.
 * Mapping from/to DTOs and Entities happens in the data layer.
 */

@Serializable
data class Tournament(
    val id: String,
    val name: String,
    val format: BracketFormat,
    val status: TournamentStatus,
    @SerialName("created_by")  val createdBy: String,
    @SerialName("invite_code") val inviteCode: String?,
    @SerialName("max_teams")   val maxTeams: Int,
    @SerialName("is_public")   val isPublic: Boolean,
    @SerialName("registration_deadline") val registrationDeadline: Instant? = null,
    @SerialName("created_at")  val createdAt: Instant,
    @SerialName("updated_at")  val updatedAt: Instant
)

@Serializable
data class Team(
    val id: String,
    @SerialName("tournament_id") val tournamentId: String,
    val name: String,
    val seed: Int?,
    @SerialName("logo_url")  val logoUrl: String?,
    @SerialName("user_id")   val userId: String? = null,
    @SerialName("created_at") val createdAt: Instant
)

@Serializable
data class Player(
    val id: String,
    @SerialName("team_id") val teamId: String,
    val name: String
)

@Serializable
data class Round(
    val id: String,
    @SerialName("tournament_id") val tournamentId: String,
    @SerialName("round_number")  val roundNumber: Int,
    val name: String?
)

@Serializable
data class Match(
    val id: String,
    @SerialName("round_id")           val roundId: String,
    @SerialName("tournament_id")      val tournamentId: String,
    @SerialName("match_number")       val matchNumber: Int,
    @SerialName("team1_id")           val team1Id: String?,
    @SerialName("team2_id")           val team2Id: String?,
    val score1: Int?,
    val score2: Int?,
    @SerialName("winner_id")          val winnerId: String?,
    @SerialName("loser_id")           val loserId: String?,
    val status: MatchStatus,
    @SerialName("next_match_id")      val nextMatchId: String?,
    @SerialName("next_loser_match_id") val nextLoserMatchId: String?,
    @SerialName("scheduled_at")       val scheduledAt: Instant? = null
)

@Serializable
data class Standing(
    val id: String,
    @SerialName("tournament_id") val tournamentId: String,
    @SerialName("team_id")       val teamId: String,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val points: Int,
    val buchholz: Double,
    @SerialName("goals_for")     val goalsFor: Int,
    @SerialName("goals_against") val goalsAgainst: Int
)

@Serializable
data class Announcement(
    val id: String,
    @SerialName("tournament_id") val tournamentId: String,
    @SerialName("created_by")    val createdBy: String,
    val message: String,
    @SerialName("created_at")    val createdAt: Instant
)

@Serializable
data class DeviceToken(
    val id: String,
    @SerialName("user_id")    val userId: String,
    val token: String,
    val platform: DevicePlatform,
    @SerialName("created_at") val createdAt: Instant
)

@Serializable
data class TournamentOrganizer(
    val id: String,
    @SerialName("tournament_id") val tournamentId: String,
    @SerialName("user_id")       val userId: String,
    val role: OrganizerRole,
    @SerialName("created_at")    val createdAt: Instant
)

/** A user who has viewed a private tournament via its share code. */
@Serializable
data class ShareViewer(
    val id: String,
    @SerialName("tournament_id")   val tournamentId: String,
    @SerialName("user_id")         val userId: String,
    @SerialName("first_viewed_at") val firstViewedAt: Instant,
    @SerialName("last_viewed_at")  val lastViewedAt: Instant,
    val revoked: Boolean
)