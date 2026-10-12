package za.co.football;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MatchResult}.
 *
 * <p>Each test is small and independent. The {@code @DisplayName} annotations
 * describe intent in plain English so the test report is readable without
 * opening the source code.
 *
 * <p>Pattern used throughout: <em>arrange → act → assert</em>.
 * <ul>
 *   <li>Arrange: set up the inputs.</li>
 *   <li>Act: call the code under test.</li>
 *   <li>Assert: verify the outcome.</li>
 * </ul>
 */
@DisplayName("MatchResult")
class MatchResultTest {

    // -------------------------------------------------------------------------
    // Happy path — valid construction
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("stores all four fields when inputs are valid")
    void validMatchIsStoredCorrectly() {
        MatchResult match = new MatchResult("Arsenal", "Chelsea", 2, 1);

        assertEquals("Arsenal", match.homeTeam());
        assertEquals("Chelsea", match.awayTeam());
        assertEquals(2, match.homeGoals());
        assertEquals(1, match.awayGoals());
    }

    @Test
    @DisplayName("a goalless draw is valid")
    void goallessDrawIsValid() {
        MatchResult match = new MatchResult("Leeds United", "Liverpool", 0, 0);

        assertEquals(0, match.homeGoals());
        assertEquals(0, match.awayGoals());
    }

    @Test
    @DisplayName("team names are trimmed of surrounding whitespace")
    void teamNamesAreTrimmed() {
        MatchResult match = new MatchResult("  Ipswich Town  ", " Norwich City ", 1, 0);

        assertEquals("Ipswich Town", match.homeTeam());
        assertEquals("Norwich City", match.awayTeam());
    }

    @Test
    @DisplayName("two MatchResult objects with the same data are equal (record equality)")
    void recordEqualityWorks() {
        MatchResult a = new MatchResult("Arsenal", "Chelsea", 2, 1);
        MatchResult b = new MatchResult("Arsenal", "Chelsea", 2, 1);

        assertEquals(a, b);
    }

    // -------------------------------------------------------------------------
    // Team name validation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("null home team throws IllegalArgumentException")
    void nullHomeTeamIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new MatchResult(null, "Chelsea", 1, 0));
    }

    @Test
    @DisplayName("null away team throws IllegalArgumentException")
    void nullAwayTeamIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new MatchResult("Arsenal", null, 1, 0));
    }

    @ParameterizedTest(name = "blank home team \"{0}\" is rejected")
    @ValueSource(strings = {"", " ", "   ", "\t"})
    @DisplayName("blank home team throws IllegalArgumentException")
    void blankHomeTeamIsRejected(String blank) {
        assertThrows(IllegalArgumentException.class,
                () -> new MatchResult(blank, "Chelsea", 1, 0));
    }

    @ParameterizedTest(name = "blank away team \"{0}\" is rejected")
    @ValueSource(strings = {"", " ", "   ", "\t"})
    @DisplayName("blank away team throws IllegalArgumentException")
    void blankAwayTeamIsRejected(String blank) {
        assertThrows(IllegalArgumentException.class,
                () -> new MatchResult("Arsenal", blank, 1, 0));
    }

    @Test
    @DisplayName("same team name for home and away throws IllegalArgumentException")
    void sameTeamNameIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new MatchResult("Arsenal", "Arsenal", 1, 1));
    }

    @Test
    @DisplayName("same team name is rejected regardless of case")
    void sameTeamNameCaseInsensitiveIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new MatchResult("arsenal", "ARSENAL", 0, 0));
    }

    @Test
    @DisplayName("same team name is rejected even with surrounding whitespace")
    void sameTeamNameWithWhitespaceIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new MatchResult("Arsenal", "  Arsenal  ", 1, 1));
    }

    // -------------------------------------------------------------------------
    // Goal count validation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("negative home goals throws IllegalArgumentException")
    void negativeHomeGoalsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new MatchResult("Arsenal", "Chelsea", -1, 0));
    }

    @Test
    @DisplayName("negative away goals throws IllegalArgumentException")
    void negativeAwayGoalsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new MatchResult("Arsenal", "Chelsea", 0, -1));
    }
}
