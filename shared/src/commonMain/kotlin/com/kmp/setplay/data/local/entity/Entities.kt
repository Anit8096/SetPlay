package com.kmp.setplay.data.local.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.kmp.setplay.domain.model.BracketFormat
import com.kmp.setplay.domain.model.DevicePlatform
import com.kmp.setplay.domain.model.MatchStatus
import com.kmp.setplay.domain.model.TournamentStatus

/**
 * Room 3.0 KMP entities.
 *
 * Rules:
 * - All definitions live in commonMain (architectural rule 5).
 * - Enums are stored as strings via Room's built-in enum support.
 * - Nullable FK columns are represented as nullable String?.
 * - No @ForeignKey constraints in Room entities — referential integrity
 *   is enforced by Supabase (Postgres). Room is a cache, not the source of truth.
 */

@Entity(tableName = "tournaments")
data class TournamentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val format: BracketFormat,
    val status: TournamentStatus,
    @ColumnInfo(name = "created_by")  val createdBy: String,
    @ColumnInfo(name = "invite_code") val inviteCode: String?,
    @ColumnInfo(name = "max_teams")   val maxTeams: Int,
    @ColumnInfo(name = "is_public")   val isPublic: Boolean,
    @ColumnInfo(name = "created_at")  val createdAt: String, // ISO-8601 string
    @ColumnInfo(name = "updated_at")  val updatedAt: String
)

@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "tournament_id") val tournamentId: String,
    val name: String,
    val seed: Int?,
    @ColumnInfo(name = "logo_url")   val logoUrl: String?,
    @ColumnInfo(name = "created_at") val createdAt: String
)

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "team_id") val teamId: String,
    val name: String
)

@Entity(tableName = "rounds")
data class RoundEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "tournament_id") val tournamentId: String,
    @ColumnInfo(name = "round_number")  val roundNumber: Int,
    val name: String?
)

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "round_id")            val roundId: String,
    @ColumnInfo(name = "tournament_id")       val tournamentId: String,
    @ColumnInfo(name = "match_number")        val matchNumber: Int,
    @ColumnInfo(name = "team1_id")            val team1Id: String?,
    @ColumnInfo(name = "team2_id")            val team2Id: String?,
    val score1: Int?,
    val score2: Int?,
    @ColumnInfo(name = "winner_id")           val winnerId: String?,
    @ColumnInfo(name = "loser_id")            val loserId: String?,
    val status: MatchStatus,
    @ColumnInfo(name = "next_match_id")       val nextMatchId: String?,
    @ColumnInfo(name = "next_loser_match_id") val nextLoserMatchId: String?
)

@Entity(tableName = "standings")
data class StandingEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "tournament_id") val tournamentId: String,
    @ColumnInfo(name = "team_id")       val teamId: String,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val points: Int,
    val buchholz: Double,
    @ColumnInfo(name = "goals_for")     val goalsFor: Int,
    @ColumnInfo(name = "goals_against") val goalsAgainst: Int
)

@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "tournament_id") val tournamentId: String,
    @ColumnInfo(name = "created_by")    val createdBy: String,
    val message: String,
    @ColumnInfo(name = "created_at")    val createdAt: String
)

@Entity(tableName = "device_tokens")
data class DeviceTokenEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id")    val userId: String,
    val token: String,
    val platform: DevicePlatform,
    @ColumnInfo(name = "created_at") val createdAt: String
)