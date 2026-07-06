package com.kmp.setplay.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.kmp.setplay.data.local.entity.AnnouncementEntity
import com.kmp.setplay.data.local.entity.DeviceTokenEntity
import com.kmp.setplay.data.local.entity.MatchEntity
import com.kmp.setplay.data.local.entity.PlayerEntity
import com.kmp.setplay.data.local.entity.RoundEntity
import com.kmp.setplay.data.local.entity.StandingEntity
import com.kmp.setplay.data.local.entity.TeamEntity
import com.kmp.setplay.data.local.entity.TournamentEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room 3.0 KMP DAOs.
 * All functions are suspend or Flow — no blocking calls (rule 7).
 * Upsert is used for cache writes so remote updates overwrite local state cleanly.
 */

@Dao
interface TournamentDao {
    @Query("SELECT * FROM tournaments WHERE created_by = :userId ORDER BY created_at DESC")
    fun observeMyTournaments(userId: String): Flow<List<TournamentEntity>>

    @Query("SELECT * FROM tournaments WHERE id = :id")
    fun observeTournament(id: String): Flow<TournamentEntity?>

    @Query("SELECT * FROM tournaments WHERE invite_code = :code LIMIT 1")
    suspend fun getTournamentByInviteCode(code: String): TournamentEntity?

    @Upsert
    suspend fun upsertAll(tournaments: List<TournamentEntity>)

    @Upsert
    suspend fun upsert(tournament: TournamentEntity)

    @Query("DELETE FROM tournaments WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface TeamDao {
    @Query("SELECT * FROM teams WHERE tournament_id = :tournamentId ORDER BY seed ASC")
    fun observeTeams(tournamentId: String): Flow<List<TeamEntity>>

    @Query("SELECT * FROM teams WHERE id = :id")
    suspend fun getTeamById(id: String): TeamEntity?

    @Upsert
    suspend fun upsertAll(teams: List<TeamEntity>)

    @Upsert
    suspend fun upsert(team: TeamEntity)

    @Query("DELETE FROM teams WHERE tournament_id = :tournamentId")
    suspend fun deleteByTournament(tournamentId: String)

    @Query("DELETE FROM teams WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players WHERE team_id = :teamId")
    fun observePlayers(teamId: String): Flow<List<PlayerEntity>>

    @Upsert
    suspend fun upsertAll(players: List<PlayerEntity>)

    @Query("DELETE FROM players WHERE team_id = :teamId")
    suspend fun deleteByTeam(teamId: String)
}

@Dao
interface RoundDao {
    @Query("SELECT * FROM rounds WHERE tournament_id = :tournamentId ORDER BY round_number ASC")
    fun observeRounds(tournamentId: String): Flow<List<RoundEntity>>

    @Upsert
    suspend fun upsertAll(rounds: List<RoundEntity>)

    @Query("DELETE FROM rounds WHERE tournament_id = :tournamentId")
    suspend fun deleteByTournament(tournamentId: String)
}

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches WHERE tournament_id = :tournamentId ORDER BY match_number ASC")
    fun observeMatches(tournamentId: String): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE round_id = :roundId ORDER BY match_number ASC")
    fun observeMatchesByRound(roundId: String): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun getMatchById(id: String): MatchEntity?

    @Upsert
    suspend fun upsertAll(matches: List<MatchEntity>)

    @Upsert
    suspend fun upsert(match: MatchEntity)

    @Query("DELETE FROM matches WHERE tournament_id = :tournamentId")
    suspend fun deleteByTournament(tournamentId: String)
}

@Dao
interface StandingDao {
    @Query("SELECT * FROM standings WHERE tournament_id = :tournamentId ORDER BY points DESC, wins DESC")
    fun observeStandings(tournamentId: String): Flow<List<StandingEntity>>

    @Upsert
    suspend fun upsertAll(standings: List<StandingEntity>)

    @Query("DELETE FROM standings WHERE tournament_id = :tournamentId")
    suspend fun deleteByTournament(tournamentId: String)
}

@Dao
interface AnnouncementDao {
    @Query("SELECT * FROM announcements WHERE tournament_id = :tournamentId ORDER BY created_at DESC")
    fun observeAnnouncements(tournamentId: String): Flow<List<AnnouncementEntity>>

    @Upsert
    suspend fun upsertAll(announcements: List<AnnouncementEntity>)

    @Query("DELETE FROM announcements WHERE tournament_id = :tournamentId")
    suspend fun deleteByTournament(tournamentId: String)
}

@Dao
interface DeviceTokenDao {
    @Query("SELECT * FROM device_tokens WHERE user_id = :userId")
    suspend fun getTokensForUser(userId: String): List<DeviceTokenEntity>

    @Upsert
    suspend fun upsert(token: DeviceTokenEntity)

    @Query("DELETE FROM device_tokens WHERE token = :token")
    suspend fun deleteByToken(token: String)
}