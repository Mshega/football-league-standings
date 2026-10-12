package za.co.football;

/**
 * An immutable record representing one completed football match.
 *
 * <p>A Java {@code record} is the right choice here because a match result is
 * pure data — it never changes after the final whistle. The compiler generates
 * {@code equals}, {@code hashCode}, and {@code toString} automatically.
 *
 * <p><strong>Validation rules (enforced in the compact constructor):</strong>
 * <ul>
 *   <li>Team names must not be null or blank (whitespace-only counts as blank).</li>
 *   <li>Home and away teams must be different (case-insensitive comparison).</li>
 *   <li>Goal counts must be zero or positive; negative goals are rejected.</li>
 * </ul>
 *
 * <p>Example:
 * <pre>
 *   MatchResult m = new MatchResult("Arsenal", "Chelsea", 2, 1);
 *   // Arsenal won 2-1 at home
 * </pre>
 *
 * @param homeTeam   name of the home side, as it appears in the CSV
 * @param awayTeam   name of the away side, as it appears in the CSV
 * @param homeGoals  goals scored by the home side (≥ 0)
 * @param awayGoals  goals scored by the away side (≥ 0)
 */
public record MatchResult(String homeTeam, String awayTeam, int homeGoals, int awayGoals) {

    /**
     * Compact constructor — runs automatically whenever a {@code MatchResult}
     * is created, including inside {@code CsvMatchReader}.
     *
     * <p>Using a compact constructor (no parameter list) means the record
     * fields are still assigned normally; we just add validation before that.
     */
    public MatchResult {
        if (homeTeam == null || homeTeam.isBlank()) {
            throw new IllegalArgumentException("Home team name must not be blank.");
        }
        if (awayTeam == null || awayTeam.isBlank()) {
            throw new IllegalArgumentException("Away team name must not be blank.");
        }
        if (homeTeam.trim().equalsIgnoreCase(awayTeam.trim())) {
            throw new IllegalArgumentException(
                    "A team cannot play against itself: \"" + homeTeam.trim() + "\".");
        }
        if (homeGoals < 0) {
            throw new IllegalArgumentException(
                    "Home goals cannot be negative, got: " + homeGoals);
        }
        if (awayGoals < 0) {
            throw new IllegalArgumentException(
                    "Away goals cannot be negative, got: " + awayGoals);
        }

        // Normalise team names: strip leading/trailing whitespace.
        // The record fields are assigned after this block completes.
        homeTeam = homeTeam.trim();
        awayTeam = awayTeam.trim();
    }
}
