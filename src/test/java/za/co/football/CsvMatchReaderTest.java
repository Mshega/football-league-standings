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
 * Unit tests for {@link CsvMatchReader}.
 *
 * <p>All file I/O uses {@code @TempDir} — JUnit 5 creates a temporary directory
 * before each test class and deletes it (and everything inside it) afterwards.
 * No temporary files are left on disk after the test run.
 *
 * <p>Each test writes a small CSV string to a temp file and calls
 * {@link CsvMatchReader#read(Path)}, then asserts the result or the exception.
 */
@DisplayName("CsvMatchReader")
class CsvMatchReaderTest {

    @TempDir
    Path tempDir;

    // -------------------------------------------------------------------------
    // Helper: write a string to a temp file and return its Path
    // -------------------------------------------------------------------------

    private Path writeCsv(String content) throws Exception {
        Path file = tempDir.resolve("test.csv");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("valid input")
    class ValidInput {

        @Test
        @DisplayName("reads a file with one data row")
        void readsOneRow() throws Exception {
            Path csv = writeCsv(
                    "home_team,away_team,home_goals,away_goals\n" +
                    "Arsenal,Chelsea,2,1\n");

            List<MatchResult> results = CsvMatchReader.read(csv);

            assertEquals(1, results.size());
            MatchResult m = results.get(0);
            assertEquals("Arsenal", m.homeTeam());
            assertEquals("Chelsea", m.awayTeam());
            assertEquals(2, m.homeGoals());
            assertEquals(1, m.awayGoals());
        }

        @Test
        @DisplayName("reads multiple data rows in order")
        void readsMultipleRows() throws Exception {
            Path csv = writeCsv(
                    "home_team,away_team,home_goals,away_goals\n" +
                    "Arsenal,Chelsea,2,1\n" +
                    "Leeds United,Liverpool,0,0\n" +
                    "Ipswich Town,Stoke City,3,2\n");

            List<MatchResult> results = CsvMatchReader.read(csv);

            assertEquals(3, results.size());
            assertEquals("Arsenal",      results.get(0).homeTeam());
            assertEquals("Leeds United", results.get(1).homeTeam());
            assertEquals("Ipswich Town", results.get(2).homeTeam());
        }

        @Test
        @DisplayName("header is case-insensitive")
        void headerIsCaseInsensitive() throws Exception {
            Path csv = writeCsv(
                    "HOME_TEAM,AWAY_TEAM,HOME_GOALS,AWAY_GOALS\n" +
                    "Arsenal,Chelsea,1,0\n");

            assertDoesNotThrow(() -> CsvMatchReader.read(csv));
        }

        @Test
        @DisplayName("blank lines anywhere are silently skipped")
        void blankLinesSkipped() throws Exception {
            Path csv = writeCsv(
                    "\n" +
                    "home_team,away_team,home_goals,away_goals\n" +
                    "\n" +
                    "Arsenal,Chelsea,2,1\n" +
                    "\n");

            List<MatchResult> results = CsvMatchReader.read(csv);
            assertEquals(1, results.size());
        }

        @Test
        @DisplayName("field whitespace is trimmed")
        void fieldWhitespaceIsTrimmed() throws Exception {
            Path csv = writeCsv(
                    "home_team,away_team,home_goals,away_goals\n" +
                    "  Arsenal  ,  Chelsea  , 2 , 1 \n");

            List<MatchResult> results = CsvMatchReader.read(csv);
            assertEquals("Arsenal", results.get(0).homeTeam());
            assertEquals("Chelsea", results.get(0).awayTeam());
        }

        @Test
        @DisplayName("returns an empty list when file contains only the header")
        void headerOnlyFileReturnsEmptyList() throws Exception {
            Path csv = writeCsv("home_team,away_team,home_goals,away_goals\n");
            List<MatchResult> results = CsvMatchReader.read(csv);
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("truly empty file returns an empty list")
        void emptyFileReturnsEmptyList() throws Exception {
            Path csv = writeCsv("");
            List<MatchResult> results = CsvMatchReader.read(csv);
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("goalless draw (0-0) is valid")
        void goallessDrawIsValid() throws Exception {
            Path csv = writeCsv(
                    "home_team,away_team,home_goals,away_goals\n" +
                    "Arsenal,Chelsea,0,0\n");

            List<MatchResult> results = CsvMatchReader.read(csv);
            assertEquals(0, results.get(0).homeGoals());
            assertEquals(0, results.get(0).awayGoals());
        }

        @Test
        @DisplayName("returned list is unmodifiable")
        void returnedListIsUnmodifiable() throws Exception {
            Path csv = writeCsv(
                    "home_team,away_team,home_goals,away_goals\n" +
                    "Arsenal,Chelsea,1,0\n");

            List<MatchResult> results = CsvMatchReader.read(csv);
            assertThrows(UnsupportedOperationException.class,
                    () -> results.add(new MatchResult("X", "Y", 0, 0)));
        }
    }

    // -------------------------------------------------------------------------
    // Quoted fields
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("quoted fields (RFC 4180)")
    class QuotedFields {

        @Test
        @DisplayName("quoted team name containing a comma is parsed correctly")
        void quotedNameWithComma() throws Exception {
            Path csv = writeCsv(
                    "home_team,away_team,home_goals,away_goals\n" +
                    "\"Notts, County\",Chelsea,1,0\n");

            List<MatchResult> results = CsvMatchReader.read(csv);
            assertEquals("Notts, County", results.get(0).homeTeam());
        }

        @Test
        @DisplayName("escaped double-quote inside a quoted field becomes a single quote")
        void escapedQuoteInsideQuotedField() throws Exception {
            Path csv = writeCsv(
                    "home_team,away_team,home_goals,away_goals\n" +
                    "\"Say \"\"hello\"\" FC\",Chelsea,1,0\n");

            List<MatchResult> results = CsvMatchReader.read(csv);
            assertEquals("Say \"hello\" FC", results.get(0).homeTeam());
        }

        @Test
        @DisplayName("unquoted field still works when others are quoted")
        void mixedQuotedAndUnquotedFields() throws Exception {
            Path csv = writeCsv(
                    "home_team,away_team,home_goals,away_goals\n" +
                    "Arsenal,\"Leeds United\",2,0\n");

            List<MatchResult> results = CsvMatchReader.read(csv);
            assertEquals("Arsenal",      results.get(0).homeTeam());
            assertEquals("Leeds United", results.get(0).awayTeam());
        }
    }

    // -------------------------------------------------------------------------
    // parseCsvLine unit tests (package-private method tested directly)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("parseCsvLine (internal parser)")
    class ParseCsvLine {

        @Test
        @DisplayName("simple unquoted row splits on commas")
        void simpleUnquotedRow() {
            List<String> fields = CsvMatchReader.parseCsvLine("a,b,c,d", 1);
            assertEquals(List.of("a", "b", "c", "d"), fields);
        }

        @Test
        @DisplayName("quoted field with comma is kept as one field")
        void quotedFieldWithComma() {
            List<String> fields = CsvMatchReader.parseCsvLine("\"a,b\",c", 1);
            assertEquals(List.of("a,b", "c"), fields);
        }

        @Test
        @DisplayName("escaped double-quote inside quoted field")
        void escapedDoubleQuote() {
            List<String> fields = CsvMatchReader.parseCsvLine("\"a\"\"b\",c", 1);
            assertEquals(List.of("a\"b", "c"), fields);
        }

        @Test
        @DisplayName("empty field between commas")
        void emptyFieldBetweenCommas() {
            List<String> fields = CsvMatchReader.parseCsvLine("a,,c", 1);
            assertEquals(List.of("a", "", "c"), fields);
        }

        @Test
        @DisplayName("character after closing quote that is not comma throws")
        void charAfterClosingQuoteThrows() {
            assertThrows(CsvParseException.class,
                    () -> CsvMatchReader.parseCsvLine("\"a\"X,b", 1));
        }

        @Test
        @DisplayName("multi-line quoted field is not supported — produces CsvParseException")
        void multilineQuotedFieldIsNotSupported() throws Exception {
            // Documented limitation: a quoted field containing an embedded newline
            // spans two physical lines. readLine() splits it, so the second fragment
            // becomes its own row and fails with a column-count or parse error.
            // This test documents the observable failure mode rather than asserting
            // that multi-line fields are silently accepted.
            Path csv = writeCsv(
                    "home_team,away_team,home_goals,away_goals\n" +
                    "\"Line1\nLine2\",Chelsea,1,0\n");  // embedded newline in team name

            // The reader will see row 2 as: '"Line1'  (3 fields after split)
            // and row 3 as: 'Line2",Chelsea,1,0' (4 fields but first is malformed)
            // Either way a CsvParseException is expected, not a silent wrong parse.
            assertThrows(CsvParseException.class, () -> CsvMatchReader.read(csv),
                    "Multi-line quoted fields are not supported; a CsvParseException must be thrown");
        }
    }

    // -------------------------------------------------------------------------
    // Error cases
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("error cases")
    class ErrorCases {

        @Test
        @DisplayName("null path throws IllegalArgumentException")
        void nullPathThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> CsvMatchReader.read(null));
        }

        @Test
        @DisplayName("missing header throws CsvParseException")
        void missingHeaderThrows() throws Exception {
            Path csv = writeCsv("Arsenal,Chelsea,2,1\n");
            CsvParseException ex = assertThrows(CsvParseException.class,
                    () -> CsvMatchReader.read(csv));
            assertEquals(1, ex.getLineNumber());
        }

        @Test
        @DisplayName("wrong header text throws CsvParseException")
        void wrongHeaderThrows() throws Exception {
            Path csv = writeCsv("team1,team2,score1,score2\nArsenal,Chelsea,2,1\n");
            CsvParseException ex = assertThrows(CsvParseException.class,
                    () -> CsvMatchReader.read(csv));
            assertEquals(1, ex.getLineNumber());
        }

        @Test
        @DisplayName("row with too few columns throws CsvParseException with correct line number")
        void tooFewColumnsThrows() throws Exception {
            Path csv = writeCsv(
                    "home_team,away_team,home_goals,away_goals\n" +
                    "Arsenal,Chelsea,2\n");   // only 3 columns

            CsvParseException ex = assertThrows(CsvParseException.class,
                    () -> CsvMatchReader.read(csv));
            assertEquals(2, ex.getLineNumber(),
                    "Error should be reported on line 2 (the bad data row)");
        }

        @Test
        @DisplayName("row with too many columns throws CsvParseException")
        void tooManyColumnsThrows() throws Exception {
            Path csv = writeCsv(
                    "home_team,away_team,home_goals,away_goals\n" +
                    "Arsenal,Chelsea,2,1,extra\n");  // 5 columns

            CsvParseException ex = assertThrows(CsvParseException.class,
                    () -> CsvMatchReader.read(csv));
            assertEquals(2, ex.getLineNumber());
        }

        @Test
        @DisplayName("non-numeric home_goals throws CsvParseException")
        void nonNumericHomeGoalsThrows() throws Exception {
            Path csv = writeCsv(
                    "home_team,away_team,home_goals,away_goals\n" +
                    "Arsenal,Chelsea,two,1\n");

            CsvParseException ex = assertThrows(CsvParseException.class,
                    () -> CsvMatchReader.read(csv));
            assertEquals(2, ex.getLineNumber());
            assertTrue(ex.getMessage().contains("home_goals"));
        }

        @Test
        @DisplayName("non-numeric away_goals throws CsvParseException")
        void nonNumericAwayGoalsThrows() throws Exception {
            Path csv = writeCsv(
                    "home_team,away_team,home_goals,away_goals\n" +
                    "Arsenal,Chelsea,1,one\n");

            CsvParseException ex = assertThrows(CsvParseException.class,
                    () -> CsvMatchReader.read(csv));
            assertEquals(2, ex.getLineNumber());
            assertTrue(ex.getMessage().contains("away_goals"));
        }

        @Test
        @DisplayName("negative home_goals throws CsvParseException")
        void negativeHomeGoalsThrows() throws Exception {
            Path csv = writeCsv(
                    "home_team,away_team,home_goals,away_goals\n" +
                    "Arsenal,Chelsea,-1,0\n");

            assertThrows(CsvParseException.class, () -> CsvMatchReader.read(csv));
        }

        @Test
        @DisplayName("negative away_goals throws CsvParseException")
        void negativeAwayGoalsThrows() throws Exception {
            Path csv = writeCsv(
                    "home_team,away_team,home_goals,away_goals\n" +
                    "Arsenal,Chelsea,0,-1\n");

            assertThrows(CsvParseException.class, () -> CsvMatchReader.read(csv));
        }

        @Test
        @DisplayName("same team as home and away throws CsvParseException")
        void sameTeamThrows() throws Exception {
            Path csv = writeCsv(
                    "home_team,away_team,home_goals,away_goals\n" +
                    "Arsenal,Arsenal,1,1\n");

            CsvParseException ex = assertThrows(CsvParseException.class,
                    () -> CsvMatchReader.read(csv));
            assertEquals(2, ex.getLineNumber());
        }

        @Test
        @DisplayName("error on line 4 while lines 2 and 3 are valid")
        void errorLineNumberIsAccurate() throws Exception {
            Path csv = writeCsv(
                    "home_team,away_team,home_goals,away_goals\n" +
                    "Arsenal,Chelsea,2,1\n" +
                    "Leeds United,Liverpool,0,0\n" +
                    "Ipswich Town,Stoke City,bad,2\n");

            CsvParseException ex = assertThrows(CsvParseException.class,
                    () -> CsvMatchReader.read(csv));
            assertEquals(4, ex.getLineNumber());
        }
    }
}
