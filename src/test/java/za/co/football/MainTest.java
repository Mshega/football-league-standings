package za.co.football;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Main#run(String[], PrintStream)}.
 *
 * <p>All tests call {@code Main.run()} directly — never {@code Main.main()} —
 * so that {@link System#exit} is never invoked during the test run.
 *
 * <p>A {@link ByteArrayOutputStream} captures stderr for each test so we can
 * assert on error messages without polluting the test console.
 *
 * <p>{@code @TempDir} provides isolated temporary directories; JUnit 5 deletes
 * them automatically after each test class.
 */
@DisplayName("Main (CLI entry point)")
class MainTest {

    @TempDir
    Path tempDir;

    // -------------------------------------------------------------------------
    // Helper: capture stderr
    // -------------------------------------------------------------------------

    private static PrintStream capturingStream(ByteArrayOutputStream buf) {
        return new PrintStream(buf, true, StandardCharsets.UTF_8);
    }

    private static String captured(ByteArrayOutputStream buf) {
        return buf.toString(StandardCharsets.UTF_8);
    }

    // -------------------------------------------------------------------------
    // Helper: write a minimal valid input CSV
    // -------------------------------------------------------------------------

    private Path writeValidInput(String filename) throws Exception {
        Path p = tempDir.resolve(filename);
        Files.writeString(p,
                "home_team,away_team,home_goals,away_goals\n" +
                "Arsenal,Chelsea,2,1\n" +
                "Liverpool,Leeds United,1,1\n",
                StandardCharsets.UTF_8);
        return p;
    }

    // -------------------------------------------------------------------------
    // Argument validation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("argument validation")
    class ArgumentValidation {

        @Test
        @DisplayName("no arguments returns EXIT_BAD_ARGS")
        void noArgsReturnsBadArgs() {
            var buf = new ByteArrayOutputStream();
            int code = Main.run(new String[]{}, capturingStream(buf));
            assertEquals(Main.EXIT_BAD_ARGS, code);
            assertTrue(captured(buf).contains("2 arguments"),
                    "Error message should mention expected argument count");
        }

        @Test
        @DisplayName("one argument returns EXIT_BAD_ARGS")
        void oneArgReturnsBadArgs() {
            var buf = new ByteArrayOutputStream();
            int code = Main.run(new String[]{"only-one.csv"}, capturingStream(buf));
            assertEquals(Main.EXIT_BAD_ARGS, code);
        }

        @Test
        @DisplayName("three arguments returns EXIT_BAD_ARGS")
        void threeArgsReturnsBadArgs() {
            var buf = new ByteArrayOutputStream();
            int code = Main.run(new String[]{"a.csv", "b.csv", "extra"}, capturingStream(buf));
            assertEquals(Main.EXIT_BAD_ARGS, code);
        }

        @Test
        @DisplayName("null args returns EXIT_BAD_ARGS")
        void nullArgsReturnsBadArgs() {
            var buf = new ByteArrayOutputStream();
            int code = Main.run(null, capturingStream(buf));
            assertEquals(Main.EXIT_BAD_ARGS, code);
        }

        @Test
        @DisplayName("error message includes usage instructions")
        void errorMessageIncludesUsage() {
            var buf = new ByteArrayOutputStream();
            Main.run(new String[]{}, capturingStream(buf));
            String output = captured(buf);
            assertTrue(output.contains("Usage:"),
                    "Error output should include a usage line");
        }
    }

    // -------------------------------------------------------------------------
    // Input file errors
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("input file errors")
    class InputFileErrors {

        @Test
        @DisplayName("non-existent input file returns EXIT_FILE_NOT_FOUND")
        void missingInputFileReturnsFileNotFound() {
            var buf = new ByteArrayOutputStream();
            Path missing  = tempDir.resolve("does-not-exist.csv");
            Path output   = tempDir.resolve("out.csv");

            int code = Main.run(
                    new String[]{missing.toString(), output.toString()},
                    capturingStream(buf));

            assertEquals(Main.EXIT_FILE_NOT_FOUND, code);
            assertTrue(captured(buf).toLowerCase().contains("not found"),
                    "Error message should say 'not found'");
        }

        @Test
        @DisplayName("malformed CSV returns EXIT_BAD_CSV")
        void malformedCsvReturnsBadCsv() throws Exception {
            Path bad = tempDir.resolve("bad.csv");
            Files.writeString(bad,
                    "home_team,away_team,home_goals,away_goals\n" +
                    "Arsenal,Chelsea,notanumber,1\n",
                    StandardCharsets.UTF_8);

            var buf = new ByteArrayOutputStream();
            int code = Main.run(
                    new String[]{bad.toString(), tempDir.resolve("out.csv").toString()},
                    capturingStream(buf));

            assertEquals(Main.EXIT_BAD_CSV, code);
            String msg = captured(buf);
            assertTrue(msg.toLowerCase().contains("malformed") || msg.toLowerCase().contains("csv"),
                    "Error message should mention CSV problem");
        }

        @Test
        @DisplayName("wrong header returns EXIT_BAD_CSV with line number in message")
        void wrongHeaderReturnsBadCsv() throws Exception {
            Path bad = tempDir.resolve("wrongheader.csv");
            Files.writeString(bad,
                    "team_a,team_b,score_a,score_b\nArsenal,Chelsea,2,1\n",
                    StandardCharsets.UTF_8);

            var buf = new ByteArrayOutputStream();
            int code = Main.run(
                    new String[]{bad.toString(), tempDir.resolve("out.csv").toString()},
                    capturingStream(buf));

            assertEquals(Main.EXIT_BAD_CSV, code);
            assertTrue(captured(buf).contains("Line 1"),
                    "Error message should include the line number from CsvParseException");
        }
    }

    // -------------------------------------------------------------------------
    // Successful execution
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("successful execution")
    class SuccessfulExecution {

        @Test
        @DisplayName("valid input produces output file and returns EXIT_OK")
        void validInputProducesOutputAndReturnsZero() throws Exception {
            Path input  = writeValidInput("input.csv");
            Path output = tempDir.resolve("output.csv");
            var buf = new ByteArrayOutputStream();

            int code = Main.run(
                    new String[]{input.toString(), output.toString()},
                    capturingStream(buf));

            assertEquals(Main.EXIT_OK, code, "Should return 0 on success");
            assertTrue(Files.exists(output), "Output file must be created");
        }

        @Test
        @DisplayName("output CSV has correct header as its first line")
        void outputHasCorrectHeader() throws Exception {
            Path input  = writeValidInput("input.csv");
            Path output = tempDir.resolve("output.csv");

            Main.run(
                    new String[]{input.toString(), output.toString()},
                    new PrintStream(new ByteArrayOutputStream()));

            List<String> lines = Files.readAllLines(output, StandardCharsets.UTF_8);
            assertEquals(CsvStandingsWriter.HEADER, lines.get(0));
        }

        @Test
        @DisplayName("output CSV has one row per team plus the header")
        void outputRowCountMatchesTeamCount() throws Exception {
            Path input  = writeValidInput("input.csv");  // Arsenal, Chelsea, Liverpool, Leeds
            Path output = tempDir.resolve("output.csv");

            Main.run(
                    new String[]{input.toString(), output.toString()},
                    new PrintStream(new ByteArrayOutputStream()));

            List<String> lines = Files.readAllLines(output, StandardCharsets.UTF_8);
            // 4 teams + 1 header = 5 lines
            assertEquals(5, lines.size());
        }

        @Test
        @DisplayName("position column is sequential starting at 1")
        void positionColumnIsSequential() throws Exception {
            Path input  = writeValidInput("input.csv");
            Path output = tempDir.resolve("output.csv");

            Main.run(
                    new String[]{input.toString(), output.toString()},
                    new PrintStream(new ByteArrayOutputStream()));

            List<String> lines = Files.readAllLines(output, StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) {
                String firstField = lines.get(i).split(",")[0];
                assertEquals(String.valueOf(i), firstField);
            }
        }

        @Test
        @DisplayName("output directory is created if it does not exist")
        void outputDirectoryIsCreatedAutomatically() throws Exception {
            Path input  = writeValidInput("input.csv");
            Path outputDir = tempDir.resolve("new-subdir");
            Path output = outputDir.resolve("standings.csv");

            // The directory does not exist yet — run() must create it.
            assertFalse(Files.exists(outputDir));

            int code = Main.run(
                    new String[]{input.toString(), output.toString()},
                    new PrintStream(new ByteArrayOutputStream()));

            assertEquals(Main.EXIT_OK, code);
            assertTrue(Files.exists(output), "Output file must exist in the newly created directory");
        }

        @Test
        @DisplayName("goal_average column never contains the string 'Infinity'")
        void infinityNeverAppearsInOutput() throws Exception {
            // Create input where one team has GA=0 (wins 1-0)
            Path input = tempDir.resolve("zeroGA.csv");
            Files.writeString(input,
                    "home_team,away_team,home_goals,away_goals\n" +
                    "Arsenal,Chelsea,1,0\n",
                    StandardCharsets.UTF_8);
            Path output = tempDir.resolve("output.csv");

            Main.run(
                    new String[]{input.toString(), output.toString()},
                    new PrintStream(new ByteArrayOutputStream()));

            String content = Files.readString(output, StandardCharsets.UTF_8);
            assertFalse(content.contains("Infinity"),
                    "The string 'Infinity' must never appear in the output CSV");
        }

        @Test
        @DisplayName("standings are ranked by points descending")
        void standingsAreRankedByPoints() throws Exception {
            // Arsenal win (2 pts), Chelsea lose (0 pts)
            Path input = tempDir.resolve("ranked.csv");
            Files.writeString(input,
                    "home_team,away_team,home_goals,away_goals\n" +
                    "Arsenal,Chelsea,1,0\n",
                    StandardCharsets.UTF_8);
            Path output = tempDir.resolve("output.csv");

            Main.run(
                    new String[]{input.toString(), output.toString()},
                    new PrintStream(new ByteArrayOutputStream()));

            List<String> lines = Files.readAllLines(output, StandardCharsets.UTF_8);
            assertTrue(lines.get(1).contains("Arsenal"),
                    "Arsenal (2 pts) must be first");
            assertTrue(lines.get(2).contains("Chelsea"),
                    "Chelsea (0 pts) must be second");
        }

        @Test
        @DisplayName("existing output file is overwritten silently")
        void existingOutputFileIsOverwritten() throws Exception {
            Path input  = writeValidInput("input.csv");
            Path output = tempDir.resolve("output.csv");

            // Write something to the output file first
            Files.writeString(output, "stale content", StandardCharsets.UTF_8);

            int code = Main.run(
                    new String[]{input.toString(), output.toString()},
                    new PrintStream(new ByteArrayOutputStream()));

            assertEquals(Main.EXIT_OK, code);
            String content = Files.readString(output, StandardCharsets.UTF_8);
            assertFalse(content.contains("stale"),
                    "Old content must be replaced");
            assertTrue(content.startsWith(CsvStandingsWriter.HEADER),
                    "Output must start with the standings header");
        }
    }

    // -------------------------------------------------------------------------
    // Empty input
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("empty input")
    class EmptyInput {

        @Test
        @DisplayName("header-only input produces output with only the header")
        void headerOnlyInputProducesHeaderOnlyOutput() throws Exception {
            Path input = tempDir.resolve("empty.csv");
            Files.writeString(input,
                    "home_team,away_team,home_goals,away_goals\n",
                    StandardCharsets.UTF_8);
            Path output = tempDir.resolve("output.csv");

            int code = Main.run(
                    new String[]{input.toString(), output.toString()},
                    new PrintStream(new ByteArrayOutputStream()));

            assertEquals(Main.EXIT_OK, code);
            List<String> lines = Files.readAllLines(output, StandardCharsets.UTF_8);
            assertEquals(1, lines.size(), "Only the header should be written");
            assertEquals(CsvStandingsWriter.HEADER, lines.get(0));
        }
    }
}
