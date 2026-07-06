package com.kmp.setplay.data.local

import com.kmp.setplay.domain.model.Announcement
import com.kmp.setplay.domain.model.Match
import com.kmp.setplay.domain.model.Round
import com.kmp.setplay.domain.model.Standing
import com.kmp.setplay.domain.model.Team
import com.kmp.setplay.domain.model.Tournament
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Web platform local cache — in-memory implementation.
 *
 * On web there is no Room database, so we use MutableStateFlows as an
 * in-memory store. The repository's onStart block fetches from Supabase
 * and calls save*() here, which pushes data into the flows so the UI
 * actually receives it. Realtime updates also call save*() to keep
 * the flows current for the lifetime of the session.
 *
 * Nothing is persisted to disk — on refresh, data is re-fetched from Supabase.
 */
class NoOpLocalCache : LocalCache {

    // ── In-memory stores ──────────────────────────────────────────────────────
    // Prefixed with underscore to avoid clashing with interface parameter names

    private val _tournaments   = MutableStateFlow<List<Tournament>>(emptyList())
    private val _teams         = MutableStateFlow<List<Team>>(emptyList())
    private val _matches       = MutableStateFlow<List<Match>>(emptyList())
    private val _standings     = MutableStateFlow<List<Standing>>(emptyList())
    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())

    // ── Tournaments ───────────────────────────────────────────────────────────

    override fun observeMyTournaments(userId: String): Flow<List<Tournament>> =
        _tournaments.asStateFlow().map { list -> list.filter { it.createdBy == userId } }

    override fun observeTournament(tournamentId: String): Flow<Tournament?> =
        _tournaments.asStateFlow().map { list -> list.find { it.id == tournamentId } }

    override suspend fun saveTournaments(tournaments: List<Tournament>) {
        _tournaments.update { current ->
            val ids = tournaments.map { it.id }.toSet()
            current.filter { it.id !in ids } + tournaments
        }
    }

    override suspend fun saveTournament(tournament: Tournament) {
        _tournaments.update { current ->
            current.filter { it.id != tournament.id } + tournament
        }
    }

    override suspend fun deleteTournament(tournamentId: String) {
        _tournaments.update { current -> current.filter { it.id != tournamentId } }
        // Mirror Room's cascade so deleted-tournament children don't linger in memory either.
        _teams.update { current -> current.filter { it.tournamentId != tournamentId } }
        _matches.update { current -> current.filter { it.tournamentId != tournamentId } }
        _standings.update { current -> current.filter { it.tournamentId != tournamentId } }
        _announcements.update { current -> current.filter { it.tournamentId != tournamentId } }
    }

    override suspend fun getTournamentByInviteCode(code: String): Tournament? =
        _tournaments.value.find { it.inviteCode == code }

    // ── Teams ─────────────────────────────────────────────────────────────────

    override fun observeTeams(tournamentId: String): Flow<List<Team>> =
        _teams.asStateFlow().map { list -> list.filter { it.tournamentId == tournamentId } }

    override suspend fun saveTeams(teams: List<Team>) {
        _teams.update { current ->
            val ids = teams.map { it.id }.toSet()
            current.filter { it.id !in ids } + teams
        }
    }

    override suspend fun saveTeam(team: Team) {
        _teams.update { current -> current.filter { it.id != team.id } + team }
    }

    override suspend fun deleteTeam(teamId: String) {
        _teams.update { current -> current.filter { it.id != teamId } }
    }

    // ── Rounds ────────────────────────────────────────────────────────────────

    override suspend fun saveRounds(rounds: List<Round>) = Unit // rounds not observed directly

    // ── Matches ───────────────────────────────────────────────────────────────

    override fun observeMatches(tournamentId: String): Flow<List<Match>> =
        _matches.asStateFlow().map { list -> list.filter { it.tournamentId == tournamentId } }

    override suspend fun saveMatches(matches: List<Match>) {
        _matches.update { current ->
            val ids = matches.map { it.id }.toSet()
            current.filter { it.id !in ids } + matches
        }
    }

    override suspend fun saveMatch(match: Match) {
        _matches.update { current -> current.filter { it.id != match.id } + match }
    }

    override suspend fun getMatch(matchId: String): Match? =
        _matches.value.find { it.id == matchId }

    // ── Standings ─────────────────────────────────────────────────────────────

    override fun observeStandings(tournamentId: String): Flow<List<Standing>> =
        _standings.asStateFlow().map { list -> list.filter { it.tournamentId == tournamentId } }

    override suspend fun saveStandings(standings: List<Standing>) {
        _standings.update { current ->
            val ids = standings.map { it.id }.toSet()
            current.filter { it.id !in ids } + standings
        }
    }

    // ── Announcements ─────────────────────────────────────────────────────────

    override fun observeAnnouncements(tournamentId: String): Flow<List<Announcement>> =
        _announcements.asStateFlow().map { list -> list.filter { it.tournamentId == tournamentId } }

    override suspend fun saveAnnouncements(announcements: List<Announcement>) {
        _announcements.update { current ->
            val ids = announcements.map { it.id }.toSet()
            current.filter { it.id !in ids } + announcements
        }
    }
}