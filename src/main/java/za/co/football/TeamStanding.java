package za.co.football;

/**
 * Accumulates league statistics for a single team across multiple matches.
 *
 * <p>Each instance represents one row in the standings table — the kind you
 * would see printed in a 1974 newspaper. Start with a team name and all
 * counters at zero, then call {@link #update(int, int)} once for every match
 * the team played.
 *
 * <h2>Historical scoring rules (1974/75 English First Division)</h2>
 * <ul>
 *   <li><strong>Win  → 2 points</strong></li>
 *   <li><strong>Draw → 1 point</strong></li>
 *   <li><strong>Loss → 0 points</strong></li>
 * </ul>
 * The switch to 3 points for a win did not happen until the 1981/82 season.
 * Using the wrong value here would silently produce wrong standings, so this
 * policy is tested explicitly in {@code TeamStandingTest}.
 *
 * <h2>Goal average (not goal difference)</h2>
 * The Football League ranked tied teams by <em>goal average</em>
 * (goals scored ÷ goals conceded) until the 1976/77 season, not by goal
 * difference (goals scored − goals conceded). See {@link #goalAverage()}.
 */
public class TeamStanding {

    /** Points awarded for a win under the 1974/75 two-point system. */
    public static final int POINTS_FOR_WIN = 2;

    /** Points awarded for a draw. */
    public static final int POINTS_FOR_DRAW = 1;

    /** Points awarded for a loss. */
    public static final int POINTS_FOR_LOSS = 0;

    private final String teamName;

    private boolean frozen = false;   // set to true by StandingsCalculator after ranking

    private int played;
    private int won;
    private int drawn;
    private int lost;
    private int goalsFor;
    private int goalsAgainst;
    private int points;

    /**
     * Creates a new standing for the given team with all counters at zero.
     *
     * @param teamName the team's name — must not be blank
     * @throws IllegalArgumentException if {@code teamName} is null or blank
     */
    public TeamStanding(String teamName) {
        if (teamName == null || teamName.isBlank()) {
            throw new IllegalArgumentException("Team name must not be blank.");
        }
        this.teamName = teamName.trim();
    }

    /**
     * Freezes this standing so that {@link #update} can no longer be called.
     *
     * <p>Called by {@link StandingsCalculator} after all matches have been
     * processed and the list has been sorted. This prevents callers who hold
     * a reference to a returned {@code TeamStanding} from accidentally (or
     * deliberately) mutating its statistics after the fact.
     *
     * <p>Package-private: only code inside {@code za.co.football} may freeze
     * a standing. Once frozen, a standing cannot be unfrozen.
     */
    void freeze() {
        this.frozen = true;
    }

    /**
     * Records the outcome of one match for this team.
     *
     * <p>Call this method once per match, passing in the goals this team
     * scored and the goals this team conceded. The method determines the
     * result (win/draw/loss) and updates all counters accordingly.
     *
     * <p>Example — team won 3-1:
     * <pre>
     *   standing.update(3, 1);  // goalsFor += 3, goalsAgainst += 1, won++, points += 2
     * </pre>
     *
     * @param goalsScored   goals scored by this team in the match (≥ 0)
     * @param goalsConceded goals conceded by this team in the match (≥ 0)
     * @throws IllegalArgumentException if either argument is negative
     */
    public void update(int goalsScored, int goalsConceded) {
        if (frozen) {
            throw new IllegalStateException(
                    "TeamStanding for \"" + teamName + "\" is frozen and cannot be updated.");
        }
        if (goalsScored < 0) {
            throw new IllegalArgumentException(
                    "Goals scored cannot be negative, got: " + goalsScored);
        }
        if (goalsConceded < 0) {
            throw new IllegalArgumentException(
                    "Goals conceded cannot be negative, got: " + goalsConceded);
        }

        played++;
        goalsFor += goalsScored;
        goalsAgainst += goalsConceded;

        if (goalsScored > goalsConceded) {
            won++;
            points += POINTS_FOR_WIN;
        } else if (goalsScored == goalsConceded) {
            drawn++;
            points += POINTS_FOR_DRAW;
        } else {
            lost++;
            points += POINTS_FOR_LOSS;
        }
    }

    /**
     * Calculates the goal average for ranking purposes.
     *
     * <p>Goal average = goals for ÷ goals against.
     *
     * <p><strong>Historical source:</strong> the Football League used goal
     * average (not goal difference) as the first tiebreaker from the 1894–95
     * season through the 1975–76 season. The official classification rules
     * are: 1) Points; 2) Goal average; 3) Goals scored.
     * Source: Ian Laschke, <em>Rothmans Book of Football League Records
     * 1888–89 to 1978–79</em>, as cited in the Wikipedia 1970–71 Football
     * League article.
     *
     * <p><strong>Zero-goals-against policy:</strong> if a team has conceded
     * zero goals, ordinary division is undefined. This implementation returns
     * {@link Double#POSITIVE_INFINITY}, which sorts <em>above</em> all teams
     * with a finite goal average. This is mathematically consistent (the limit
     * of GF/GA as GA→0⁺ is +∞) and historically reasonable — no Football
     * League First Division team has ever conceded zero goals in a completed
     * season, so the Football League never needed to define this case formally.
     *
     * <p>If both goals for <em>and</em> goals against are zero (e.g. a team
     * has played only goalless draws), the result is also
     * {@code POSITIVE_INFINITY}. This is consistent but acknowledged as a
     * synthetic edge case.
     *
     * <p><strong>CSV output note:</strong> {@code Double.POSITIVE_INFINITY}
     * serialises as the string {@code "Infinity"} in Java. The CSV writer
     * ({@code CsvStandingsWriter}) is responsible for converting this to a
     * human-readable representation (e.g. an empty string or "N/A") so that
     * the output file is valid and spreadsheet-compatible. Do not write
     * {@code "Infinity"} to CSV without deliberate formatting.
     *
     * @return goals-for divided by goals-against, or
     *         {@code Double.POSITIVE_INFINITY} when goals-against is zero
     */
    public double goalAverage() {
        if (goalsAgainst == 0) {
            return Double.POSITIVE_INFINITY;
        }
        return (double) goalsFor / goalsAgainst;
    }

    // -------------------------------------------------------------------------
    // Getters — read-only access; no setters to keep mutation controlled
    // -------------------------------------------------------------------------

    /** @return the team's name */
    public String getTeamName() { return teamName; }

    /** @return total matches played */
    public int getPlayed() { return played; }

    /** @return total matches won */
    public int getWon() { return won; }

    /** @return total matches drawn */
    public int getDrawn() { return drawn; }

    /** @return total matches lost */
    public int getLost() { return lost; }

    /** @return total goals scored across all matches */
    public int getGoalsFor() { return goalsFor; }

    /** @return total goals conceded across all matches */
    public int getGoalsAgainst() { return goalsAgainst; }

    /** @return total points accumulated under the 2-point win system */
    public int getPoints() { return points; }

    /**
     * Returns a human-readable summary of this standing, useful in test
     * failure messages and debug output.
     *
     * <p>Format: {@code TeamName P:x W:x D:x L:x GF:x GA:x Pts:x}
     */
    @Override
    public String toString() {
        return String.format("%s P:%d W:%d D:%d L:%d GF:%d GA:%d Pts:%d",
                teamName, played, won, drawn, lost, goalsFor, goalsAgainst, points);
    }
}
