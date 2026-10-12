package za.co.football;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link StandingsCalculator}.
 *
 * <p>Each test builds a small set of {@link MatchResult} objects, feeds them
 * to {@link StandingsCalculator#calculate}, and asserts specific properties of
 * the returned list. No file I/O is involved — this is pure logic testing.
 *
 * <p>Tests are grouped by concern using {@code @Nested} classes:
 * <ul>
 *   <li>Input validation (null collection, null entries, empty input)</li>
 *   <li>Statistics accumulation (home/away stats, multiple matches)</li>
 *   <li>Ranking (points, goal average, goals scored, alphabetical fallback)</li>
 *   <li>Edge cases (zero goals against, single team pair, one match)</li>
 * </ul>
 */
@DisplayName("StandingsCalculator")
class StandingsCalculatorTest {

    // -------------------------------------------------------------------------
    // Helper: find a team's standing by name, failing clearly if not found
    // -------------------------------------------------------------------------

    private static TeamStanding findTeam(List<TeamStanding> standings, String name) {
        return standings.stream()
                .filter(s -> s.getTeamName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Team \"" + name + "\" not found in standings: " + standings));
    }

    // -------------------------------------------------------------------------
    // Input validation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("input validation")
    class InputValidation {

        @Test
        @DisplayName("null collection throws IllegalArgumentException")
        void nullCollectionIsRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> StandingsCalculator.calculate(null));
        }

        @Test
        @DisplayName("collection containing a null entry throws IllegalArgumentException")
        void nullEntryInCollectionIsRejected() {
            List<MatchResult> withNull = new ArrayList<>();
            withNull.add(new MatchResult("Arsenal", "Chelsea", 1, 0));
            withNull.add(null);

            assertThrows(IllegalArgumentException.class,
                    () -> StandingsCalculator.calculate(withNull));
        }

        @Test
        @DisplayName("empty collection returns an empty list")
        void emptyCollectionReturnsEmptyList() {
            List<TeamStanding> result = StandingsCalculator.calculate(List.of());
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returned list is unmodifiable")
        void returnedListIsUnmodifiable() {
            List<MatchResult> matches = List.of(
                    new MatchResult("Arsenal", "Chelsea", 1, 0));

            List<TeamStanding> result = StandingsCalculator.calculate(matches);

            assertThrows(UnsupportedOperationException.class,
                    () -> result.add(new TeamStanding("Intruder")),
                    "Callers must not be able to modify the returned standings list");
        }

        @Test
        @DisplayName("returned TeamStanding objects are frozen — update() throws")
        void returnedStandingsAreFrozen() {
            List<MatchResult> matches = List.of(
                    new MatchResult("Arsenal", "Chelsea", 1, 0));

            List<TeamStanding> result = StandingsCalculator.calculate(matches);
            TeamStanding standing = result.get(0);

            assertThrows(IllegalStateException.class,
                    () -> standing.update(5, 0),
                    "A standing returned by calculate() must be frozen — update() must throw");
        }
    }

    // -------------------------------------------------------------------------
    // Statistics accumulation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("statistics accumulation")
    class StatisticsAccumulation {

        @Test
        @DisplayName("one match: home win — correct stats for both teams")
        void oneMatchHomeWin() {
            // Arsenal beat Chelsea 2-1 at home.
            // Arsenal: W=1, GF=2, GA=1, Pts=2
            // Chelsea: L=1, GF=1, GA=2, Pts=0
            List<MatchResult> matches = List.of(
                    new MatchResult("Arsenal", "Chelsea", 2, 1));

            List<TeamStanding> standings = StandingsCalculator.calculate(matches);
            assertEquals(2, standings.size());

            TeamStanding arsenal = findTeam(standings, "Arsenal");
            assertEquals(1, arsenal.getPlayed());
            assertEquals(1, arsenal.getWon());
            assertEquals(0, arsenal.getDrawn());
            assertEquals(0, arsenal.getLost());
            assertEquals(2, arsenal.getGoalsFor());
            assertEquals(1, arsenal.getGoalsAgainst());
            assertEquals(2, arsenal.getPoints(), "Win must give 2 points (1974/75 rules)");

            TeamStanding chelsea = findTeam(standings, "Chelsea");
            assertEquals(1, chelsea.getPlayed());
            assertEquals(0, chelsea.getWon());
            assertEquals(0, chelsea.getDrawn());
            assertEquals(1, chelsea.getLost());
            assertEquals(1, chelsea.getGoalsFor());
            assertEquals(2, chelsea.getGoalsAgainst());
            assertEquals(0, chelsea.getPoints());
        }

        @Test
        @DisplayName("one match: away win — away team stats are correctly updated")
        void oneMatchAwayWin() {
            // Liverpool win 0-3 away at Arsenal.
            // Arsenal (home): L=1, GF=0, GA=3, Pts=0
            // Liverpool (away): W=1, GF=3, GA=0, Pts=2
            List<MatchResult> matches = List.of(
                    new MatchResult("Arsenal", "Liverpool", 0, 3));

            List<TeamStanding> standings = StandingsCalculator.calculate(matches);

            TeamStanding liverpool = findTeam(standings, "Liverpool");
            assertEquals(1, liverpool.getWon());
            assertEquals(3, liverpool.getGoalsFor());
            assertEquals(0, liverpool.getGoalsAgainst());
            assertEquals(2, liverpool.getPoints());

            TeamStanding arsenal = findTeam(standings, "Arsenal");
            assertEquals(1, arsenal.getLost());
            assertEquals(0, arsenal.getGoalsFor());
            assertEquals(3, arsenal.getGoalsAgainst());
            assertEquals(0, arsenal.getPoints());
        }

        @Test
        @DisplayName("one match: draw — both teams get 1 point")
        void oneMatchDraw() {
            List<MatchResult> matches = List.of(
                    new MatchResult("Leeds United", "Liverpool", 1, 1));

            List<TeamStanding> standings = StandingsCalculator.calculate(matches);

            TeamStanding leeds = findTeam(standings, "Leeds United");
            assertEquals(1, leeds.getDrawn());
            assertEquals(1, leeds.getPoints());

            TeamStanding liverpool = findTeam(standings, "Liverpool");
            assertEquals(1, liverpool.getDrawn());
            assertEquals(1, liverpool.getPoints());
        }

        @Test
        @DisplayName("each team appears exactly once, regardless of match count")
        void eachTeamAppearsExactlyOnce() {
            // Three matches, three teams — each team plays twice.
            List<MatchResult> matches = List.of(
                    new MatchResult("Arsenal",  "Chelsea",       2, 1),
                    new MatchResult("Chelsea",  "Liverpool",     0, 0),
                    new MatchResult("Liverpool","Arsenal",       1, 3));

            List<TeamStanding> standings = StandingsCalculator.calculate(matches);
            assertEquals(3, standings.size(), "Each team appears exactly once");
        }

        @Test
        @DisplayName("team statistics accumulate correctly across multiple matches")
        void statsAccumulateAcrossMultipleMatches() {
            // Arsenal plays three matches:
            //   1. vs Chelsea (home) 2-1: win
            //   2. vs Liverpool (away, Arsenal is away) — match is Liverpool 0 Arsenal 3: win
            //   3. vs Leeds (home) 1-1: draw
            // Arsenal totals: P=3, W=2, D=1, L=0, GF=6, GA=2, Pts=5
            List<MatchResult> matches = List.of(
                    new MatchResult("Arsenal",  "Chelsea",   2, 1),  // Arsenal home win
                    new MatchResult("Liverpool","Arsenal",   0, 3),  // Arsenal away win
                    new MatchResult("Arsenal",  "Leeds United", 1, 1)  // draw
            );

            List<TeamStanding> standings = StandingsCalculator.calculate(matches);
            TeamStanding arsenal = findTeam(standings, "Arsenal");

            assertEquals(3, arsenal.getPlayed());
            assertEquals(2, arsenal.getWon());
            assertEquals(1, arsenal.getDrawn());
            assertEquals(0, arsenal.getLost());
            assertEquals(6, arsenal.getGoalsFor());
            assertEquals(2, arsenal.getGoalsAgainst());
            assertEquals(5, arsenal.getPoints()); // 2+2+1
        }

        @Test
        @DisplayName("total number of teams equals distinct teams across all matches")
        void numberOfTeamsIsDistinctCount() {
            // Four teams, each playing once.
            List<MatchResult> matches = List.of(
                    new MatchResult("Arsenal",  "Chelsea",    1, 0),
                    new MatchResult("Liverpool","Leeds United",2, 2)
            );

            List<TeamStanding> standings = StandingsCalculator.calculate(matches);
            assertEquals(4, standings.size());
        }
    }

    // -------------------------------------------------------------------------
    // Ranking — points
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("ranking by points")
    class RankingByPoints {

        @Test
        @DisplayName("team with more points ranks higher")
        void higherPointsRanksFirst() {
            // Arsenal win (2 pts), Chelsea lose (0 pts).
            List<MatchResult> matches = List.of(
                    new MatchResult("Arsenal", "Chelsea", 1, 0));

            List<TeamStanding> standings = StandingsCalculator.calculate(matches);

            assertEquals("Arsenal", standings.get(0).getTeamName(),
                    "Arsenal won and should be top of the table");
            assertEquals("Chelsea", standings.get(1).getTeamName());
        }

        @Test
        @DisplayName("three-team standings ordered by points")
        void threeTeamsOrderedByPoints() {
            // Arsenal 2 wins (4 pts), Chelsea 1 win (2 pts), Liverpool 0 pts.
            // Matches:
            //   Arsenal 2-0 Chelsea  → Arsenal W, Chelsea L
            //   Arsenal 1-0 Liverpool → Arsenal W, Liverpool L
            //   Chelsea 1-0 Liverpool → Chelsea W, Liverpool L  (but Liverpool already 0)
            // After: Arsenal=4, Chelsea=2, Liverpool=0
            List<MatchResult> matches = List.of(
                    new MatchResult("Arsenal",   "Chelsea",   2, 0),
                    new MatchResult("Arsenal",   "Liverpool", 1, 0),
                    new MatchResult("Chelsea",   "Liverpool", 1, 0)
            );

            List<TeamStanding> standings = StandingsCalculator.calculate(matches);

            assertEquals("Arsenal",   standings.get(0).getTeamName());
            assertEquals("Chelsea",   standings.get(1).getTeamName());
            assertEquals("Liverpool", standings.get(2).getTeamName());
        }
    }

    // -------------------------------------------------------------------------
    // Ranking — goal average tiebreaker
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("ranking by goal average (tiebreaker)")
    class RankingByGoalAverage {

        @Test
        @DisplayName("equal points: higher goal average ranks first")
        void higherGoalAverageRanksFirst() {
            // Both Arsenal and Chelsea have 2 points (one win each).
            // Arsenal beat Liverpool 3-0: GA = 3.0 (infinity, GA=0... but let's use
            // a match that gives a finite GA for this test)
            //
            // Three teams:
            //   Arsenal  beat  Coventry 4-1  (GF=4, GA=1, avg=4.0, pts=2)
            //   Chelsea  beat  Coventry 2-1  — but we need separate opponents.
            //
            // Setup (each pair plays exactly once):
            //   Arsenal  beat  Liverpool 4-1  → Arsenal: GF=4, GA=1, avg=4.0, pts=2
            //   Chelsea  beat  Leeds United 2-1 → Chelsea: GF=2, GA=1, avg=2.0, pts=2
            //   Liverpool lost, Leeds lost — both 0 pts
            List<MatchResult> matches = List.of(
                    new MatchResult("Arsenal",    "Liverpool",   4, 1),
                    new MatchResult("Chelsea",    "Leeds United", 2, 1)
            );

            List<TeamStanding> standings = StandingsCalculator.calculate(matches);

            // Arsenal and Chelsea are both on 2 points; Arsenal's avg (4.0) > Chelsea's (2.0)
            int arsenalPos = standings.indexOf(findTeam(standings, "Arsenal"));
            int chelseaPos = standings.indexOf(findTeam(standings, "Chelsea"));
            assertTrue(arsenalPos < chelseaPos,
                    "Arsenal (goal avg 4.0) should rank above Chelsea (goal avg 2.0) " +
                    "when both are on 2 points");
        }

        @Test
        @DisplayName("goal average is GF/GA — not goal difference")
        void goalAverageIsRatioNotDifference() {
            // Demonstrates why the formula matters:
            // Team A: GF=6, GA=3 → average=2.0, difference=3
            // Team B: GF=4, GA=1 → average=4.0, difference=3
            // Goal difference ties them; goal average separates them (B ranks higher).
            //
            // We set up two independent pairs of matches so each team has exactly
            // the stats above after one match each.
            // TeamB beats Opponent2 4-1.
            // TeamA beats Opponent1 6-3.
            List<MatchResult> matches = List.of(
                    new MatchResult("Team A", "Opponent1", 6, 3),  // avg=2.0, diff=+3
                    new MatchResult("Team B", "Opponent2", 4, 1)   // avg=4.0, diff=+3
            );

            List<TeamStanding> standings = StandingsCalculator.calculate(matches);

            // Both on 2 pts. Same goal difference (+3). But B has higher goal average.
            TeamStanding a = findTeam(standings, "Team A");
            TeamStanding b = findTeam(standings, "Team B");
            assertEquals(a.getGoalsFor() - a.getGoalsAgainst(),
                         b.getGoalsFor() - b.getGoalsAgainst(),
                    "Goal difference is the same for both teams (+3)");
            assertTrue(b.goalAverage() > a.goalAverage(),
                    "Team B's goal average (4.0) must be higher than Team A's (2.0)");

            int posA = standings.indexOf(a);
            int posB = standings.indexOf(b);
            assertTrue(posB < posA,
                    "Team B must rank higher because goal average, not goal difference, is used");
        }
    }

    // -------------------------------------------------------------------------
    // Ranking — goals scored tiebreaker
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("ranking by goals scored (second tiebreaker)")
    class RankingByGoalsScored {

        @Test
        @DisplayName("equal points and goal average: more goals scored ranks first")
        void moreGoalsScoredRanksFirst() {
            // Arsenal and Chelsea are both on 2 points.
            // Same goal average (both 3/1 = 3.0... no: we need same avg, different GF).
            // Same avg means same GF/GA ratio. E.g. both 2/1 = 2.0 but we need
            // different GF totals. That's impossible with the same ratio and integer goals
            // in a single match. Use two matches each:
            //
            // Arsenal: wins 4-2 and 2-1 → GF=6, GA=3, avg=2.0, pts=4
            // Chelsea: wins 4-2 and 2-1 → same... we need them on same points too.
            //
            // Simpler: both teams win one match, same avg, different GF.
            // Same avg with one match each: both need GF/GA equal.
            // Arsenal beats Opp1 by 4-2 (avg=2.0, gf=4, pts=2)
            // Chelsea beats Opp2 by 2-1 (avg=2.0, gf=2, pts=2)
            // Both avg=2.0, both pts=2 — Arsenal has more goals scored.
            List<MatchResult> matches = List.of(
                    new MatchResult("Arsenal", "Opponent1", 4, 2),  // avg=2.0, gf=4
                    new MatchResult("Chelsea", "Opponent2", 2, 1)   // avg=2.0, gf=2
            );

            List<TeamStanding> standings = StandingsCalculator.calculate(matches);

            TeamStanding arsenal = findTeam(standings, "Arsenal");
            TeamStanding chelsea = findTeam(standings, "Chelsea");

            assertEquals(arsenal.getPoints(), chelsea.getPoints(), "Same points");
            assertEquals(arsenal.goalAverage(), chelsea.goalAverage(), 1e-9, "Same goal average");
            assertTrue(arsenal.getGoalsFor() > chelsea.getGoalsFor(), "Arsenal scored more");

            int arsenalPos = standings.indexOf(arsenal);
            int chelseaPos = standings.indexOf(chelsea);
            assertTrue(arsenalPos < chelseaPos,
                    "Arsenal must rank above Chelsea because of more goals scored");
        }
    }

    // -------------------------------------------------------------------------
    // Ranking — alphabetical fallback
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("alphabetical fallback when all criteria are equal")
    class AlphabeticalFallback {

        @Test
        @DisplayName("equal points, goal average, and goals scored: alphabetical A before Z")
        void alphabeticalFallbackWhenAllCriteriaAreEqual() {
            // Both teams win 2-1. Identical points (2), identical avg (2.0),
            // identical goals scored (2). Zebra FC comes after Alpha FC.
            List<MatchResult> matches = List.of(
                    new MatchResult("Zebra FC", "Opponent1", 2, 1),
                    new MatchResult("Alpha FC", "Opponent2", 2, 1)
            );

            List<TeamStanding> standings = StandingsCalculator.calculate(matches);

            // Ignore losing teams — find the position of the two winning teams
            TeamStanding alpha = findTeam(standings, "Alpha FC");
            TeamStanding zebra = findTeam(standings, "Zebra FC");

            assertEquals(alpha.getPoints(), zebra.getPoints());
            assertEquals(alpha.goalAverage(), zebra.goalAverage(), 1e-9);
            assertEquals(alpha.getGoalsFor(), zebra.getGoalsFor());

            int alphaPos = standings.indexOf(alpha);
            int zebraPos = standings.indexOf(zebra);
            assertTrue(alphaPos < zebraPos,
                    "Alpha FC (A) must come before Zebra FC (Z) as the final fallback");
        }
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("edge cases")
    class EdgeCases {

        @Test
        @DisplayName("team with zero goals against: goal average is POSITIVE_INFINITY")
        void teamWithZeroGoalsAgainstHasInfiniteGoalAverage() {
            // Liverpool win 3-0. GA=0 → goalAverage() = POSITIVE_INFINITY.
            List<MatchResult> matches = List.of(
                    new MatchResult("Liverpool", "Arsenal", 3, 0));

            List<TeamStanding> standings = StandingsCalculator.calculate(matches);
            TeamStanding liverpool = findTeam(standings, "Liverpool");

            assertEquals(0, liverpool.getGoalsAgainst());
            assertTrue(Double.isInfinite(liverpool.goalAverage()),
                    "Team with GA=0 must have POSITIVE_INFINITY goal average");
        }

        @Test
        @DisplayName("team with zero goals against ranks above team with finite goal average")
        void infiniteGoalAverageRanksAboveFiniteGoalAverage() {
            // Both teams are on 2 points (one win each).
            // Liverpool win 1-0: GA=0 → avg=INFINITY
            // Arsenal win 5-1: GA=1 → avg=5.0  (very high, but finite)
            List<MatchResult> matches = List.of(
                    new MatchResult("Liverpool", "Opponent1", 1, 0),  // avg=∞
                    new MatchResult("Arsenal",   "Opponent2", 5, 1)   // avg=5.0
            );

            List<TeamStanding> standings = StandingsCalculator.calculate(matches);

            int liverpoolPos = standings.indexOf(findTeam(standings, "Liverpool"));
            int arsenalPos   = standings.indexOf(findTeam(standings, "Arsenal"));

            assertTrue(liverpoolPos < arsenalPos,
                    "Liverpool (avg=∞) must rank above Arsenal (avg=5.0) despite the high value");
        }

        @Test
        @DisplayName("two teams with zero goals against: goal average tie broken by goals scored")
        void twoTeamsWithZeroGoalsAgainstTiebrokenByGoalsScored() {
            // Liverpool win 3-0 (GF=3, GA=0, avg=∞, pts=2)
            // Arsenal  win 1-0 (GF=1, GA=0, avg=∞, pts=2)
            // Same points, same avg (both ∞) — goals scored (3 vs 1) breaks the tie.
            List<MatchResult> matches = List.of(
                    new MatchResult("Liverpool", "Opponent1", 3, 0),
                    new MatchResult("Arsenal",   "Opponent2", 1, 0)
            );

            List<TeamStanding> standings = StandingsCalculator.calculate(matches);

            int liverpoolPos = standings.indexOf(findTeam(standings, "Liverpool"));
            int arsenalPos   = standings.indexOf(findTeam(standings, "Arsenal"));

            assertTrue(liverpoolPos < arsenalPos,
                    "Liverpool (GF=3) must rank above Arsenal (GF=1) when both have avg=∞");
        }

        @Test
        @DisplayName("single match: exactly two teams appear in standings")
        void singleMatchProducesTwoTeams() {
            List<MatchResult> matches = List.of(
                    new MatchResult("Arsenal", "Chelsea", 0, 0));

            List<TeamStanding> standings = StandingsCalculator.calculate(matches);
            assertEquals(2, standings.size());
        }

        @Test
        @DisplayName("result list size equals the number of distinct teams across all matches")
        void listSizeEqualsDistinctTeamCount() {
            // Six teams, three matches.
            List<MatchResult> matches = List.of(
                    new MatchResult("A", "B", 1, 0),
                    new MatchResult("C", "D", 2, 2),
                    new MatchResult("E", "F", 0, 1)
            );
            assertEquals(6, StandingsCalculator.calculate(matches).size());
        }

        @Test
        @DisplayName("same team pair playing twice: stats accumulate, one entry per team")
        void sameTeamPairPlayingTwice() {
            // Arsenal vs Chelsea twice: one win each. Each ends with P=2, W=1, D=0, L=1.
            List<MatchResult> matches = List.of(
                    new MatchResult("Arsenal", "Chelsea", 2, 1),  // Arsenal win
                    new MatchResult("Arsenal", "Chelsea", 0, 1)   // Chelsea win
            );

            List<TeamStanding> standings = StandingsCalculator.calculate(matches);
            assertEquals(2, standings.size(), "Still exactly two teams");

            TeamStanding arsenal = findTeam(standings, "Arsenal");
            TeamStanding chelsea = findTeam(standings, "Chelsea");

            assertEquals(2, arsenal.getPlayed());
            assertEquals(1, arsenal.getWon());
            assertEquals(1, arsenal.getLost());

            assertEquals(2, chelsea.getPlayed());
            assertEquals(1, chelsea.getWon());
            assertEquals(1, chelsea.getLost());
        }
    }
}
