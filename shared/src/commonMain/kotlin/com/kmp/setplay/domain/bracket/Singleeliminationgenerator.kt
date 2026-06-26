package com.kmp.setplay.domain.bracket

import com.kmp.setplay.domain.model.Match
import com.kmp.setplay.domain.model.MatchStatus
import com.kmp.setplay.domain.model.Round
import com.kmp.setplay.domain.model.Team

/**
 * Single Elimination bracket generator.
 *
 * Rules:
 * - Teams are seeded (1 vs last, 2 vs second-last) if seeds are present,
 *   otherwise random pairing.
 * - Bracket size is rounded up to the next power of 2 — excess slots are BYEs.
 * - next_match_id links are set so the repository can advance winners automatically.
 * - Pure functions — no side effects, no suspend. The repository calls these
 *   and persists the results.
 */
object SingleEliminationGenerator {

    data class BracketResult(
        val rounds: List<Round>,
        val matches: List<Match>
    )

    fun generate(
        tournamentId: String,
        teams: List<Team>,
        idGenerator: () -> String
    ): BracketResult {
        require(teams.size >= 2) { "Need at least 2 teams" }

        val bracketSize = nextPowerOfTwo(teams.size)
        val sortedTeams = sortTeams(teams, bracketSize)

        val rounds = mutableListOf<Round>()
        val matches = mutableListOf<Match>()

        // ── Round 1 ───────────────────────────────────────────────────────────
        val round1Id = idGenerator()
        val totalRounds = log2(bracketSize)

        rounds.add(Round(
            id = round1Id,
            tournamentId = tournamentId,
            roundNumber = 1,
            name = getRoundName(1, totalRounds)
        ))

        // Build first round matches — pair teams[0] vs teams[n-1], etc.
        val round1Matches = mutableListOf<Match>()
        for (i in 0 until bracketSize / 2) {
            val team1 = sortedTeams.getOrNull(i)
            val team2 = sortedTeams.getOrNull(bracketSize - 1 - i)
            val isBye = team2 == null

            round1Matches.add(
                Match(
                    id = idGenerator(),
                    roundId = round1Id,
                    tournamentId = tournamentId,
                    matchNumber = i + 1,
                    team1Id = team1?.id,
                    team2Id = team2?.id,
                    score1 = null,
                    score2 = null,
                    winnerId = if (isBye) team1?.id else null,
                    loserId = null,
                    status = if (isBye) MatchStatus.BYE else MatchStatus.SCHEDULED,
                    nextMatchId = null, // linked after all rounds are created
                    nextLoserMatchId = null
                )
            )
        }
        matches.addAll(round1Matches)

        // ── Subsequent rounds ─────────────────────────────────────────────────
        var previousRoundMatches = round1Matches
        for (roundNum in 2..totalRounds) {
            val roundId = idGenerator()
            val roundMatchCount = previousRoundMatches.size / 2

            rounds.add(Round(
                id = roundId,
                tournamentId = tournamentId,
                roundNumber = roundNum,
                name = getRoundName(roundNum, totalRounds)
            ))

            val roundMatches = mutableListOf<Match>()
            for (i in 0 until roundMatchCount) {
                roundMatches.add(
                    Match(
                        id = idGenerator(),
                        roundId = roundId,
                        tournamentId = tournamentId,
                        matchNumber = i + 1,
                        team1Id = null,
                        team2Id = null,
                        score1 = null,
                        score2 = null,
                        winnerId = null,
                        loserId = null,
                        status = MatchStatus.SCHEDULED,
                        nextMatchId = null,
                        nextLoserMatchId = null
                    )
                )
            }

            // Link previous round matches to current round matches
            for (i in previousRoundMatches.indices) {
                val nextMatch = roundMatches[i / 2]
                val prevMatch = previousRoundMatches[i]
                // Update nextMatchId on the previous round's match
                val idx = matches.indexOfFirst { it.id == prevMatch.id }
                if (idx != -1) {
                    matches[idx] = matches[idx].copy(nextMatchId = nextMatch.id)
                }
            }

            matches.addAll(roundMatches)
            previousRoundMatches = roundMatches
        }

        return BracketResult(rounds = rounds, matches = matches)
    }

    /**
     * Call this when a match result is submitted to advance the winner.
     * Returns the updated match and the next match with the winner slotted in.
     */
    fun advanceWinner(
        completedMatch: Match,
        allMatches: List<Match>
    ): List<Match> {
        val winnerId = completedMatch.winnerId ?: return emptyList()
        val nextMatch = allMatches.firstOrNull {
            it.id == completedMatch.nextMatchId
        } ?: return emptyList()

        // Slot winner into team1 or team2 of the next match
        val updated = if (nextMatch.team1Id == null) {
            nextMatch.copy(team1Id = winnerId)
        } else {
            nextMatch.copy(team2Id = winnerId)
        }

        return listOf(updated)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun sortTeams(teams: List<Team>, bracketSize: Int): List<Team?> {
        val seeded = teams.sortedWith(compareBy(nullsLast()) { it.seed })
        // Pad with nulls for BYEs
        return seeded + List(bracketSize - seeded.size) { null }
    }

    private fun nextPowerOfTwo(n: Int): Int {
        var power = 1
        while (power < n) power *= 2
        return power
    }

    private fun log2(n: Int): Int {
        var count = 0
        var value = n
        while (value > 1) {
            value /= 2
            count++
        }
        return count
    }

    private fun getRoundName(roundNumber: Int, totalRounds: Int): String {
        val roundsFromEnd = totalRounds - roundNumber
        return when (roundsFromEnd) {
            0 -> "Final"
            1 -> "Semi-Finals"
            2 -> "Quarter-Finals"
            else -> "Round $roundNumber"
        }
    }
}