package com.kmp.setplay.data.remote.dto

import com.kmp.setplay.domain.model.Announcement
import com.kmp.setplay.domain.model.BracketFormat
import com.kmp.setplay.domain.model.DevicePlatform
import com.kmp.setplay.domain.model.DeviceToken
import com.kmp.setplay.domain.model.Match
import com.kmp.setplay.domain.model.MatchStatus
import com.kmp.setplay.domain.model.OrganizerRole
import com.kmp.setplay.domain.model.Player
import com.kmp.setplay.domain.model.Round
import com.kmp.setplay.domain.model.Standing
import com.kmp.setplay.domain.model.Team
import com.kmp.setplay.domain.model.Tournament
import com.kmp.setplay.domain.model.TournamentOrganizer
import com.kmp.setplay.domain.model.TournamentStatus
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase DTOs — mirror Postgres column names exactly via @SerialName.
 * These are only used in the data/remote layer and converted to domain
 * models before being passed up to the repository or UI.
 */

// Used in: observeMyTournaments, observeTournament, createTournament,
// getTournamentByInviteCode, Realtime insert/update handler
@Serializable
data class TournamentDto(
    val id: String,
    val name: String,
    val format: BracketFormat,
    val status: TournamentStatus,
    @SerialName("created_by")  val createdBy: String,
    @SerialName("invite_code") val inviteCode: String?,
    @SerialName("max_teams")   val maxTeams: Int,
    @SerialName("is_public")   val isPublic: Boolean,
    @SerialName("created_at")  val createdAt: Instant,
    @SerialName("updated_at")  val updatedAt: Instant
) {
    fun toDomain() = Tournament(
        id = id, name = name, format = format, status = status,
        createdBy = createdBy, inviteCode = inviteCode, maxTeams = maxTeams,
        isPublic = isPublic, createdAt = createdAt, updatedAt = updatedAt
    )
}

// Used in: observeTeams, addTeam, generateBracket
@Serializable
data class TeamDto(
    val id: String,
    @SerialName("tournament_id") val tournamentId: String,
    val name: String,
    val seed: Int?,
    @SerialName("logo_url")   val logoUrl: String?,
    @SerialName("created_at") val createdAt: Instant
) {
    fun toDomain() = Team(
        id = id, tournamentId = tournamentId, name = name,
        seed = seed, logoUrl = logoUrl, createdAt = createdAt
    )
}

// Not yet used — add back when player roster feature is built
@Serializable
data class PlayerDto(
    val id: String,
    @SerialName("team_id") val teamId: String,
    val name: String
) {
    fun toDomain() = Player(id = id, teamId = teamId, name = name)
}

// Used in: generateBracket (insert only, never decoded back from Supabase)
@Serializable
data class RoundDto(
    val id: String,
    @SerialName("tournament_id") val tournamentId: String,
    @SerialName("round_number")  val roundNumber: Int,
    val name: String?
) {
    fun toDomain() = Round(
        id = id, tournamentId = tournamentId,
        roundNumber = roundNumber, name = name
    )
}

// Used in: observeMatches, updateMatch, generateBracket,
// Realtime insert/update handler
@Serializable
data class MatchDto(
    val id: String,
    @SerialName("round_id")            val roundId: String,
    @SerialName("tournament_id")       val tournamentId: String,
    @SerialName("match_number")        val matchNumber: Int,
    @SerialName("team1_id")            val team1Id: String?,
    @SerialName("team2_id")            val team2Id: String?,
    val score1: Int?,
    val score2: Int?,
    @SerialName("winner_id")           val winnerId: String?,
    @SerialName("loser_id")            val loserId: String?,
    val status: MatchStatus,
    @SerialName("next_match_id")       val nextMatchId: String?,
    @SerialName("next_loser_match_id") val nextLoserMatchId: String?
) {
    fun toDomain() = Match(
        id = id, roundId = roundId, tournamentId = tournamentId,
        matchNumber = matchNumber, team1Id = team1Id, team2Id = team2Id,
        score1 = score1, score2 = score2, winnerId = winnerId, loserId = loserId,
        status = status, nextMatchId = nextMatchId, nextLoserMatchId = nextLoserMatchId
    )
}

// Used in: observeStandings
@Serializable
data class StandingDto(
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
) {
    fun toDomain() = Standing(
        id = id, tournamentId = tournamentId, teamId = teamId,
        wins = wins, losses = losses, draws = draws, points = points,
        buchholz = buchholz, goalsFor = goalsFor, goalsAgainst = goalsAgainst
    )
}

// Used in: observeAnnouncements
@Serializable
data class AnnouncementDto(
    val id: String,
    @SerialName("tournament_id") val tournamentId: String,
    @SerialName("created_by")    val createdBy: String,
    val message: String,
    @SerialName("created_at")    val createdAt: Instant
) {
    fun toDomain() = Announcement(
        id = id, tournamentId = tournamentId, createdBy = createdBy,
        message = message, createdAt = createdAt
    )
}

// Not yet used — add back when push notification feature is built
@Serializable
data class DeviceTokenDto(
    val id: String,
    @SerialName("user_id")    val userId: String,
    val token: String,
    val platform: DevicePlatform,
    @SerialName("created_at") val createdAt: Instant
) {
    fun toDomain() = DeviceToken(
        id = id, userId = userId, token = token,
        platform = platform, createdAt = createdAt
    )
}

// Used in: TournamentRepository.getOrganizerRole
@Serializable
data class TournamentOrganizerDto(
    val id: String,
    @SerialName("tournament_id") val tournamentId: String,
    @SerialName("user_id")       val userId: String,
    val role: OrganizerRole,
    @SerialName("created_at")    val createdAt: Instant
) {
    fun toDomain() = TournamentOrganizer(
        id = id, tournamentId = tournamentId, userId = userId,
        role = role, createdAt = createdAt
    )
}

// ── Network Requests (KMP Web Serialization) ──────────────────────────────────

// Used in: addTeam
@Serializable
data class InsertTeamRequestDto(
    @SerialName("tournament_id") val tournamentId: String,
    val name: String,
    val seed: Int?
)

// Used in: updateMatch — sets score, winner, loser, status on completion
@Serializable
data class UpdateMatchResultRequestDto(
    val score1: Int,
    val score2: Int,
    @SerialName("winner_id") val winnerId: String?,
    @SerialName("loser_id") val loserId: String?,
    val status: MatchStatus
)

// Used in: updateMatch — slots winner into team1 or team2 of the next match
@Serializable
data class AdvanceWinnerRequestDto(
    @SerialName("team1_id") val team1Id: String?,
    @SerialName("team2_id") val team2Id: String?
)

// Used in: createTournament
@Serializable
data class InsertTournamentRequestDto(
    val name: String,
    val format: BracketFormat,
    val status: TournamentStatus,
    @SerialName("created_by") val createdBy: String,
    @SerialName("max_teams") val maxTeams: Int,
    @SerialName("is_public") val isPublic: Boolean
)

// Used in: updateTournament
@Serializable
data class UpdateTournamentRequestDto(
    val name: String,
    val status: TournamentStatus,
    @SerialName("is_public") val isPublic: Boolean,
    @SerialName("max_teams") val maxTeams: Int
)