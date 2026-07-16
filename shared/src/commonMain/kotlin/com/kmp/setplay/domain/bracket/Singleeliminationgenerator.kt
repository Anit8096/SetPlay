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

    /** Mirrors presentation.tournament.create.SeedingMode without a cross-module dependency. */
    enum class Seeding { SEEDED, BLIND_DRAW, MANUAL }

    data class BracketResult(
        val rounds: List<Round>,
        val matches: List<Match>
    )

    fun generate(
        tournamentId: String,
        teams: List<Team>,
        idGenerator: () -> String,
        seeding: Seeding = Seeding.SEEDED,
        includeThirdPlace: Boolean = false
    ): BracketResult {
        require(teams.size >= 2) { "Need at least 2 teams" }

        val bracketSize = nextPowerOfTwo(teams.size)
        val sortedTeams = orderTeams(teams, bracketSize, seeding)

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

        // Build first round matches using canonical bracket placement. Pairing
        // adjacent canonical positions gives 1v16, 8v9, 4v13, 5v12, ... so seeds 1
        // and 2 sit in opposite halves and BYEs (the padded nulls at the tail of
        // sortedTeams) spread across the bracket instead of stacking at the top —
        // the old (i, n-1-i) pairing made seeds 1 and 2 collide in round 2.
        val positions = bracketPositions(bracketSize)
        val round1Matches = mutableListOf<Match>()
        for (i in 0 until bracketSize / 2) {
            // positions[] holds the better seed of each pair first, so team1 is
            // never null — BYEs (seeds beyond the real team count) land in team2.
            val team1 = sortedTeams.getOrNull(positions[2 * i] - 1)
            val team2 = sortedTeams.getOrNull(positions[2 * i + 1] - 1)
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
        var semiFinalMatches: List<Match> = emptyList()

        for (roundNum in 2..totalRounds) {
            val roundId = idGenerator()
            val roundMatchCount = previousRoundMatches.size / 2
            val isSemiFinalRound = roundMatchCount == 1 && totalRounds >= 2 && roundNum == totalRounds

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

            if (previousRoundMatches.size == 2) {
                // The round we just linked FROM was the semi-finals
                semiFinalMatches = previousRoundMatches
            }

            matches.addAll(roundMatches)
            previousRoundMatches = roundMatches
        }

        // ── Auto-advance round-1 BYE winners ──────────────────────────────────
        // A BYE match is decided at generation time, so its winner must be slotted
        // into the next round here — advanceWinner only runs on score submission,
        // and nothing ever "completes" a BYE, so without this pass every
        // bye-winner's round-2 slot stayed TBD forever.
        for (bye in matches.filter { it.status == MatchStatus.BYE && it.winnerId != null }) {
            val nextIdx = matches.indexOfFirst { it.id == bye.nextMatchId }
            if (nextIdx == -1) continue
            matches[nextIdx] = placeInto(matches[nextIdx], bye.winnerId!!, bye.matchNumber)
        }

        // ── 3rd place match ──────────────────────────────────────────────────
        // Only meaningful when there were semi-finals (bracket size >= 4)
        if (includeThirdPlace && semiFinalMatches.size == 2) {
            val thirdPlaceRoundId = idGenerator()
            rounds.add(Round(
                id = thirdPlaceRoundId,
                tournamentId = tournamentId,
                // totalRounds + 1, NOT totalRounds: rounds has a unique constraint on
                // (tournament_id, round_number), and the Final already owns totalRounds —
                // reusing it made every 3rd-place bracket insert fail with Postgres 23505.
                // Nothing reads roundNumber for bracket layout (that groups by roundId);
                // the only consumer is the Room DAO's ORDER BY, where sorting after the
                // Final is chronologically correct anyway.
                roundNumber = totalRounds + 1,
                name = "3rd Place"
            ))
            val thirdPlaceMatch = Match(
                id = idGenerator(),
                roundId = thirdPlaceRoundId,
                tournamentId = tournamentId,
                matchNumber = 1,
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
            matches.add(thirdPlaceMatch)

            // Slot each semi-final's loser into the 3rd place match once they lose
            semiFinalMatches.forEach { sf ->
                val idx = matches.indexOfFirst { it.id == sf.id }
                if (idx != -1) {
                    matches[idx] = matches[idx].copy(nextLoserMatchId = thirdPlaceMatch.id)
                }
            }
        }

        return BracketResult(rounds = rounds, matches = matches)
    }

    /**
     * Call this when a match result is submitted to advance the winner
     * (and, for semi-finals, slot the loser into the 3rd place match).
     */
    fun advanceWinner(
        completedMatch: Match,
        allMatches: List<Match>
    ): List<Match> {
        val updates = mutableListOf<Match>()

        val winnerId = completedMatch.winnerId
        if (winnerId != null) {
            val nextMatch = allMatches.firstOrNull { it.id == completedMatch.nextMatchId }
            if (nextMatch != null) {
                updates += placeInto(nextMatch, winnerId, completedMatch.matchNumber)
            }
        }

        val loserId = completedMatch.loserId
        if (loserId != null) {
            val loserMatch = allMatches.firstOrNull { it.id == completedMatch.nextLoserMatchId }
            if (loserMatch != null) {
                updates += placeInto(loserMatch, loserId, completedMatch.matchNumber)
            }
        }

        return updates
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Slots [teamId] into [target]: an odd feeder matchNumber feeds the top slot
     * (team1), an even one the bottom (team2) — matching how feeders are laid out
     * visually — with first-free-slot as the fallback so a legitimately filled
     * slot (e.g. a pre-advanced BYE winner) is never overwritten.
     */
    private fun placeInto(target: Match, teamId: String, feederMatchNumber: Int): Match {
        val preferTop = feederMatchNumber % 2 == 1
        return when {
            preferTop && target.team1Id == null -> target.copy(team1Id = teamId)
            !preferTop && target.team2Id == null -> target.copy(team2Id = teamId)
            target.team1Id == null -> target.copy(team1Id = teamId)
            else -> target.copy(team2Id = teamId)
        }
    }

    /**
     * Canonical single-elimination position order via the standard doubling
     * expansion: [1,2] -> [1,4,2,3] -> [1,8,4,5,2,7,3,6] -> ... Each element x of
     * a size-k list expands to [x, 2k+1-x]. Pairing adjacent entries of the final
     * list yields the classic bracket (for 16: 1v16, 8v9, 4v13, 5v12, 2v15, 7v10,
     * 3v14, 6v11).
     */
    private fun bracketPositions(bracketSize: Int): List<Int> {
        var positions = listOf(1, 2)
        while (positions.size < bracketSize) {
            val n = positions.size * 2
            positions = positions.flatMap { listOf(it, n + 1 - it) }
        }
        return positions
    }

    private fun orderTeams(teams: List<Team>, bracketSize: Int, seeding: Seeding): List<Team?> {
        val ordered = when (seeding) {
            Seeding.SEEDED     -> teams.sortedWith(compareBy(nullsLast()) { it.seed })
            Seeding.BLIND_DRAW -> teams.shuffled()
            Seeding.MANUAL     -> teams // insertion / manually-set order is preserved as-is
        }
        // Pad with nulls for BYEs
        return ordered + List(bracketSize - ordered.size) { null }
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