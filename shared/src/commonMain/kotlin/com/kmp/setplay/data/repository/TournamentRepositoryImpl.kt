package com.kmp.setplay.data.repository

import com.kmp.setplay.data.local.LocalCache
import com.kmp.setplay.data.remote.dto.AdvanceWinnerRequestDto
import com.kmp.setplay.data.remote.dto.AnnouncementDto
import com.kmp.setplay.data.remote.dto.InsertTeamRequestDto
import com.kmp.setplay.data.remote.dto.UpdateTeamRequestDto
import com.kmp.setplay.data.remote.dto.InsertTournamentRequestDto
import com.kmp.setplay.data.remote.dto.MatchDto
import com.kmp.setplay.data.remote.dto.RecordShareViewRequestDto
import com.kmp.setplay.data.remote.dto.RoundDto
import com.kmp.setplay.data.remote.dto.ShareViewerDto
import com.kmp.setplay.data.remote.dto.StandingDto
import com.kmp.setplay.data.remote.dto.TeamDto
import com.kmp.setplay.data.remote.dto.TournamentDto
import com.kmp.setplay.data.remote.dto.TournamentOrganizerDto
import com.kmp.setplay.data.remote.dto.UpdateMatchResultRequestDto
import com.kmp.setplay.data.remote.dto.UpdateMatchScheduleRequestDto
import com.kmp.setplay.data.remote.dto.UpdateShareViewerRequestDto
import com.kmp.setplay.data.remote.dto.UpdateTournamentRequestDto
import com.kmp.setplay.domain.bracket.SingleEliminationGenerator
import com.kmp.setplay.domain.model.Announcement
import com.kmp.setplay.domain.model.BracketFormat
import com.kmp.setplay.domain.model.Match
import com.kmp.setplay.domain.model.MatchStatus
import com.kmp.setplay.domain.model.OrganizerRole
import com.kmp.setplay.domain.model.ShareViewer
import com.kmp.setplay.domain.model.Standing
import com.kmp.setplay.domain.model.Team
import com.kmp.setplay.domain.model.Tournament
import com.kmp.setplay.domain.model.TournamentOrganizer
import com.kmp.setplay.domain.model.TournamentStatus
import com.kmp.setplay.domain.repository.ParticipationSummary
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class TournamentRepositoryImpl(
    private val supabase: SupabaseClient,
    private val cache: LocalCache
) : TournamentRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val activeChannels = mutableSetOf<String>()

    private suspend fun <T> io(block: suspend () -> T): Result<T> =
        withContext(Dispatchers.Default) { runCatching { block() } }

    // ── Tournaments ───────────────────────────────────────────────────────────

    override fun observeMyTournaments(userId: String): Flow<List<Tournament>> = flow {
        // 1. Fetch from Supabase first — this is the source of truth
        val remote = runCatching {
            supabase.postgrest["tournaments"]
                .select { filter { eq("created_by", userId) } }
                .decodeList<TournamentDto>()
                .map { it.toDomain() }
        }.getOrNull()

        if (remote != null) {
            // 2. Save fresh data to Room
            cache.saveTournaments(remote)
        }
        // 3. Open Realtime channel so future changes flow in automatically
        scope.launch { subscribeToTournaments(userId) }

        // 4. Now observe Room — it has up-to-date data (or stale data if network failed)
        // Either way the UI gets something and Realtime keeps it current from here
        cache.observeMyTournaments(userId).collect { emit(it) }
    }.flowOn(Dispatchers.Default)

    override fun observeTournament(tournamentId: String): Flow<Tournament?> = flow {
        // 1. Fetch from Supabase first
        val remote = runCatching {
            supabase.postgrest["tournaments"]
                .select { filter { eq("id", tournamentId) } }
                .decodeSingle<TournamentDto>()
                .toDomain()
        }.getOrNull()

        if (remote != null) {
            // 2. Save to Room
            cache.saveTournament(remote)
        }
        // 3. Open Realtime channel
        scope.launch { subscribeToTournamentDetail(tournamentId) }

        // 4. Observe Room
        cache.observeTournament(tournamentId).collect { emit(it) }
    }.flowOn(Dispatchers.Default)

    override suspend fun getPublicTournaments(): Result<List<Tournament>> = io {
        supabase.postgrest["tournaments"]
            .select { filter { eq("is_public", true) } }
            .decodeList<TournamentDto>()
            .map { it.toDomain() }
            .filter { it.status == TournamentStatus.REGISTRATION || it.status == TournamentStatus.IN_PROGRESS }
    }

    override fun observeMatches(tournamentId: String): Flow<List<Match>> = flow {
        val remote = runCatching {
            supabase.postgrest["matches"]
                .select { filter { eq("tournament_id", tournamentId) } }
                .decodeList<MatchDto>()
                .map { it.toDomain() }
        }.getOrNull()

        if (remote != null) cache.saveMatches(remote)

        scope.launch { subscribeToTournamentDetail(tournamentId) }

        cache.observeMatches(tournamentId).collect { emit(it) }
    }.flowOn(Dispatchers.Default)

    override fun observeTeams(tournamentId: String): Flow<List<Team>> = flow {
        val remote = runCatching {
            supabase.postgrest["teams"]
                .select { filter { eq("tournament_id", tournamentId) } }
                .decodeList<TeamDto>()
                .map { it.toDomain() }
        }.getOrNull()

        if (remote != null) cache.saveTeams(remote)

        cache.observeTeams(tournamentId).collect { emit(it) }
    }.flowOn(Dispatchers.Default)

    override fun observeStandings(tournamentId: String): Flow<List<Standing>> = flow {
        val remote = runCatching {
            supabase.postgrest["standings"]
                .select { filter { eq("tournament_id", tournamentId) } }
                .decodeList<StandingDto>()
                .map { it.toDomain() }
        }.getOrNull()

        if (remote != null) cache.saveStandings(remote)

        cache.observeStandings(tournamentId).collect { emit(it) }
    }.flowOn(Dispatchers.Default)

    override fun observeAnnouncements(tournamentId: String): Flow<List<Announcement>> = flow {
        val remote = runCatching {
            supabase.postgrest["announcements"]
                .select { filter { eq("tournament_id", tournamentId) } }
                .decodeList<AnnouncementDto>()
                .map { it.toDomain() }
        }.getOrNull()

        if (remote != null) cache.saveAnnouncements(remote)

        cache.observeAnnouncements(tournamentId).collect { emit(it) }
    }.flowOn(Dispatchers.Default)

    // ── Writes (already remote-first — Supabase confirmed before cache update) ─

    override suspend fun createTournament(
        name: String,
        format: BracketFormat,
        maxTeams: Int,
        isPublic: Boolean
    ): Result<Tournament> = io {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: error("Not authenticated")

        val dto = supabase.postgrest["tournaments"]
            .insert(
                InsertTournamentRequestDto(
                    name = name,
                    format = format,
                    status = TournamentStatus.DRAFT,
                    createdBy = userId,
                    maxTeams = maxTeams,
                    isPublic = isPublic
                )
            ) { select() }
            .decodeSingle<TournamentDto>()

        val domain = dto.toDomain()
        cache.saveTournament(domain)
        domain
    }

    override suspend fun updateTournament(tournament: Tournament): Result<Unit> = io {
        supabase.postgrest["tournaments"]
            .update(
                UpdateTournamentRequestDto(
                    name = tournament.name,
                    status = tournament.status,
                    isPublic = tournament.isPublic,
                    maxTeams = tournament.maxTeams,
                    registrationDeadline = tournament.registrationDeadline
                )
            ) { filter { eq("id", tournament.id) } }
        cache.saveTournament(tournament)
    }

    override suspend fun deleteTournament(tournamentId: String): Result<Unit> = io {
        supabase.postgrest["tournaments"]
            .delete { filter { eq("id", tournamentId) } }
        cache.deleteTournament(tournamentId)
    }

    // ── Teams ─────────────────────────────────────────────────────────────────

    override suspend fun addTeam(
        tournamentId: String,
        name: String,
        seed: Int?
    ): Result<Team> = io {
        val dto = supabase.postgrest["teams"]
            .insert(
                InsertTeamRequestDto(
                    tournamentId = tournamentId,
                    name = name,
                    seed = seed
                )
            ) { select() }
            .decodeSingle<TeamDto>()

        val domain = dto.toDomain()
        cache.saveTeam(domain)
        domain
    }

    override suspend fun deleteTeam(teamId: String): Result<Unit> = io {
        supabase.postgrest["teams"]
            .delete { filter { eq("id", teamId) } }
        // Without this the team lingers in Room/NoOp state until the next full remote
        // refetch (e.g. app restart), since deletes aren't picked up by Realtime here.
        cache.deleteTeam(teamId)
    }

    override suspend fun renameTeam(teamId: String, name: String): Result<Unit> = io {
        supabase.postgrest["teams"]
            .update(UpdateTeamRequestDto(name = name)) { filter { eq("id", teamId) } }
        val updated = supabase.postgrest["teams"]
            .select { filter { eq("id", teamId) } }
            .decodeSingle<TeamDto>()
            .toDomain()
        // Same reasoning as deleteTeam — write the confirmed remote result into the
        // cache so the UI (Room Flow / NoOp StateFlow) actually reflects the rename
        // instead of waiting for the next cold fetch.
        cache.saveTeam(updated)
    }

    override suspend fun registerForTournament(
        tournamentId: String,
        userId: String,
        displayName: String
    ): Result<Team> = io {
        val tournament = supabase.postgrest["tournaments"]
            .select { filter { eq("id", tournamentId) } }
            .decodeSingle<TournamentDto>()
            .toDomain()

        // maxTeams == 0 is the explicit "No Limit" sentinel (see CreateTournamentViewModel),
        // not a real cap, so only enforce it when a real limit was set.
        if (tournament.maxTeams > 0) {
            val existingCount = supabase.postgrest["teams"]
                .select { filter { eq("tournament_id", tournamentId) } }
                .decodeList<TeamDto>()
                .size
            if (existingCount >= tournament.maxTeams) {
                error("This tournament is full")
            }
        }

        val dto = supabase.postgrest["teams"]
            .insert(
                InsertTeamRequestDto(
                    tournamentId = tournamentId,
                    name = displayName,
                    seed = null,
                    userId = userId
                )
            ) { select() }
            .decodeSingle<TeamDto>()

        val domain = dto.toDomain()
        cache.saveTeam(domain)
        domain
    }

    override suspend fun getParticipationSummary(
        tournamentId: String,
        userId: String?
    ): Result<ParticipationSummary> = io {
        val teams = supabase.postgrest["teams"]
            .select { filter { eq("tournament_id", tournamentId) } }
            .decodeList<TeamDto>()
            .map { it.toDomain() }

        ParticipationSummary(
            participantCount = teams.size,
            hasJoined = userId != null && teams.any { it.userId == userId }
        )
    }

    // ── Join ──────────────────────────────────────────────────────────────────

    override suspend fun getTournamentByInviteCode(code: String): Result<Tournament> =
        io {
            // Always check Supabase first for invite code lookups —
            // the tournament may have been created on another device
            val dto = supabase.postgrest["tournaments"]
                .select { filter { eq("invite_code", code) } }
                .decodeSingle<TournamentDto>()
            val domain = dto.toDomain()
            cache.saveTournament(domain)
            domain
        }

    // ── Bracket ───────────────────────────────────────────────────────────────

    override suspend fun generateBracket(
        tournamentId: String,
        seeding: SingleEliminationGenerator.Seeding,
        includeThirdPlace: Boolean
    ): Result<Unit> = io {
        val teams = supabase.postgrest["teams"]
            .select { filter { eq("tournament_id", tournamentId) } }
            .decodeList<TeamDto>()
            .map { it.toDomain() }

        require(teams.size >= 2) { "Need at least 2 teams to generate bracket" }

        val result = SingleEliminationGenerator.generate(
            tournamentId = tournamentId,
            teams = teams,
            idGenerator = { Uuid.random().toString() },
            seeding = seeding,
            includeThirdPlace = includeThirdPlace
        )

        supabase.postgrest["rounds"].insert(
            result.rounds.map { round ->
                RoundDto(
                    id = round.id,
                    tournamentId = round.tournamentId,
                    roundNumber = round.roundNumber,
                    name = round.name
                )
            }
        )
        cache.saveRounds(result.rounds)

        supabase.postgrest["matches"].insert(
            result.matches.map { match ->
                MatchDto(
                    id = match.id,
                    roundId = match.roundId,
                    tournamentId = match.tournamentId,
                    matchNumber = match.matchNumber,
                    team1Id = match.team1Id,
                    team2Id = match.team2Id,
                    score1 = match.score1,
                    score2 = match.score2,
                    status = match.status,
                    nextMatchId = match.nextMatchId,
                    nextLoserMatchId = match.nextLoserMatchId,
                    winnerId = match.winnerId,
                    loserId = match.loserId
                )
            }
        )
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
    ): Result<Unit> = io {
        // Fetch from Supabase directly — don't trust cache for match state
        val match = supabase.postgrest["matches"]
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

        // 1. Send to Supabase
        supabase.postgrest["matches"]
            .update(
                UpdateMatchResultRequestDto(
                    score1 = score1,
                    score2 = score2,
                    winnerId = winnerId,
                    loserId = loserId,
                    status = MatchStatus.COMPLETED
                )
            ) { filter { eq("id", matchId) } }

        // 2. Confirmed — now update cache
        cache.saveMatch(updatedMatch)

        // 3. Advance winner into next match
        val allMatches = supabase.postgrest["matches"]
            .select { filter { eq("tournament_id", updatedMatch.tournamentId) } }
            .decodeList<MatchDto>()
            .map { it.toDomain() }

        SingleEliminationGenerator.advanceWinner(updatedMatch, allMatches)
            .forEach { updatedNextOrLoserMatch ->
                supabase.postgrest["matches"]
                    .update(
                        AdvanceWinnerRequestDto(
                            team1Id = updatedNextOrLoserMatch.team1Id,
                            team2Id = updatedNextOrLoserMatch.team2Id
                        )
                    ) { filter { eq("id", updatedNextOrLoserMatch.id) } }
                cache.saveMatch(updatedNextOrLoserMatch)
            }
    }

    override suspend fun setMatchSchedule(matchId: String, scheduledAt: Instant?): Result<Unit> = io {
        supabase.postgrest["matches"]
            .update(UpdateMatchScheduleRequestDto(scheduledAt = scheduledAt)) {
                filter { eq("id", matchId) }
            }
        val updated = supabase.postgrest["matches"]
            .select { filter { eq("id", matchId) } }
            .decodeSingle<MatchDto>()
            .toDomain()
        cache.saveMatch(updated)
    }

    // ── Organizers ────────────────────────────────────────────────────────────

    override suspend fun getOrganizerRole(
        tournamentId: String,
        userId: String
    ): Result<OrganizerRole?> = io {
        val results = supabase.postgrest["tournament_organizers"]
            .select {
                filter {
                    eq("tournament_id", tournamentId)
                    eq("user_id", userId)
                }
            }
            .decodeList<TournamentOrganizerDto>()
        results.firstOrNull()?.role
    }

    override suspend fun getOrganizers(tournamentId: String): Result<List<TournamentOrganizer>> = io {
        supabase.postgrest["tournament_organizers"]
            .select { filter { eq("tournament_id", tournamentId) } }
            .decodeList<TournamentOrganizerDto>()
            .map { it.toDomain() }
    }

    // ── Share code access ────────────────────────────────────────────────────

    override suspend fun recordShareView(tournamentId: String, userId: String): Result<Unit> = io {
        supabase.postgrest["tournament_share_views"]
            .upsert(
                RecordShareViewRequestDto(
                    tournamentId = tournamentId,
                    userId = userId,
                    lastViewedAt = kotlin.time.Clock.System.now()
                )
            ) { onConflict = "tournament_id,user_id" }
    }

    override suspend fun isShareAccessRevoked(tournamentId: String, userId: String): Result<Boolean> = io {
        supabase.postgrest["tournament_share_views"]
            .select {
                filter {
                    eq("tournament_id", tournamentId)
                    eq("user_id", userId)
                }
            }
            .decodeList<ShareViewerDto>()
            .firstOrNull()?.revoked ?: false
    }

    override suspend fun getShareViewers(tournamentId: String): Result<List<ShareViewer>> = io {
        supabase.postgrest["tournament_share_views"]
            .select { filter { eq("tournament_id", tournamentId) } }
            .decodeList<ShareViewerDto>()
            .map { it.toDomain() }
            .sortedByDescending { it.lastViewedAt }
    }

    override suspend fun revokeShareAccess(tournamentId: String, userId: String): Result<Unit> = io {
        supabase.postgrest["tournament_share_views"]
            .update(UpdateShareViewerRequestDto(revoked = true)) {
                filter {
                    eq("tournament_id", tournamentId)
                    eq("user_id", userId)
                }
            }
    }

    override suspend fun restoreShareAccess(tournamentId: String, userId: String): Result<Unit> = io {
        supabase.postgrest["tournament_share_views"]
            .update(UpdateShareViewerRequestDto(revoked = false)) {
                filter {
                    eq("tournament_id", tournamentId)
                    eq("user_id", userId)
                }
            }
    }

    // ── Realtime ──────────────────────────────────────────────────────────────

    private suspend fun subscribeToTournaments(userId: String) {
        val key = "tournaments:$userId"
        if (!activeChannels.add(key)) return

        val channel = supabase.realtime.channel(key)

        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "tournaments"
            filter("created_by", FilterOperator.EQ, userId)
        }

        channel.subscribe()

        runCatching {
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
        }.onFailure {
            activeChannels.remove(key)
        }
    }

    private suspend fun subscribeToTournamentDetail(tournamentId: String) {
        val key = "tournament:$tournamentId"
        if (!activeChannels.add(key)) return

        val channel = supabase.realtime.channel(key)

        val matchChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "matches"
            filter("tournament_id", FilterOperator.EQ, tournamentId)
        }

        channel.subscribe()

        runCatching {
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
        }.onFailure {
            activeChannels.remove(key)
        }
    }
}