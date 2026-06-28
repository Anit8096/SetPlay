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

    private val tournaments   = MutableStateFlow<List<Tournament>>(emptyList())
    private val teams         = MutableStateFlow<List<Team>>(emptyList())
    private val matches       = MutableStateFlow<List<Match>>(emptyList())
    private val standings     = MutableStateFlow<List<Standing>>(emptyList())
    private val announcements = MutableStateFlow<List<Announcement>>(emptyList())

    // ── Tournaments ───────────────────────────────────────────────────────────

    override fun observeMyTournaments(userId: String): Flow<List<Tournament>> =
        tournaments.asStateFlow().map { list -> list.filter { it.createdBy == userId } }

    override fun observeTournament(tournamentId: String): Flow<Tournament?> =
        tournaments.asStateFlow().map { list -> list.find { it.id == tournamentId } }

    override suspend fun saveTournaments(list: List<Tournament>) {
        tournaments.update { current ->
            val ids = list.map { it.id }.toSet()
            current.filter { it.id !in ids } + list
        }
    }

    override suspend fun saveTournament(tournament: Tournament) {
        tournaments.update { current ->
            current.filter { it.id != tournament.id } + tournament
        }
    }

    override suspend fun deleteTournament(tournamentId: String) {
        tournaments.update { current -> current.filter { it.id != tournamentId } }
    }

    override suspend fun getTournamentByInviteCode(code: String): Tournament? =
        tournaments.value.find { it.inviteCode == code }

    // ── Teams ─────────────────────────────────────────────────────────────────

    override fun observeTeams(tournamentId: String): Flow<List<Team>> =
        teams.asStateFlow().map { list -> list.filter { it.tournamentId == tournamentId } }

    override suspend fun saveTeams(list: List<Team>) {
        teams.update { current ->
            val ids = list.map { it.id }.toSet()
            current.filter { it.id !in ids } + list
        }
    }

    override suspend fun saveTeam(team: Team) {
        teams.update { current -> current.filter { it.id != team.id } + team }
    }

    // ── Rounds ────────────────────────────────────────────────────────────────

    override suspend fun saveRounds(rounds: List<Round>) = Unit // rounds not observed directly

    // ── Matches ───────────────────────────────────────────────────────────────

    override fun observeMatches(tournamentId: String): Flow<List<Match>> =
        matches.asStateFlow().map { list -> list.filter { it.tournamentId == tournamentId } }

    override suspend fun saveMatches(list: List<Match>) {
        matches.update { current ->
            val ids = list.map { it.id }.toSet()
            current.filter { it.id !in ids } + list
        }
    }

    override suspend fun saveMatch(match: Match) {
        matches.update { current -> current.filter { it.id != match.id } + match }
    }

    override suspend fun getMatch(matchId: String): Match? =
        matches.value.find { it.id == matchId }

    // ── Standings ─────────────────────────────────────────────────────────────

    override fun observeStandings(tournamentId: String): Flow<List<Standing>> =
        standings.asStateFlow().map { list -> list.filter { it.tournamentId == tournamentId } }

    override suspend fun saveStandings(list: List<Standing>) {
        standings.update { current ->
            val ids = list.map { it.id }.toSet()
            current.filter { it.id !in ids } + list
        }
    }

    // ── Announcements ─────────────────────────────────────────────────────────

    override fun observeAnnouncements(tournamentId: String): Flow<List<Announcement>> =
        announcements.asStateFlow().map { list -> list.filter { it.tournamentId == tournamentId } }

    override suspend fun saveAnnouncements(list: List<Announcement>) {
        announcements.update { current ->
            val ids = list.map { it.id }.toSet()
            current.filter { it.id !in ids } + list
        }
    }
}