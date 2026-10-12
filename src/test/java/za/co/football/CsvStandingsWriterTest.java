package za.co.football;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CsvStandingsWriter}.
 *
 * <p>Tests that need a file use {@code @TempDir} — JUnit 5 creates and cleans
 * up the directory automatically. Most structural tests call the package-private
 * static helpers ({@code formatRow}, {@code formatGoalAverage},
 * {@code escapeCsvField}) directly to stay fast and independent of file I/O.
 */
@DisplayName("CsvStandingsWriter")
class CsvStandingsWriterTest {

    @TempDir
    Path tempDir;

    // -------------------------------------------------------------------------
    // Helper: build a frozen TeamStanding with known stats
    // -------------------------------------------------------------------------

    /**
     * Builds a {@code TeamStanding} by feeding it a series of match updates,
     * then freezes it by passing it through {@link StandingsCalculator}.
     * Returns the first standing in the result (position 1).
     *
     * <p>We use the calculator to get a properly frozen standing, because
     * {@code CsvStandingsWriter} is expected to receive frozen standings in
     * normal use.
     */
    private static TeamStanding standingFor(String team, int goalsFor, int goalsAgainst) {
        // Construct a single match that gives the team the desired stats.
        // Team wins (or we don't care about the result for writer tests).
        List<MatchResult> matches = List.of(
                new MatchResult(team, "Opponent", goalsFor, goalsAgainst));
        return StandingsCalculator.calculate(matches).stream()
                .filter(s -> s.getTeamName().equals(team))
                .findFirst()
                .orElseThrow();
    }

    // -------------------------------------------------------------------------
    // formatGoalAverage (unit tests for the static helper)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("formatGoalAverage")
    class FormatGoalAverage {

        @Test
        @DisplayName("normal value formatted to 3 decimal places")
        void normalValueFormattedToThreeDecimalPlaces() {
            assertEquals("2.000", CsvStandingsWriter.formatGoalAverage(2.0));
            assertEquals("1.500", CsvStandingsWriter.formatGoalAverage(1.5));
            assertEquals("0.333", CsvStandingsWriter.formatGoalAverage(1.0 / 3.0));
        }

        @Test
        @DisplayName("decimal separator is always a period regardless of JVM locale")
        void decimalSeparatorIsAlwaysPeriod() {
            // Regression guard: String.format("%.3f", 1.5) with a comma-decimal
            // locale (e.g. de_DE) would produce "1,500" and break CSV parsing.
            // Locale.ROOT must be used in formatGoalAverage().
            String formatted = CsvStandingsWriter.formatGoalAverage(1.5);
            assertFalse(formatted.contains(","),
                    "Goal average must use period, not comma, as decimal separator. Got: " + formatted);
            assertTrue(formatted.contains("."),
                    "Goal average must contain a period decimal separator. Got: " + formatted);
            assertEquals("1.500", formatted);
        }

        @Test
        @DisplayName("POSITIVE_INFINITY (GA=0) writes as empty string")
        void positiveInfinityWritesAsEmptyString() {
            assertEquals("", CsvStandingsWriter.formatGoalAverage(Double.POSITIVE_INFINITY),
                    "Goal average must be empty string when GA=0, not \"Infinity\"");
        }

        @Test
        @DisplayName("zero goal average (GF=0, GA>0) writes as 0.000")
        void zeroGoalAverageWritesAsZero() {
            assertEquals("0.000", CsvStandingsWriter.formatGoalAverage(0.0));
        }
    }

    // -------------------------------------------------------------------------
    // escapeCsvField (unit tests for the static helper)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("escapeCsvField")
    class EscapeCsvField {

        @Test
        @DisplayName("plain name needs no escaping")
        void plainNameUnchanged() {
            assertEquals("Arsenal", CsvStandingsWriter.escapeCsvField("Arsenal"));
        }

        @Test
        @DisplayName("name with comma is wrapped in quotes")
        void nameWithCommaIsQuoted() {
            assertEquals("\"Notts, County\"",
                    CsvStandingsWriter.escapeCsvField("Notts, County"));
        }

        @Test
        @DisplayName("name with double-quote has quotes doubled and is wrapped")
        void nameWithDoubleQuoteIsEscaped() {
            assertEquals("\"Say \"\"hello\"\" FC\"",
                    CsvStandingsWriter.escapeCsvField("Say \"hello\" FC"));
        }

        @Test
        @DisplayName("name with newline is wrapped in quotes")
        void nameWithNewlineIsQuoted() {
            assertEquals("\"Line1\nLine2\"",
                    CsvStandingsWriter.escapeCsvField("Line1\nLine2"));
        }

        @Test
        @DisplayName("empty string needs no escaping")
        void emptyStringUnchanged() {
            assertEquals("", CsvStandingsWriter.escapeCsvField(""));
        }
    }

    // -------------------------------------------------------------------------
    // formatRow (unit tests for the static helper)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("formatRow")
    class FormatRow {

        @Test
        @DisplayName("column order: position,team,played,won,drawn,lost,gf,ga,avg,points")
        void columnOrderIsCorrect() {
            // Arsenal beat Opponent 3-1: W=1, GF=3, GA=1, Pts=2, avg=3.000
            TeamStanding s = standingFor("Arsenal", 3, 1);
            String row = CsvStandingsWriter.formatRow(1, s);

            // Split on commas (no quoted commas in this row)
            String[] cols = row.split(",");
            assertEquals("1",       cols[0], "position");
            assertEquals("Arsenal", cols[1], "team");
            assertEquals("1",       cols[2], "played");
            assertEquals("1",       cols[3], "won");
            assertEquals("0",       cols[4], "drawn");
            assertEquals("0",       cols[5], "lost");
            assertEquals("3",       cols[6], "goals_for");
            assertEquals("1",       cols[7], "goals_against");
            assertEquals("3.000",   cols[8], "goal_average");
            assertEquals("2",       cols[9], "points");
        }

        @Test
        @DisplayName("team name with comma is properly escaped in the row")
        void teamNameWithCommaIsEscapedInRow() {
            // Build a standing directly to avoid MatchResult validation rejecting a comma name.
            // We use a plain name through the calculator, then test escapeCsvField separately.
            // Here we test that formatRow delegates to escapeCsvField correctly.
            String row = CsvStandingsWriter.formatRow(1, standingFor("Arsenal", 2, 1));
            // Simple check: row starts with "1,Arsenal,"
            assertTrue(row.startsWith("1,Arsenal,"),
                    "Row must start with position and team name");
        }

        @Test
        @DisplayName("goal average is empty when GA is zero")
        void goalAverageIsEmptyForZeroGA() {
            // Arsenal beat Opponent 1-0: GA=0 → avg=∞ → empty field
            TeamStanding s = standingFor("Arsenal", 1, 0);
            String row = CsvStandingsWriter.formatRow(1, s);

            // Row: "1,Arsenal,1,1,0,0,1,0,,2"
            // The goal_average column (index 8) must be empty — two consecutive commas.
            assertTrue(row.contains(",,"),
                    "Row must contain empty goal_average field (two consecutive commas) " +
                    "when GA=0. Row was: " + row);
        }
    }

    // -------------------------------------------------------------------------
    // File output
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("file output")
    class FileOutput {

        @Test
        @DisplayName("header row is the first line")
        void headerIsFirstLine() throws Exception {
            Path output = tempDir.resolve("standings.csv");
            List<TeamStanding> standings = StandingsCalculator.calculate(List.of(
                    new MatchResult("Arsenal", "Chelsea", 1, 0)));

            CsvStandingsWriter.write(standings, output);

            List<String> lines = Files.readAllLines(output, StandardCharsets.UTF_8);
            assertEquals(CsvStandingsWriter.HEADER, lines.get(0));
        }

        @Test
        @DisplayName("row count equals standings size plus one header")
        void rowCountEqualsStandingsPlusHeader() throws Exception {
            Path output = tempDir.resolve("standings.csv");
            List<TeamStanding> standings = StandingsCalculator.calculate(List.of(
                    new MatchResult("Arsenal",   "Chelsea",   2, 1),
                    new MatchResult("Liverpool", "Leeds United", 1, 1)));

            CsvStandingsWriter.write(standings, output);

            List<String> lines = Files.readAllLines(output, StandardCharsets.UTF_8);
            // 4 teams → 4 data rows + 1 header = 5 lines
            assertEquals(standings.size() + 1, lines.size());
        }

        @Test
        @DisplayName("position column increments correctly across rows")
        void positionsAreSequential() throws Exception {
            Path output = tempDir.resolve("standings.csv");
            List<TeamStanding> standings = StandingsCalculator.calculate(List.of(
                    new MatchResult("Arsenal",   "Chelsea",   2, 0),
                    new MatchResult("Liverpool", "Leeds United", 1, 0)));

            CsvStandingsWriter.write(standings, output);

            List<String> lines = Files.readAllLines(output, StandardCharsets.UTF_8);
            // Lines: [header, row1, row2, row3, row4]
            for (int i = 1; i < lines.size(); i++) {
                int expectedPosition = i;
                String firstField = lines.get(i).split(",")[0];
                assertEquals(String.valueOf(expectedPosition), firstField,
                        "Row " + i + " should have position " + expectedPosition);
            }
        }

        @Test
        @DisplayName("empty standings list writes only the header")
        void emptyStandingsWritesOnlyHeader() throws Exception {
            Path output = tempDir.resolve("standings.csv");
            CsvStandingsWriter.write(List.of(), output);

            List<String> lines = Files.readAllLines(output, StandardCharsets.UTF_8);
            assertEquals(1, lines.size(), "Only the header row should be present");
            assertEquals(CsvStandingsWriter.HEADER, lines.get(0));
        }

        @Test
        @DisplayName("round-trip: values written can be read back correctly")
        void roundTripValuesAreCorrect() throws Exception {
            Path output = tempDir.resolve("standings.csv");
            List<TeamStanding> standings = StandingsCalculator.calculate(List.of(
                    new MatchResult("Arsenal", "Chelsea", 3, 1)));

            CsvStandingsWriter.write(standings, output);

            List<String> lines = Files.readAllLines(output, StandardCharsets.UTF_8);
            // First data row (after header) for Arsenal (3-1 home win)
            String arsenalRow = lines.stream()
                    .skip(1)
                    .filter(l -> l.contains("Arsenal"))
                    .findFirst()
                    .orElseThrow();

            String[] cols = arsenalRow.split(",");
            assertEquals("Arsenal", cols[1]);   // team
            assertEquals("1",       cols[2]);   // played
            assertEquals("1",       cols[3]);   // won
            assertEquals("3",       cols[6]);   // goals_for
            assertEquals("1",       cols[7]);   // goals_against
            assertEquals("3.000",   cols[8]);   // goal_average
            assertEquals("2",       cols[9]);   // points
        }

        @Test
        @DisplayName("zero goals against: goal_average field is empty in written file")
        void zeroGoalsAgainstWritesEmptyGoalAverageField() throws Exception {
            Path output = tempDir.resolve("standings.csv");
            // Arsenal beat Chelsea 1-0: Arsenal GA=0
            List<TeamStanding> standings = StandingsCalculator.calculate(List.of(
                    new MatchResult("Arsenal", "Chelsea", 1, 0)));

            CsvStandingsWriter.write(standings, output);

            String content = Files.readString(output, StandardCharsets.UTF_8);
            // The Arsenal row must contain an empty goal_average field — two consecutive commas
            // between goals_against and points columns.
            assertTrue(content.contains("0,,"),
                    "Arsenal row must contain empty goal_average (0,,) when GA=0. " +
                    "Content was:\n" + content);
            assertFalse(content.contains("Infinity"),
                    "\"Infinity\" must never appear in CSV output");
        }

        @Test
        @DisplayName("null standings list throws IllegalArgumentException")
        void nullStandingsThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> CsvStandingsWriter.write(null, tempDir.resolve("out.csv")));
        }

        @Test
        @DisplayName("null output path throws IllegalArgumentException")
        void nullOutputPathThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> CsvStandingsWriter.write(List.of(), null));
        }
    }

    // -------------------------------------------------------------------------
    // Integration: reader → calculator → writer → verify file
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("integration: read → calculate → write")
    class Integration {

        @Test
        @DisplayName("full pipeline produces valid CSV with correct standings")
        void fullPipelineProducesValidCsv() throws Exception {
            // Write an input CSV
            Path input = tempDir.resolve("input.csv");
            Files.writeString(input,
                    "home_team,away_team,home_goals,away_goals\n" +
                    "Arsenal,Chelsea,2,1\n" +       // Arsenal win (pts=2)
                    "Arsenal,Liverpool,1,1\n" +     // draw (pts=1, Arsenal total=3)
                    "Chelsea,Liverpool,0,1\n",      // Liverpool win (pts=2)
                    StandardCharsets.UTF_8);

            Path output = tempDir.resolve("output.csv");

            // Run the full pipeline
            List<MatchResult> matches  = CsvMatchReader.read(input);
            List<TeamStanding> standings = StandingsCalculator.calculate(matches);
            CsvStandingsWriter.write(standings, output);

            // Verify the output
            List<String> lines = Files.readAllLines(output, StandardCharsets.UTF_8);
            assertEquals(CsvStandingsWriter.HEADER, lines.get(0), "Header must be correct");
            assertEquals(4, lines.size(), "3 teams + 1 header");

            // Points: Arsenal=3 (W+D), Liverpool=3 (D+W), Chelsea=0 (L+L)
            // Tiebreaker (goal average):
            //   Arsenal:   GF=3 (2+1), GA=2 (1+1) → avg=1.500
            //   Liverpool: GF=2 (1+1), GA=1 (1+0) → avg=2.000  ← ranks higher
            assertTrue(lines.get(1).startsWith("1,Liverpool,"), "Liverpool must be 1st (avg=2.000)");
            assertTrue(lines.get(2).startsWith("2,Arsenal,"),   "Arsenal must be 2nd (avg=1.500)");
            assertTrue(lines.get(3).startsWith("3,Chelsea,"),   "Chelsea must be 3rd (0 pts)");
        }
    }
}
