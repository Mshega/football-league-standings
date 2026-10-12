package za.co.football;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TeamStanding}.
 *
 * <p>Tests are grouped using {@code @Nested} classes so related scenarios sit
 * together. This keeps the test class navigable as coverage grows.
 *
 * <p>Key things verified here:
 * <ul>
 *   <li>The <strong>2-point win</strong> rule (not 3) — correct for 1974/75.</li>
 *   <li>Goal average = GF ÷ GA, not goal difference.</li>
 *   <li>Goal average when GA = 0 returns {@code POSITIVE_INFINITY}.</li>
 *   <li>Statistics accumulate correctly across several matches.</li>
 * </ul>
 */
@DisplayName("TeamStanding")
class TeamStandingTest {

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("new standing starts with all counters at zero")
        void freshStandingIsAllZero() {
            TeamStanding s = new TeamStanding("Arsenal");

            assertEquals("Arsenal", s.getTeamName());
            assertEquals(0, s.getPlayed());
            assertEquals(0, s.getWon());
            assertEquals(0, s.getDrawn());
            assertEquals(0, s.getLost());
            assertEquals(0, s.getGoalsFor());
            assertEquals(0, s.getGoalsAgainst());
            assertEquals(0, s.getPoints());
        }

        @Test
        @DisplayName("team name is trimmed on construction")
        void teamNameIsTrimmed() {
            TeamStanding s = new TeamStanding("  Leeds United  ");
            assertEquals("Leeds United", s.getTeamName());
        }

        @Test
        @DisplayName("null team name throws IllegalArgumentException")
        void nullNameIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> new TeamStanding(null));
        }

        @Test
        @DisplayName("blank team name throws IllegalArgumentException")
        void blankNameIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> new TeamStanding("   "));
        }
    }

    // -------------------------------------------------------------------------
    // Single-match outcomes
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("single match outcomes")
    class SingleMatchOutcomes {

        private TeamStanding standing;

        @BeforeEach
        void setUp() {
            standing = new TeamStanding("Arsenal");
        }

        @Test
        @DisplayName("win: played+1, won+1, points+2 — verifies 2-point win rule")
        void homeWinAwards2Points() {
            standing.update(3, 1);  // won 3-1

            assertEquals(1, standing.getPlayed());
            assertEquals(1, standing.getWon());
            assertEquals(0, standing.getDrawn());
            assertEquals(0, standing.getLost());
            assertEquals(3, standing.getGoalsFor());
            assertEquals(1, standing.getGoalsAgainst());
            // *** Critical: must be 2, not 3 — this is 1974/75, not modern rules ***
            assertEquals(2, standing.getPoints(),
                    "A win must award 2 points under 1974/75 Football League rules");
        }

        @Test
        @DisplayName("away win: same counters update correctly regardless of perspective")
        void awayWinFromOpponentPerspective() {
            // Suppose Arsenal lost 0-2: they scored 0, conceded 2
            standing.update(0, 2);

            assertEquals(1, standing.getPlayed());
            assertEquals(0, standing.getWon());
            assertEquals(0, standing.getDrawn());
            assertEquals(1, standing.getLost());
            assertEquals(0, standing.getGoalsFor());
            assertEquals(2, standing.getGoalsAgainst());
            assertEquals(0, standing.getPoints());
        }

        @Test
        @DisplayName("draw: played+1, drawn+1, points+1")
        void drawAwards1Point() {
            standing.update(1, 1);

            assertEquals(1, standing.getPlayed());
            assertEquals(0, standing.getWon());
            assertEquals(1, standing.getDrawn());
            assertEquals(0, standing.getLost());
            assertEquals(1, standing.getGoalsFor());
            assertEquals(1, standing.getGoalsAgainst());
            assertEquals(1, standing.getPoints());
        }

        @Test
        @DisplayName("goalless draw: valid and awards 1 point")
        void goallessDrawIsValid() {
            standing.update(0, 0);

            assertEquals(1, standing.getPlayed());
            assertEquals(1, standing.getDrawn());
            assertEquals(1, standing.getPoints());
            assertEquals(0, standing.getGoalsFor());
            assertEquals(0, standing.getGoalsAgainst());
        }

        @Test
        @DisplayName("loss: played+1, lost+1, points unchanged")
        void lossAwards0Points() {
            standing.update(0, 1);

            assertEquals(1, standing.getPlayed());
            assertEquals(0, standing.getWon());
            assertEquals(0, standing.getDrawn());
            assertEquals(1, standing.getLost());
            assertEquals(0, standing.getPoints());
        }

        @Test
        @DisplayName("negative goals scored throws IllegalArgumentException")
        void negativeGoalsScoredIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> standing.update(-1, 0));
        }

        @Test
        @DisplayName("negative goals conceded throws IllegalArgumentException")
        void negativeGoalsConcededIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> standing.update(0, -1));
        }
    }

    // -------------------------------------------------------------------------
    // Accumulated statistics across multiple matches
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("accumulated statistics across multiple matches")
    class AccumulatedStatistics {

        @Test
        @DisplayName("ten matches accumulate played, goals and points correctly")
        void tenMatchesAccumulateCorrectly() {
            TeamStanding s = new TeamStanding("Ipswich Town");

            // Simulate a 10-match sequence: W W W W W W W D L L
            s.update(2, 0);  // win
            s.update(3, 1);  // win
            s.update(1, 0);  // win
            s.update(2, 1);  // win
            s.update(4, 2);  // win
            s.update(1, 0);  // win
            s.update(3, 2);  // win
            s.update(1, 1);  // draw
            s.update(0, 2);  // loss
            s.update(1, 3);  // loss

            assertEquals(10, s.getPlayed());
            assertEquals(7,  s.getWon());
            assertEquals(1,  s.getDrawn());
            assertEquals(2,  s.getLost());

            // Goals: scored 2+3+1+2+4+1+3+1+0+1 = 18
            assertEquals(18, s.getGoalsFor());
            // Goals against: 0+1+0+1+2+0+2+1+2+3 = 12
            assertEquals(12, s.getGoalsAgainst());

            // Points: 7 wins × 2 + 1 draw × 1 + 2 losses × 0 = 15
            assertEquals(15, s.getPoints());
        }

        @Test
        @DisplayName("won + drawn + lost always equals played")
        void wdlSumEqualsPlayed() {
            TeamStanding s = new TeamStanding("Chelsea");
            s.update(2, 1);
            s.update(0, 0);
            s.update(1, 2);
            s.update(3, 0);

            assertEquals(s.getPlayed(), s.getWon() + s.getDrawn() + s.getLost());
        }

        @Test
        @DisplayName("points never exceed played × 2 under 2-point win system")
        void pointsNeverExceedTwicePlayedUnder2PointSystem() {
            TeamStanding s = new TeamStanding("Liverpool");
            s.update(5, 0);  // win
            s.update(4, 0);  // win
            s.update(3, 0);  // win

            assertTrue(s.getPoints() <= s.getPlayed() * TeamStanding.POINTS_FOR_WIN,
                    "Maximum possible points = played × 2 under the 1974/75 system");
            assertEquals(6, s.getPoints());
        }
    }

    // -------------------------------------------------------------------------
    // Goal average
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("goal average")
    class GoalAverageTests {

        @Test
        @DisplayName("goal average = goals for / goals against")
        void goalAverageIsCorrect() {
            TeamStanding s = new TeamStanding("Arsenal");
            s.update(3, 2); // GF=3, GA=2 → average = 1.5

            assertEquals(1.5, s.goalAverage(), 1e-9,
                    "Goal average must be GF ÷ GA, not goal difference");
        }

        @Test
        @DisplayName("goal average is not goal difference")
        void goalAverageIsNotGoalDifference() {
            TeamStanding s = new TeamStanding("Arsenal");
            s.update(4, 2); // GF=4, GA=2

            double average    = s.goalAverage();       // expected: 2.0
            double difference = s.getGoalsFor() - s.getGoalsAgainst(); // would be: 2.0 here too

            // Use a case where the two formulas diverge: GF=6, GA=3
            TeamStanding s2 = new TeamStanding("Chelsea");
            s2.update(6, 3); // average=2.0, difference=3.0 — different values

            assertNotEquals(s2.getGoalsFor() - s2.getGoalsAgainst(),
                            s2.goalAverage(),
                            "This confirms the two formulas produce different results; " +
                            "we use goal average, not difference");
        }

        @Test
        @DisplayName("goal average with GA = 0 returns POSITIVE_INFINITY (used for sorting)")
        void goalAverageWithZeroGoalsAgainstIsPositiveInfinity() {
            // Historical note: the Football League never formally defined this
            // case because no team ever conceded zero goals in a full season.
            // POSITIVE_INFINITY is the mathematically correct limit of GF/GA
            // as GA approaches zero, and it sorts the team above all others
            // with a finite goal average — which is the fairest interpretation.
            //
            // IMPORTANT: CsvStandingsWriter must NOT write "Infinity" to CSV.
            // It must format this as an empty string or explicit label instead.
            TeamStanding s = new TeamStanding("Stoke City");
            s.update(1, 0); // scored 1, conceded 0 → GA = 0

            assertEquals(Double.POSITIVE_INFINITY, s.goalAverage(),
                    "A team with GA=0 must return POSITIVE_INFINITY for correct sorting");
            assertTrue(Double.isInfinite(s.goalAverage()),
                    "Double.isInfinite() confirms this value needs special CSV formatting");
        }

        @Test
        @DisplayName("goal average with GF = 0 and GA = 0 returns POSITIVE_INFINITY")
        void goalAverageWithBothZeroIsPositiveInfinity() {
            // Synthetic edge case: a team that has only played goalless draws.
            // The Football League never encountered this. We treat it the same
            // as GA=0 to remain consistent and avoid NaN in sorting.
            TeamStanding s = new TeamStanding("Stoke City");
            s.update(0, 0); // goalless draw — GF=0, GA=0

            assertEquals(Double.POSITIVE_INFINITY, s.goalAverage(),
                    "0/0 is undefined; POSITIVE_INFINITY avoids NaN and is " +
                    "consistent with the GA=0 policy");
        }

        @Test
        @DisplayName("goal average with GF = 0 and GA > 0 is zero")
        void goalAverageWithZeroGoalsScoredIsZero() {
            TeamStanding s = new TeamStanding("Coventry City");
            s.update(0, 3); // lost 0-3 → GF=0, GA=3

            assertEquals(0.0, s.goalAverage(), 1e-9);
        }

        @Test
        @DisplayName("goal average accumulates over multiple matches")
        void goalAverageAccumulatesOverMultipleMatches() {
            TeamStanding s = new TeamStanding("Derby County");
            s.update(2, 1); // GF=2, GA=1
            s.update(4, 3); // GF=6, GA=4

            // goalAverage = 6/4 = 1.5
            assertEquals(1.5, s.goalAverage(), 1e-9);
        }
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("toString includes team name and key statistics")
    void toStringContainsUsefulInfo() {
        TeamStanding s = new TeamStanding("Arsenal");
        s.update(2, 1);

        String str = s.toString();
        assertTrue(str.contains("Arsenal"),  "toString should contain team name");
        assertTrue(str.contains("2"),        "toString should contain goals for");
        assertTrue(str.contains("1"),        "toString should contain goals against");
    }

    // -------------------------------------------------------------------------
    // Freeze behaviour
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("freeze protection")
    class FreezeProtection {

        @Test
        @DisplayName("update() throws IllegalStateException after freeze()")
        void updateAfterFreezeThrows() {
            TeamStanding s = new TeamStanding("Arsenal");
            s.update(2, 1); // valid before freeze
            s.freeze();

            assertThrows(IllegalStateException.class, () -> s.update(1, 0),
                    "update() must be blocked after freeze()");
        }

        @Test
        @DisplayName("all getters and goalAverage() still work after freeze()")
        void gettersWorkAfterFreeze() {
            TeamStanding s = new TeamStanding("Arsenal");
            s.update(2, 1);
            s.freeze();

            // No exception expected for any read operation
            assertDoesNotThrow(() -> {
                s.getTeamName();
                s.getPlayed();
                s.getWon();
                s.getDrawn();
                s.getLost();
                s.getGoalsFor();
                s.getGoalsAgainst();
                s.getPoints();
                s.goalAverage();
                s.toString();
            });
        }
    }
}
