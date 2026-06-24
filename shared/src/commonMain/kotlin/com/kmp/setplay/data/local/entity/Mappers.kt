package com.kmp.setplay.data.local.entity

import com.kmp.setplay.domain.model.Announcement
import com.kmp.setplay.domain.model.DeviceToken
import com.kmp.setplay.domain.model.Match
import com.kmp.setplay.domain.model.Player
import com.kmp.setplay.domain.model.Round
import com.kmp.setplay.domain.model.Standing
import com.kmp.setplay.domain.model.Team
import com.kmp.setplay.domain.model.Tournament
import kotlinx.datetime.Instant

// ── Tournament ────────────────────────────────────────────────────────────────

fun TournamentEntity.toDomain() = Tournament(
    id = id,
    name = name,
    format = format,
    status = status,
    createdBy = createdBy,
    inviteCode = inviteCode,
    maxTeams = maxTeams,
    isPublic = isPublic,
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt)
)

fun Tournament.toEntity() = TournamentEntity(
    id = id,
    name = name,
    format = format,
    status = status,
    createdBy = createdBy,
    inviteCode = inviteCode,
    maxTeams = maxTeams,
    isPublic = isPublic,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString()
)

// ── Team ──────────────────────────────────────────────────────────────────────

fun TeamEntity.toDomain() = Team(
    id = id,
    tournamentId = tournamentId,
    name = name,
    seed = seed,
    logoUrl = logoUrl,
    createdAt = Instant.parse(createdAt)
)

fun Team.toEntity() = TeamEntity(
    id = id,
    tournamentId = tournamentId,
    name = name,
    seed = seed,
    logoUrl = logoUrl,
    createdAt = createdAt.toString()
)

// ── Player ────────────────────────────────────────────────────────────────────

fun PlayerEntity.toDomain() = Player(id = id, teamId = teamId, name = name)
fun Player.toEntity() = PlayerEntity(id = id, teamId = teamId, name = name)

// ── Round ─────────────────────────────────────────────────────────────────────

fun RoundEntity.toDomain() = Round(
    id = id,
    tournamentId = tournamentId,
    roundNumber = roundNumber,
    name = name
)

fun Round.toEntity() = RoundEntity(
    id = id,
    tournamentId = tournamentId,
    roundNumber = roundNumber,
    name = name
)

// ── Match ─────────────────────────────────────────────────────────────────────

fun MatchEntity.toDomain() = Match(
    id = id,
    roundId = roundId,
    tournamentId = tournamentId,
    matchNumber = matchNumber,
    team1Id = team1Id,
    team2Id = team2Id,
    score1 = score1,
    score2 = score2,
    winnerId = winnerId,
    loserId = loserId,
    status = status,
    nextMatchId = nextMatchId,
    nextLoserMatchId = nextLoserMatchId
)

fun Match.toEntity() = MatchEntity(
    id = id,
    roundId = roundId,
    tournamentId = tournamentId,
    matchNumber = matchNumber,
    team1Id = team1Id,
    team2Id = team2Id,
    score1 = score1,
    score2 = score2,
    winnerId = winnerId,
    loserId = loserId,
    status = status,
    nextMatchId = nextMatchId,
    nextLoserMatchId = nextLoserMatchId
)

// ── Standing ──────────────────────────────────────────────────────────────────

fun StandingEntity.toDomain() = Standing(
    id = id,
    tournamentId = tournamentId,
    teamId = teamId,
    wins = wins,
    losses = losses,
    draws = draws,
    points = points,
    buchholz = buchholz,
    goalsFor = goalsFor,
    goalsAgainst = goalsAgainst
)

fun Standing.toEntity() = StandingEntity(
    id = id,
    tournamentId = tournamentId,
    teamId = teamId,
    wins = wins,
    losses = losses,
    draws = draws,
    points = points,
    buchholz = buchholz,
    goalsFor = goalsFor,
    goalsAgainst = goalsAgainst
)

// ── Announcement ──────────────────────────────────────────────────────────────

fun AnnouncementEntity.toDomain() = Announcement(
    id = id,
    tournamentId = tournamentId,
    createdBy = createdBy,
    message = message,
    createdAt = Instant.parse(createdAt)
)

fun Announcement.toEntity() = AnnouncementEntity(
    id = id,
    tournamentId = tournamentId,
    createdBy = createdBy,
    message = message,
    createdAt = createdAt.toString()
)

// ── DeviceToken ───────────────────────────────────────────────────────────────

fun DeviceTokenEntity.toDomain() = DeviceToken(
    id = id,
    userId = userId,
    token = token,
    platform = platform,
    createdAt = Instant.parse(createdAt)
)

fun DeviceToken.toEntity() = DeviceTokenEntity(
    id = id,
    userId = userId,
    token = token,
    platform = platform,
    createdAt = createdAt.toString()
)