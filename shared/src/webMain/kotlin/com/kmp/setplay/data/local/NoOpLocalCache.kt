package com.kmp.setplay.data.local

import com.kmp.setplay.domain.model.Announcement
import com.kmp.setplay.domain.model.Match
import com.kmp.setplay.domain.model.Round
import com.kmp.setplay.domain.model.Standing
import com.kmp.setplay.domain.model.Team
import com.kmp.setplay.domain.model.Tournament
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Web platform local cache — no-op implementation.
 *
 * All observe functions return empty flows — the repository's onStart
 * block fetches from Supabase on every collection, which is the correct
 * behavior for web where Supabase Realtime is the source of truth.
 *
 * All save/delete functions are no-ops — nothing is persisted locally.
 */
class NoOpLocalCache : LocalCache {

    override fun observeMyTournaments(userId: String): Flow<List<Tournament>> = flowOf(emptyList())
    override fun observeTournament(tournamentId: String): Flow<Tournament?> = flowOf(null)
    override suspend fun saveTournaments(tournaments: List<Tournament>) = Unit
    override suspend fun saveTournament(tournament: Tournament) = Unit
    override suspend fun deleteTournament(tournamentId: String) = Unit
    override suspend fun getTournamentByInviteCode(code: String): Tournament? = null

    override fun observeTeams(tournamentId: String): Flow<List<Team>> = flowOf(emptyList())
    override suspend fun saveTeams(teams: List<Team>) = Unit
    override suspend fun saveTeam(team: Team) = Unit

    override suspend fun saveRounds(rounds: List<Round>) = Unit

    override fun observeMatches(tournamentId: String): Flow<List<Match>> = flowOf(emptyList())
    override suspend fun saveMatches(matches: List<Match>) = Unit
    override suspend fun saveMatch(match: Match) = Unit
    override suspend fun getMatch(matchId: String): Match? = null

    override fun observeStandings(tournamentId: String): Flow<List<Standing>> = flowOf(emptyList())
    override suspend fun saveStandings(standings: List<Standing>) = Unit

    override fun observeAnnouncements(tournamentId: String): Flow<List<Announcement>> = flowOf(emptyList())
    override suspend fun saveAnnouncements(announcements: List<Announcement>) = Unit
}
