package com.kmp.setplay.domain.repository

import com.kmp.setplay.domain.bracket.SingleEliminationGenerator
import com.kmp.setplay.domain.model.Announcement
import com.kmp.setplay.domain.model.BracketFormat
import com.kmp.setplay.domain.model.Match
import com.kmp.setplay.domain.model.OrganizerRole
import com.kmp.setplay.domain.model.ShareViewer
import com.kmp.setplay.domain.model.Standing
import com.kmp.setplay.domain.model.Team
import com.kmp.setplay.domain.model.Tournament
import com.kmp.setplay.domain.model.TournamentOrganizer
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow

/** Lightweight join-status summary shown on a Discover card without opening the tournament. */
data class ParticipationSummary(val participantCount: Int, val hasJoined: Boolean)

interface TournamentRepository {

    // ── Organizers ────────────────────────────────────────────────────────────
    /** Returns the role of [userId] in [tournamentId], or null if not an organizer. */
    suspend fun getOrganizerRole(tournamentId: String, userId: String): Result<OrganizerRole?>

    /** Full list of organizers/owner for a tournament — used on the Participants tab. */
    suspend fun getOrganizers(tournamentId: String): Result<List<TournamentOrganizer>>

    // ── Tournaments ───────────────────────────────────────────────────────────
    /** Observe all tournaments created by the current user. Live updates via Realtime. */
    fun observeMyTournaments(userId: String): Flow<List<Tournament>>

    /** Observe a single tournament by ID. Live updates via Realtime. */
    fun observeTournament(tournamentId: String): Flow<Tournament?>

    /** Fetches public tournaments open for registration or in progress, for the Discover sub-tab. */
    suspend fun getPublicTournaments(): Result<List<Tournament>>

    /** Participant count + whether [userId] has already joined — used on Discover cards. */
    suspend fun getParticipationSummary(tournamentId: String, userId: String?): Result<ParticipationSummary>

    /** Observe matches for a tournament. Live updates via Realtime. */
    fun observeMatches(tournamentId: String): Flow<List<Match>>

    /** Observe standings for a tournament (Round Robin / Swiss / League). */
    fun observeStandings(tournamentId: String): Flow<List<Standing>>

    /** Observe announcements for a tournament. */
    fun observeAnnouncements(tournamentId: String): Flow<List<Announcement>>
    suspend fun createTournament(
        name: String,
        format: BracketFormat,
        maxTeams: Int,
        isPublic: Boolean
    ): Result<Tournament>
    suspend fun updateTournament(tournament: Tournament): Result<Unit>
    suspend fun deleteTournament(tournamentId: String): Result<Unit>


    // ── Teams ─────────────────────────────────────────────────────────────────
    fun observeTeams(tournamentId: String): Flow<List<Team>>
    suspend fun addTeam(tournamentId: String, name: String, seed: Int?): Result<Team>
    suspend fun renameTeam(teamId: String, name: String): Result<Unit>
    suspend fun deleteTeam(teamId: String): Result<Unit>

    /** Self-registers the current user into a public tournament using a player/team display name. */
    suspend fun registerForTournament(tournamentId: String, userId: String, displayName: String): Result<Team>


    // ── Join ──────────────────────────────────────────────────────────────────
    suspend fun getTournamentByInviteCode(code: String): Result<Tournament>


    // ── Bracket ───────────────────────────────────────────────────────────────
    /**
     * Generates bracket rounds + matches in Supabase and caches locally.
     * For private tournaments this runs right after creation; for public tournaments
     * it runs when the organizer closes registration.
     */
    suspend fun generateBracket(
        tournamentId: String,
        seeding: SingleEliminationGenerator.Seeding = SingleEliminationGenerator.Seeding.SEEDED,
        includeThirdPlace: Boolean = false
    ): Result<Unit>

    /** Updates a match result and advances winners (and, for semis, the loser) to next matches. */
    suspend fun updateMatch(
        matchId: String,
        score1: Int,
        score2: Int
    ): Result<Unit>

    /** Sets or clears the scheduled date/time shown under a bracket match. */
    suspend fun setMatchSchedule(matchId: String, scheduledAt: Instant?): Result<Unit>


    // ── Share code access (private tournaments) ─────────────────────────────────
    /**
     * Records/heartbeats that [userId] viewed a private tournament via its share code.
     * Upserts on (tournamentId, userId) — never touches the `revoked` flag, so a
     * viewer's own visit can never clear an organizer-set revocation.
     */
    suspend fun recordShareView(tournamentId: String, userId: String): Result<Unit>

    /** Whether [userId]'s share-code access to [tournamentId] has been revoked by an organizer. */
    suspend fun isShareAccessRevoked(tournamentId: String, userId: String): Result<Boolean>

    /** Full list of users who have viewed a private tournament via share code — organizer only. */
    suspend fun getShareViewers(tournamentId: String): Result<List<ShareViewer>>

    suspend fun revokeShareAccess(tournamentId: String, userId: String): Result<Unit>
    suspend fun restoreShareAccess(tournamentId: String, userId: String): Result<Unit>
}