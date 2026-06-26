package com.kmp.setplay.data.local

import com.kmp.setplay.data.local.dao.AnnouncementDao
import com.kmp.setplay.data.local.dao.MatchDao
import com.kmp.setplay.data.local.dao.RoundDao
import com.kmp.setplay.data.local.dao.StandingDao
import com.kmp.setplay.data.local.dao.TeamDao
import com.kmp.setplay.data.local.dao.TournamentDao
import com.kmp.setplay.data.local.entity.toDomain
import com.kmp.setplay.data.local.entity.toEntity
import com.kmp.setplay.domain.model.Announcement
import com.kmp.setplay.domain.model.Match
import com.kmp.setplay.domain.model.Round
import com.kmp.setplay.domain.model.Standing
import com.kmp.setplay.domain.model.Team
import com.kmp.setplay.domain.model.Tournament
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomLocalCache(
    private val tournamentDao: TournamentDao,
    private val teamDao: TeamDao,
    private val roundDao: RoundDao,
    private val matchDao: MatchDao,
    private val standingDao: StandingDao,
    private val announcementDao: AnnouncementDao
) : LocalCache {

    // ── Tournaments ───────────────────────────────────────────────────────────

    override fun observeMyTournaments(userId: String): Flow<List<Tournament>> =
        tournamentDao.observeMyTournaments(userId).map { it.map { e -> e.toDomain() } }

    override fun observeTournament(tournamentId: String): Flow<Tournament?> =
        tournamentDao.observeTournament(tournamentId).map { it?.toDomain() }

    override suspend fun saveTournaments(tournaments: List<Tournament>) =
        tournamentDao.upsertAll(tournaments.map { it.toEntity() })

    override suspend fun saveTournament(tournament: Tournament) =
        tournamentDao.upsert(tournament.toEntity())

    override suspend fun deleteTournament(tournamentId: String) =
        tournamentDao.deleteById(tournamentId)

    override suspend fun getTournamentByInviteCode(code: String): Tournament? =
        tournamentDao.getTournamentByInviteCode(code)?.toDomain()

    // ── Teams ─────────────────────────────────────────────────────────────────

    override fun observeTeams(tournamentId: String): Flow<List<Team>> =
        teamDao.observeTeams(tournamentId).map { it.map { e -> e.toDomain() } }

    override suspend fun saveTeams(teams: List<Team>) =
        teamDao.upsertAll(teams.map { it.toEntity() })

    override suspend fun saveTeam(team: Team) =
        teamDao.upsert(team.toEntity())

    // ── Rounds ────────────────────────────────────────────────────────────────

    override suspend fun saveRounds(rounds: List<Round>) =
        roundDao.upsertAll(rounds.map { it.toEntity() })

    // ── Matches ───────────────────────────────────────────────────────────────

    override fun observeMatches(tournamentId: String): Flow<List<Match>> =
        matchDao.observeMatches(tournamentId).map { it.map { e -> e.toDomain() } }

    override suspend fun saveMatches(matches: List<Match>) =
        matchDao.upsertAll(matches.map { it.toEntity() })

    override suspend fun saveMatch(match: Match) =
        matchDao.upsert(match.toEntity())

    override suspend fun getMatch(matchId: String): Match? =
        matchDao.getMatchById(matchId)?.toDomain()

    // ── Standings ─────────────────────────────────────────────────────────────

    override fun observeStandings(tournamentId: String): Flow<List<Standing>> =
        standingDao.observeStandings(tournamentId).map { it.map { e -> e.toDomain() } }

    override suspend fun saveStandings(standings: List<Standing>) =
        standingDao.upsertAll(standings.map { it.toEntity() })

    // ── Announcements ─────────────────────────────────────────────────────────

    override fun observeAnnouncements(tournamentId: String): Flow<List<Announcement>> =
        announcementDao.observeAnnouncements(tournamentId).map { it.map { e -> e.toDomain() } }

    override suspend fun saveAnnouncements(announcements: List<Announcement>) =
        announcementDao.upsertAll(announcements.map { it.toEntity() })
}
