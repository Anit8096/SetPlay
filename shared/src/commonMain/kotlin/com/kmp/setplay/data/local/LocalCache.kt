package com.kmp.setplay.data.local

import com.kmp.setplay.domain.model.Announcement
import com.kmp.setplay.domain.model.Match
import com.kmp.setplay.domain.model.Round
import com.kmp.setplay.domain.model.Standing
import com.kmp.setplay.domain.model.Team
import com.kmp.setplay.domain.model.Tournament
import kotlinx.coroutines.flow.Flow

/**
 * Platform-specific local cache abstraction.
 *
 * androidMain → RoomLocalCache (Room DAOs)
 * webMain     → NoOpLocalCache (always returns empty/null, Supabase is source of truth)
 */
interface LocalCache {

    // ── Tournaments ───────────────────────────────────────────────────────────
    fun observeMyTournaments(userId: String): Flow<List<Tournament>>
    fun observeTournament(tournamentId: String): Flow<Tournament?>
    suspend fun saveTournaments(tournaments: List<Tournament>)
    suspend fun saveTournament(tournament: Tournament)
    suspend fun deleteTournament(tournamentId: String)
    suspend fun getTournamentByInviteCode(code: String): Tournament?

    // ── Teams ─────────────────────────────────────────────────────────────────
    fun observeTeams(tournamentId: String): Flow<List<Team>>
    suspend fun saveTeams(teams: List<Team>)
    suspend fun saveTeam(team: Team)

    // ── Rounds ────────────────────────────────────────────────────────────────
    suspend fun saveRounds(rounds: List<Round>)

    // ── Matches ───────────────────────────────────────────────────────────────
    fun observeMatches(tournamentId: String): Flow<List<Match>>
    suspend fun saveMatches(matches: List<Match>)
    suspend fun saveMatch(match: Match)
    suspend fun getMatch(matchId: String): Match?

    // ── Standings ─────────────────────────────────────────────────────────────
    fun observeStandings(tournamentId: String): Flow<List<Standing>>
    suspend fun saveStandings(standings: List<Standing>)

    // ── Announcements ─────────────────────────────────────────────────────────
    fun observeAnnouncements(tournamentId: String): Flow<List<Announcement>>
    suspend fun saveAnnouncements(announcements: List<Announcement>)
}
