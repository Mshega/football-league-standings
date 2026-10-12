package za.co.football;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Processes a collection of {@link MatchResult} objects and produces a ranked
 * list of {@link TeamStanding} objects — one per team, sorted according to the
 * Football League's official 1974/75 classification rules.
 *
 * <h2>Ranking order</h2>
 * <ol>
 *   <li><strong>Points</strong> — descending (more points = higher position)</li>
 *   <li><strong>Goal average</strong> — descending (GF ÷ GA, higher = better)</li>
 *   <li><strong>Goals scored</strong> — descending (more goals = higher position)</li>
 *   <li><strong>Team name</strong> — ascending (alphabetical A→Z, deterministic
 *       fallback only; not a documented historical rule)</li>
 * </ol>
 *
 * <h2>Goal average and infinity</h2>
 * When a team's goals-against total is zero, {@link TeamStanding#goalAverage()}
 * returns {@link Double#POSITIVE_INFINITY}. The comparator handles this correctly
 * via {@link Double#compare}, which defines a total order over all {@code double}
 * values including infinity — no subtraction is used, avoiding overflow or NaN.
 *
 * <h2>Usage</h2>
 * <pre>
 *   List&lt;MatchResult&gt; results = ...;
 *   List&lt;TeamStanding&gt; table = StandingsCalculator.calculate(results);
 * </pre>
 *
 * <p>The returned list is unmodifiable. The method is stateless; there is no
 * need to instantiate this class — all behaviour is in the static factory method.
 */
public final class StandingsCalculator {

    /**
     * The ranking comparator, expressed as a chain of independent criteria.
     *
     * <p>Reading this from left to right:
     * <ol>
     *   <li>Sort by points — higher points first.</li>
     *   <li>If points are equal, sort by goal average — higher average first.</li>
     *   <li>If goal average is also equal, sort by goals scored — more goals first.</li>
     *   <li>If all three are equal, sort alphabetically by team name — A before Z.</li>
     * </ol>
     *
     * <p><strong>Why each criterion is wrapped independently:</strong>
     * Java's {@link Comparator#reversed()} reverses the <em>entire accumulated
     * comparator chain</em>, not just the last step. Calling
     * {@code chainSoFar.thenComparingInt(fn).reversed()} would flip every
     * prior criterion as well, producing the wrong order. The safe pattern is
     * to wrap each descending criterion in its own
     * {@code Comparator.comparingX(fn).reversed()} and pass it to
     * {@code thenComparing(...)}, so each reversal is scoped to exactly one
     * criterion.
     *
     * <p><strong>Why {@link Double#compare} is used for goal average:</strong>
     * Subtraction ({@code a.goalAverage() - b.goalAverage()}) is unsafe for
     * {@code double} comparisons: it can lose precision and produces {@code NaN}
     * when either operand is {@code NaN}. {@link Double#compare} defines a
     * consistent total order over all {@code double} values including
     * {@link Double#POSITIVE_INFINITY} and {@code NaN}.
     */
    static final Comparator<TeamStanding> RANKING_COMPARATOR =
            // Step 1: higher points first
            Comparator.comparing(
                    TeamStanding::getPoints,
                    Comparator.reverseOrder()
            )
            // Step 2: higher goal average first (safe double comparison)
            .thenComparing(
                    Comparator.comparingDouble(TeamStanding::goalAverage).reversed()
            )
            // Step 3: more goals scored first
            .thenComparing(
                    TeamStanding::getGoalsFor,
                    Comparator.reverseOrder()
            )
            // Step 4: team name A→Z — deterministic fallback, not a historical rule
            .thenComparing(TeamStanding::getTeamName);

    // Private constructor — this class is not meant to be instantiated.
    private StandingsCalculator() {}

    /**
     * Processes the given match results and returns the league standings in
     * ranking order.
     *
     * <p>Each team that appears in any match result (as home or away team)
     * will have exactly one entry in the returned list. Statistics are
     * accumulated across all matches.
     *
     * @param matchResults the complete set of match results to process;
     *                     must not be null; individual entries must not be null
     * @return an unmodifiable, ranked list of standings — one entry per team;
     *         returns an empty list if {@code matchResults} is empty
     * @throws IllegalArgumentException if {@code matchResults} is null
     * @throws IllegalArgumentException if any entry in {@code matchResults} is null
     */
    public static List<TeamStanding> calculate(Collection<MatchResult> matchResults) {
        if (matchResults == null) {
            throw new IllegalArgumentException(
                    "matchResults must not be null.");
        }

        // LinkedHashMap preserves insertion order during accumulation, which
        // keeps behaviour predictable when debugging — the final sort overrides
        // this order anyway.
        Map<String, TeamStanding> standingsMap = new LinkedHashMap<>();

        int matchIndex = 0;
        for (MatchResult match : matchResults) {
            if (match == null) {
                throw new IllegalArgumentException(
                        "matchResults contains a null entry at index " + matchIndex + ".");
            }

            // Look up or create the standing for each team.
            // computeIfAbsent creates a new TeamStanding only when the team
            // has not been seen before.
            TeamStanding homeStanding = standingsMap.computeIfAbsent(
                    match.homeTeam(), TeamStanding::new);
            TeamStanding awayStanding = standingsMap.computeIfAbsent(
                    match.awayTeam(), TeamStanding::new);

            // Update both teams from the same match.
            // Home team: scored homeGoals, conceded awayGoals.
            // Away team: scored awayGoals, conceded homeGoals.
            homeStanding.update(match.homeGoals(), match.awayGoals());
            awayStanding.update(match.awayGoals(), match.homeGoals());

            matchIndex++;
        }

        // Copy to a list, sort by the ranking comparator, freeze every standing
        // so returned objects cannot be mutated by callers, then wrap as
        // unmodifiable so the list structure itself cannot be changed either.
        List<TeamStanding> ranked = new ArrayList<>(standingsMap.values());
        ranked.sort(RANKING_COMPARATOR);
        ranked.forEach(TeamStanding::freeze);
        return Collections.unmodifiableList(ranked);
    }
}
