package za.co.football;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads football match results from a UTF-8 CSV file and returns them as a
 * list of {@link MatchResult} objects.
 *
 * <h2>Expected file format</h2>
 * <pre>
 *   home_team,away_team,home_goals,away_goals
 *   Arsenal,Chelsea,2,1
 *   Leeds United,Liverpool,0,0
 *   "Notts County","Stoke City",1,2
 * </pre>
 *
 * <h2>Parsing rules</h2>
 * <ul>
 *   <li><strong>Header:</strong> the first non-blank line must be exactly
 *       {@code home_team,away_team,home_goals,away_goals} (case-insensitive,
 *       whitespace trimmed). A missing or wrong header raises
 *       {@link CsvParseException}.</li>
 *   <li><strong>Blank lines:</strong> silently skipped anywhere in the file.</li>
 *   <li><strong>Whitespace:</strong> each parsed field is trimmed before use.</li>
 *   <li><strong>Column count:</strong> exactly four columns are required per
 *       data row. Rows with more or fewer columns raise {@link CsvParseException}
 *       with the 1-based line number.</li>
 *   <li><strong>Goals:</strong> must be non-negative integers. Non-numeric or
 *       negative values raise {@link CsvParseException}.</li>
 *   <li><strong>Quoted fields (RFC 4180 subset):</strong> a field may be
 *       surrounded by {@code "..."} to allow commas inside team names.
 *       A literal double-quote inside a quoted field is written as {@code ""}.
 *       Example: {@code "O'Neill, Frank's XI"} or {@code "He said ""hello"""}.
 *       <em>Limitation:</em> embedded newlines inside quoted fields are not
 *       supported. Each logical CSV record must fit on a single line.</li>
 *   <li><strong>Encoding:</strong> UTF-8.</li>
 * </ul>
 *
 * <h2>No third-party CSV library</h2>
 * Parsing is hand-rolled to avoid adding dependencies for a well-scoped problem.
 * The only unsupported RFC 4180 feature is multi-line quoted fields, which
 * cannot occur in football team names.
 *
 * <h2>Usage</h2>
 * <pre>
 *   List&lt;MatchResult&gt; results = CsvMatchReader.read(Path.of("results.csv"));
 * </pre>
 */
public final class CsvMatchReader {

    /** Expected header — normalised to lower-case for comparison. */
    private static final String EXPECTED_HEADER = "home_team,away_team,home_goals,away_goals";

    private CsvMatchReader() {}

    /**
     * Reads all match results from the given CSV file.
     *
     * @param csvFile path to the CSV file; must exist and be readable
     * @return an unmodifiable list of {@link MatchResult} objects in file order;
     *         empty list if the file contains only a header (or only blank lines)
     * @throws UncheckedIOException if the file cannot be read
     * @throws CsvParseException    if any line does not conform to the expected format
     * @throws IllegalArgumentException if {@code csvFile} is null
     */
    public static List<MatchResult> read(Path csvFile) {
        if (csvFile == null) {
            throw new IllegalArgumentException("csvFile must not be null.");
        }

        List<MatchResult> results = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
            String rawLine;
            int lineNumber = 0;
            boolean headerSeen = false;

            while ((rawLine = reader.readLine()) != null) {
                lineNumber++;

                // Skip blank lines silently.
                if (rawLine.isBlank()) {
                    continue;
                }

                if (!headerSeen) {
                    // Validate the header.
                    String normalisedHeader = rawLine.trim().toLowerCase();
                    if (!normalisedHeader.equals(EXPECTED_HEADER)) {
                        throw new CsvParseException(lineNumber,
                                "invalid header. Expected \"" + EXPECTED_HEADER +
                                "\" but found \"" + rawLine.trim() + "\".");
                    }
                    headerSeen = true;
                    continue;
                }

                // Parse a data row.
                List<String> fields = parseCsvLine(rawLine, lineNumber);

                if (fields.size() != 4) {
                    throw new CsvParseException(lineNumber,
                            "expected 4 columns but found " + fields.size() + ".");
                }

                String homeTeam  = fields.get(0).trim();
                String awayTeam  = fields.get(1).trim();
                String homeGoalRaw = fields.get(2).trim();
                String awayGoalRaw = fields.get(3).trim();

                int homeGoals = parseGoals(homeGoalRaw, "home_goals", lineNumber);
                int awayGoals = parseGoals(awayGoalRaw, "away_goals", lineNumber);

                // MatchResult's own constructor validates team names and goals.
                // We let its IllegalArgumentException propagate as-is; the caller
                // will see a clear message (e.g. "A team cannot play against itself").
                try {
                    results.add(new MatchResult(homeTeam, awayTeam, homeGoals, awayGoals));
                } catch (IllegalArgumentException e) {
                    throw new CsvParseException(lineNumber, e.getMessage(), e);
                }
            }

            if (!headerSeen) {
                // File was entirely blank — treat it as having no header.
                // An empty file is allowed to return an empty list, but a file
                // with no recognisable header at all is an error.
                // If lineNumber == 0 the file was truly empty; that's fine.
                if (lineNumber > 0) {
                    throw new CsvParseException(lineNumber,
                            "file contains no valid header row.");
                }
            }

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read CSV file: " + csvFile, e);
        }

        return List.copyOf(results);  // unmodifiable snapshot
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Parses one CSV line into a list of field strings, respecting RFC 4180
     * double-quote rules.
     *
     * <p>A field is either:
     * <ul>
     *   <li>Unquoted: characters up to the next comma or end of line.</li>
     *   <li>Quoted: starts with {@code "}, ends with a closing {@code "} that
     *       is not doubled. A {@code ""} inside a quoted field becomes a single
     *       {@code "} in the result.</li>
     * </ul>
     *
     * @param line       the raw line to parse
     * @param lineNumber 1-based line number, for error messages
     * @return list of parsed field values (not yet trimmed)
     * @throws CsvParseException if quoting is malformed
     */
    static List<String> parseCsvLine(String line, int lineNumber) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int i = 0;
        int len = line.length();

        while (i < len) {
            char ch = line.charAt(i);

            if (ch == '"') {
                // Quoted field: consume until closing quote, handling "" escapes.
                i++; // skip opening quote
                while (i < len) {
                    char inner = line.charAt(i);
                    if (inner == '"') {
                        // Could be end of field or an escaped quote ("").
                        if (i + 1 < len && line.charAt(i + 1) == '"') {
                            current.append('"'); // escaped quote
                            i += 2;
                        } else {
                            i++; // closing quote — end of quoted field
                            break;
                        }
                    } else {
                        current.append(inner);
                        i++;
                    }
                }
                // After the closing quote, the next character must be a comma or EOL.
                if (i < len && line.charAt(i) != ',') {
                    throw new CsvParseException(lineNumber,
                            "unexpected character after closing quote at position " + i + ".");
                }
            } else if (ch == ',') {
                // Field separator — commit current field, start fresh.
                fields.add(current.toString());
                current.setLength(0);
                i++;
                // If the line ends with a comma, that's a trailing empty field.
                if (i == len) {
                    fields.add("");
                }
                continue;
            } else {
                current.append(ch);
                i++;
            }
        }

        // Commit the last field (even if empty for a line that is just ",,,").
        fields.add(current.toString());
        return fields;
    }

    /**
     * Parses a goal count string into a non-negative integer.
     *
     * @param raw        the raw string from the CSV field
     * @param fieldName  column name, for error messages
     * @param lineNumber 1-based line number, for error messages
     * @return the parsed goal count
     * @throws CsvParseException if the value is not a non-negative integer
     */
    private static int parseGoals(String raw, String fieldName, int lineNumber) {
        int value;
        try {
            value = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new CsvParseException(lineNumber,
                    fieldName + " is not a valid integer: \"" + raw + "\".", e);
        }
        if (value < 0) {
            throw new CsvParseException(lineNumber,
                    fieldName + " cannot be negative: " + value + ".");
        }
        return value;
    }
}
