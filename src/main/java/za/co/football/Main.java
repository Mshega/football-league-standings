package za.co.football;

import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

/**
 * Command-line entry point for the Football League Standings calculator.
 *
 * <h2>Usage</h2>
 * <pre>
 *   java -jar football-league-standings.jar &lt;input.csv&gt; &lt;output.csv&gt;
 * </pre>
 *
 * <h2>Arguments</h2>
 * <ol>
 *   <li>{@code input.csv}  — path to a CSV file containing match results
 *       (see {@link CsvMatchReader} for the expected format).</li>
 *   <li>{@code output.csv} — path where the calculated standings CSV will be
 *       written. The parent directory is created automatically if it does not
 *       yet exist.</li>
 * </ol>
 *
 * <h2>Exit codes</h2>
 * <ul>
 *   <li>{@code 0} — success</li>
 *   <li>{@code 1} — wrong number of arguments</li>
 *   <li>{@code 2} — input file not found</li>
 *   <li>{@code 3} — malformed CSV input</li>
 *   <li>{@code 4} — I/O error reading input or writing output</li>
 *   <li>{@code 5} — unexpected error</li>
 * </ul>
 *
 * <h2>Testability</h2>
 * The actual work is done by {@link #run(String[], PrintStream)}, which returns
 * an integer exit code and accepts an injectable error stream. {@link #main}
 * calls {@link #run} and passes the result to {@link System#exit}.
 * Tests call {@link #run} directly — no {@code System.exit} in tests.
 */
public class Main {

    // Exit codes — named constants so they are self-documenting in tests.
    static final int EXIT_OK              = 0;
    static final int EXIT_BAD_ARGS        = 1;
    static final int EXIT_FILE_NOT_FOUND  = 2;
    static final int EXIT_BAD_CSV         = 3;
    static final int EXIT_IO_ERROR        = 4;
    static final int EXIT_UNEXPECTED      = 5;

    /** Usage message printed to stderr on incorrect invocation. */
    private static final String USAGE =
            "Usage: java -jar football-league-standings.jar <input.csv> <output.csv>\n" +
            "\n" +
            "  input.csv   Match results CSV (home_team,away_team,home_goals,away_goals)\n" +
            "  output.csv  Destination for the calculated standings\n" +
            "\n" +
            "The output parent directory is created automatically if it does not exist.";

    /** Entry point — delegates to run() and exits with its return code. */
    public static void main(String[] args) {
        System.exit(run(args, System.err));
    }

    /**
     * Executes the full pipeline and returns an exit code.
     *
     * <p>Separating this from {@link #main} allows tests to call it without
     * triggering {@link System#exit}.
     *
     * @param args command-line arguments (expected: two paths)
     * @param err  stream for error messages (use {@code System.err} in production,
     *             a test-controlled stream in tests)
     * @return exit code (one of the {@code EXIT_*} constants)
     */
    static int run(String[] args, PrintStream err) {
        // ------------------------------------------------------------------ //
        // 1. Argument validation
        // ------------------------------------------------------------------ //
        if (args == null || args.length != 2) {
            err.println("Error: expected exactly 2 arguments but got "
                    + (args == null ? 0 : args.length) + ".");
            err.println();
            err.println(USAGE);
            return EXIT_BAD_ARGS;
        }

        Path inputPath  = Path.of(args[0]);
        Path outputPath = Path.of(args[1]);

        // ------------------------------------------------------------------ //
        // 2. Input file existence check (early, clear error before trying to read)
        // ------------------------------------------------------------------ //
        if (!Files.exists(inputPath)) {
            err.println("Error: input file not found: " + inputPath.toAbsolutePath());
            return EXIT_FILE_NOT_FOUND;
        }

        // ------------------------------------------------------------------ //
        // 3. Create output parent directory if needed
        // ------------------------------------------------------------------ //
        Path outputParent = outputPath.toAbsolutePath().getParent();
        if (outputParent != null && !Files.exists(outputParent)) {
            try {
                Files.createDirectories(outputParent);
            } catch (Exception e) {
                err.println("Error: could not create output directory: "
                        + outputParent + " — " + e.getMessage());
                return EXIT_IO_ERROR;
            }
        }

        // ------------------------------------------------------------------ //
        // 4. Read → calculate → write
        // ------------------------------------------------------------------ //
        try {
            List<MatchResult>  results   = CsvMatchReader.read(inputPath);
            List<TeamStanding> standings = StandingsCalculator.calculate(results);
            CsvStandingsWriter.write(standings, outputPath);

            // Success summary to stdout (not stderr).
            System.out.println("Standings calculated: " + standings.size()
                    + " team(s) written to " + outputPath.toAbsolutePath());
            return EXIT_OK;

        } catch (CsvParseException e) {
            err.println("Error: malformed input CSV — " + e.getMessage());
            return EXIT_BAD_CSV;

        } catch (UncheckedIOException e) {
            Throwable cause = e.getCause();
            if (cause instanceof NoSuchFileException) {
                err.println("Error: input file not found: " + inputPath.toAbsolutePath());
                return EXIT_FILE_NOT_FOUND;
            }
            err.println("Error: I/O failure — " + e.getMessage());
            return EXIT_IO_ERROR;

        } catch (Exception e) {
            err.println("Unexpected error: " + e.getMessage());
            return EXIT_UNEXPECTED;
        }
    }
}
