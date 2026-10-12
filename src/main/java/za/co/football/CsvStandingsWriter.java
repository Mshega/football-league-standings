package za.co.football;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Writes a ranked list of {@link TeamStanding} objects to a UTF-8 CSV file.
 *
 * <h2>Output columns</h2>
 * <pre>
 *   position,team,played,won,drawn,lost,goals_for,goals_against,goal_average,points
 * </pre>
 *
 * <h2>Column details</h2>
 * <ul>
 *   <li><strong>position:</strong> 1-based rank (1 = top of the table).</li>
 *   <li><strong>team:</strong> team name, RFC 4180-escaped if necessary (see below).</li>
 *   <li><strong>played / won / drawn / lost:</strong> integer match counts.</li>
 *   <li><strong>goals_for / goals_against:</strong> integer goal counts.</li>
 *   <li><strong>goal_average:</strong> goals_for ÷ goals_against, formatted to
 *       three decimal places.
 *       <em>Special case:</em> when goals_against is zero,
 *       {@link TeamStanding#goalAverage()} returns {@link Double#POSITIVE_INFINITY}.
 *       This column is written as an <strong>empty field</strong> (nothing between
 *       the two commas) rather than the string {@code "Infinity"} or any invented
 *       large number. This is the most spreadsheet-compatible representation of an
 *       undefined ratio. The README documents this convention.</li>
 *   <li><strong>points:</strong> integer total.</li>
 * </ul>
 *
 * <h2>Team name escaping (RFC 4180)</h2>
 * If a team name contains a comma, a double-quote character, or a newline, it
 * is wrapped in double-quotes and any embedded double-quotes are doubled.
 * Examples:
 * <pre>
 *   Arsenal           →  Arsenal
 *   Notts, County     →  "Notts, County"
 *   Say "hello" FC    →  "Say ""hello"" FC"
 * </pre>
 *
 * <h2>Usage</h2>
 * <pre>
 *   List&lt;TeamStanding&gt; standings = StandingsCalculator.calculate(results);
 *   CsvStandingsWriter.write(standings, Path.of("output/standings.csv"));
 * </pre>
 */
public final class CsvStandingsWriter {

    /** CSV header row — column order is part of the public contract. */
    static final String HEADER =
            "position,team,played,won,drawn,lost,goals_for,goals_against,goal_average,points";

    private CsvStandingsWriter() {}

    /**
     * Writes the standings to the given path, creating or overwriting the file.
     *
     * <p>Parent directories must already exist; this method does not create them.
     *
     * @param standings ranked list of standings (index 0 = position 1);
     *                  must not be null; individual entries must not be null
     * @param outputFile path of the CSV file to write
     * @throws IllegalArgumentException if {@code standings} or {@code outputFile} is null
     * @throws UncheckedIOException if the file cannot be written
     */
    public static void write(List<TeamStanding> standings, Path outputFile) {
        if (standings == null) {
            throw new IllegalArgumentException("standings must not be null.");
        }
        if (outputFile == null) {
            throw new IllegalArgumentException("outputFile must not be null.");
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.newLine();

            for (int i = 0; i < standings.size(); i++) {
                TeamStanding s = standings.get(i);
                if (s == null) {
                    throw new IllegalArgumentException(
                            "standings list contains a null entry at index " + i + ".");
                }
                writer.write(formatRow(i + 1, s));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write CSV file: " + outputFile, e);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Formats one standings row as a CSV string.
     *
     * @param position 1-based rank
     * @param s        the team's standing
     * @return the formatted CSV row (no trailing newline)
     */
    static String formatRow(int position, TeamStanding s) {
        return position
                + "," + escapeCsvField(s.getTeamName())
                + "," + s.getPlayed()
                + "," + s.getWon()
                + "," + s.getDrawn()
                + "," + s.getLost()
                + "," + s.getGoalsFor()
                + "," + s.getGoalsAgainst()
                + "," + formatGoalAverage(s.goalAverage())
                + "," + s.getPoints();
    }

    /**
     * Formats the goal average for CSV output.
     *
     * <ul>
     *   <li>Normal values → three decimal places, always using a period as the
     *       decimal separator ({@link Locale#ROOT} ensures locale independence —
     *       on systems with a comma-decimal locale such as {@code de_DE}, the
     *       default {@code String.format("%.3f", v)} would produce {@code "2,333"}
     *       which would break CSV parsing).</li>
     *   <li>{@link Double#POSITIVE_INFINITY} (GA = 0) → empty string.
     *       Writing {@code "Infinity"} would be confusing and non-numeric for
     *       spreadsheet tools. An empty field clearly signals that the value is
     *       undefined and documents the GA=0 edge case visually.</li>
     * </ul>
     *
     * @param goalAverage the value from {@link TeamStanding#goalAverage()}
     * @return formatted string, never null
     */
    static String formatGoalAverage(double goalAverage) {
        if (Double.isInfinite(goalAverage)) {
            return "";  // GA = 0: undefined ratio — empty field in CSV
        }
        // Locale.ROOT guarantees a period decimal separator on every JVM locale.
        return String.format(Locale.ROOT, "%.3f", goalAverage);
    }

    /**
     * Escapes a field value for RFC 4180-compliant CSV output.
     *
     * <p>If the value contains a comma, double-quote, or newline character, it
     * is wrapped in double-quotes and any embedded double-quotes are doubled.
     * Values that require no escaping are returned unchanged.
     *
     * @param value the field value to escape (must not be null)
     * @return the escaped field, ready to embed in a CSV row
     */
    static String escapeCsvField(String value) {
        // Check whether quoting is necessary.
        boolean needsQuoting = value.contains(",")
                || value.contains("\"")
                || value.contains("\n")
                || value.contains("\r");

        if (!needsQuoting) {
            return value;
        }

        // Wrap in quotes and double any embedded quotes.
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
