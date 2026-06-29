package com.kmp.setplay.domain.repository

import com.kmp.setplay.domain.model.Announcement
import com.kmp.setplay.domain.model.BracketFormat
import com.kmp.setplay.domain.model.Match
import com.kmp.setplay.domain.model.OrganizerRole
import com.kmp.setplay.domain.model.Standing
import com.kmp.setplay.domain.model.Team
import com.kmp.setplay.domain.model.Tournament
import kotlinx.coroutines.flow.Flow

interface TournamentRepository {

    // ── Organizers ────────────────────────────────────────────────────────────
    /** Returns the role of [userId] in [tournamentId], or null if not an organizer. */
    suspend fun getOrganizerRole(tournamentId: String, userId: String): Result<OrganizerRole?>

    // ── Tournaments ───────────────────────────────────────────────────────────
    /** Observe all tournaments created by the current user. Live updates via Realtime. */
    fun observeMyTournaments(userId: String): Flow<List<Tournament>>

    /** Observe a single tournament by ID. Live updates via Realtime. */
    fun observeTournament(tournamentId: String): Flow<Tournament?>

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
    suspend fun deleteTeam(teamId: String): Result<Unit>


    // ── Join ──────────────────────────────────────────────────────────────────
    suspend fun getTournamentByInviteCode(code: String): Result<Tournament>


    // ── Bracket ───────────────────────────────────────────────────────────────
    /** Generates bracket rounds + matches in Supabase and caches locally. */
    suspend fun generateBracket(tournamentId: String): Result<Unit>

    /** Updates a match result and advances winners to next round. */
    suspend fun updateMatch(
        matchId: String,
        score1: Int,
        score2: Int
    ): Result<Unit>
}