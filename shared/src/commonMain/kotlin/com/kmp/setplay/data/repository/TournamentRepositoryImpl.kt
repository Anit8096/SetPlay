package com.kmp.setplay.data.repository

import com.kmp.setplay.data.local.LocalCache
import com.kmp.setplay.data.remote.dto.AnnouncementDto
import com.kmp.setplay.data.remote.dto.MatchDto
import com.kmp.setplay.data.remote.dto.RoundDto
import com.kmp.setplay.data.remote.dto.StandingDto
import com.kmp.setplay.data.remote.dto.TeamDto
import com.kmp.setplay.data.remote.dto.TournamentDto
import com.kmp.setplay.domain.bracket.SingleEliminationGenerator
import com.kmp.setplay.domain.model.Announcement
import com.kmp.setplay.domain.model.BracketFormat
import com.kmp.setplay.domain.model.Match
import com.kmp.setplay.domain.model.MatchStatus
import com.kmp.setplay.domain.model.Standing
import com.kmp.setplay.domain.model.Team
import com.kmp.setplay.domain.model.Tournament
import com.kmp.setplay.domain.model.TournamentStatus
import com.kmp.setplay.domain.repository.TournamentRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class TournamentRepositoryImpl(
    private val supabase: SupabaseClient,
    private val cache: LocalCache
) : TournamentRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ── Tournaments ───────────────────────────────────────────────────────────

    override fun observeMyTournaments(userId: String): Flow<List<Tournament>> {
        scope.launch { subscribeToTournaments(userId) }
        return cache.observeMyTournaments(userId).onStart {
            runCatching {
                val dtos = supabase.postgrest["tournaments"]
                    .select { filter { eq("created_by", userId) } }
                    .decodeList<TournamentDto>()
                cache.saveTournaments(dtos.map { it.toDomain() })
            }
        }
    }

    override fun observeTournament(tournamentId: String): Flow<Tournament?> {
        scope.launch { subscribeToTournamentDetail(tournamentId) }
        return cache.observeTournament(tournamentId).onStart {
            runCatching {
                val dto = supabase.postgrest["tournaments"]
                    .select { filter { eq("id", tournamentId) } }
                    .decodeSingle<TournamentDto>()
                cache.saveTournament(dto.toDomain())
            }
        }
    }

    override fun observeMatches(tournamentId: String): Flow<List<Match>> =
        cache.observeMatches(tournamentId).onStart {
            runCatching {
                val dtos = supabase.postgrest["matches"]
                    .select { filter { eq("tournament_id", tournamentId) } }
                    .decodeList<MatchDto>()
                cache.saveMatches(dtos.map { it.toDomain() })
            }
        }

    override fun observeStandings(tournamentId: String): Flow<List<Standing>> =
        cache.observeStandings(tournamentId).onStart {
            runCatching {
                val dtos = supabase.postgrest["standings"]
                    .select { filter { eq("tournament_id", tournamentId) } }
                    .decodeList<StandingDto>()
                cache.saveStandings(dtos.map { it.toDomain() })
            }
        }

    override fun observeAnnouncements(tournamentId: String): Flow<List<Announcement>> =
        cache.observeAnnouncements(tournamentId).onStart {
            runCatching {
                val dtos = supabase.postgrest["announcements"]
                    .select { filter { eq("tournament_id", tournamentId) } }
                    .decodeList<AnnouncementDto>()
                cache.saveAnnouncements(dtos.map { it.toDomain() })
            }
        }

    override suspend fun createTournament(
        name: String,
        format: BracketFormat,
        maxTeams: Int,
        isPublic: Boolean
    ): Result<Tournament> = runCatching {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: error("Not authenticated")

        val dto = supabase.postgrest["tournaments"]
            .insert(mapOf(
                "name" to name,
                "format" to format.name,
                "status" to TournamentStatus.DRAFT.name,
                "created_by" to userId,
                "max_teams" to maxTeams,
                "is_public" to isPublic
            )) { select() }
            .decodeSingle<TournamentDto>()

        val domain = dto.toDomain()
        cache.saveTournament(domain)
        domain
    }

    override suspend fun updateTournament(tournament: Tournament): Result<Unit> = runCatching {
        supabase.postgrest["tournaments"]
            .update(mapOf(
                "name" to tournament.name,
                "status" to tournament.status.name,
                "is_public" to tournament.isPublic,
                "max_teams" to tournament.maxTeams
            )) { filter { eq("id", tournament.id) } }
        cache.saveTournament(tournament)
    }

    override suspend fun deleteTournament(tournamentId: String): Result<Unit> = runCatching {
        supabase.postgrest["tournaments"]
            .delete { filter { eq("id", tournamentId) } }
        cache.deleteTournament(tournamentId)
    }

    // ── Teams ─────────────────────────────────────────────────────────────────

    override fun observeTeams(tournamentId: String): Flow<List<Team>> =
        cache.observeTeams(tournamentId).onStart {
            runCatching {
                val dtos = supabase.postgrest["teams"]
                    .select { filter { eq("tournament_id", tournamentId) } }
                    .decodeList<TeamDto>()
                cache.saveTeams(dtos.map { it.toDomain() })
            }
        }

    override suspend fun addTeam(
        tournamentId: String,
        name: String,
        seed: Int?
    ): Result<Team> = runCatching {
        val dto = supabase.postgrest["teams"]
            .insert(mapOf(
                "tournament_id" to tournamentId,
                "name" to name,
                "seed" to seed
            )) { select() }
            .decodeSingle<TeamDto>()

        val domain = dto.toDomain()
        cache.saveTeam(domain)
        domain
    }

    override suspend fun deleteTeam(teamId: String): Result<Unit> = runCatching {
        supabase.postgrest["teams"]
            .delete { filter { eq("id", teamId) } }
    }

    // ── Join ──────────────────────────────────────────────────────────────────

    override suspend fun getTournamentByInviteCode(code: String): Result<Tournament> =
        runCatching {
            cache.getTournamentByInviteCode(code) ?: run {
                val dto = supabase.postgrest["tournaments"]
                    .select { filter { eq("invite_code", code) } }
                    .decodeSingle<TournamentDto>()
                val domain = dto.toDomain()
                cache.saveTournament(domain)
                domain
            }
        }

    // ── Bracket ───────────────────────────────────────────────────────────────

    override suspend fun generateBracket(tournamentId: String): Result<Unit> = runCatching {
        val teams = supabase.postgrest["teams"]
            .select { filter { eq("tournament_id", tournamentId) } }
            .decodeList<TeamDto>()
            .map { it.toDomain() }

        require(teams.size >= 2) { "Need at least 2 teams to generate bracket" }

        val result = SingleEliminationGenerator.generate(
            tournamentId = tournamentId,
            teams = teams,
            idGenerator = { Uuid.random().toString() }
        )

        supabase.postgrest["rounds"].insert(result.rounds.map { round ->
            mapOf(
                "id" to round.id,
                "tournament_id" to round.tournamentId,
                "round_number" to round.roundNumber,
                "name" to round.name
            )
        })
        cache.saveRounds(result.rounds)

        supabase.postgrest["matches"].insert(result.matches.map { match ->
            buildMap<String, Any?> {
                put("id", match.id)
                put("round_id", match.roundId)
                put("tournament_id", match.tournamentId)
                put("match_number", match.matchNumber)
                put("team1_id", match.team1Id)
                put("team2_id", match.team2Id)
                put("status", match.status.name)
                put("next_match_id", match.nextMatchId)
                put("winner_id", match.winnerId)
            }
        })
        cache.saveMatches(result.matches)

        supabase.postgrest["tournaments"]
            .update(mapOf("status" to TournamentStatus.IN_PROGRESS.name)) {
                filter { eq("id", tournamentId) }
            }
    }

    override suspend fun updateMatch(
        matchId: String,
        score1: Int,
        score2: Int
    ): Result<Unit> = runCatching {
        val match = cache.getMatch(matchId)
            ?: supabase.postgrest["matches"]
                .select { filter { eq("id", matchId) } }
                .decodeSingle<MatchDto>()
                .toDomain()

        val winnerId = if (score1 > score2) match.team1Id else match.team2Id
        val loserId  = if (score1 > score2) match.team2Id else match.team1Id

        val updatedMatch = match.copy(
            score1 = score1,
            score2 = score2,
            winnerId = winnerId,
            loserId = loserId,
            status = MatchStatus.COMPLETED
        )

        supabase.postgrest["matches"]
            .update(mapOf(
                "score1" to score1,
                "score2" to score2,
                "winner_id" to winnerId,
                "loser_id" to loserId,
                "status" to MatchStatus.COMPLETED.name
            )) { filter { eq("id", matchId) } }

        cache.saveMatch(updatedMatch)

        // Advance winner to next match
        val allMatches = supabase.postgrest["matches"]
            .select { filter { eq("tournament_id", updatedMatch.tournamentId) } }
            .decodeList<MatchDto>()
            .map { it.toDomain() }

        SingleEliminationGenerator.advanceWinner(updatedMatch, allMatches)
            .forEach { nextMatch ->
                supabase.postgrest["matches"]
                    .update(mapOf(
                        "team1_id" to nextMatch.team1Id,
                        "team2_id" to nextMatch.team2Id
                    )) { filter { eq("id", nextMatch.id) } }
                cache.saveMatch(nextMatch)
            }
    }

    // ── Realtime ──────────────────────────────────────────────────────────────

    private suspend fun subscribeToTournaments(userId: String) {
        val channel = supabase.realtime.channel("tournaments:$userId")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "tournaments"
            filter("created_by", FilterOperator.EQ, userId)
        }
        channel.subscribe()
        changes.collect { action ->
            when (action) {
                is PostgresAction.Insert -> runCatching {
                    cache.saveTournament(action.decodeRecord<TournamentDto>().toDomain())
                }
                is PostgresAction.Update -> runCatching {
                    cache.saveTournament(action.decodeRecord<TournamentDto>().toDomain())
                }
                is PostgresAction.Delete -> runCatching {
                    val id = action.oldRecord["id"]?.toString() ?: return@runCatching
                    cache.deleteTournament(id)
                }
                else -> Unit
            }
        }
    }

    private suspend fun subscribeToTournamentDetail(tournamentId: String) {
        val channel = supabase.realtime.channel("tournament:$tournamentId")
        val matchChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "matches"
            filter("created_by", FilterOperator.EQ, tournamentId)
        }
        channel.subscribe()
        matchChanges.collect { action ->
            when (action) {
                is PostgresAction.Insert -> runCatching {
                    cache.saveMatch(action.decodeRecord<MatchDto>().toDomain())
                }
                is PostgresAction.Update -> runCatching {
                    cache.saveMatch(action.decodeRecord<MatchDto>().toDomain())
                }
                else -> Unit
            }
        }
    }
}
