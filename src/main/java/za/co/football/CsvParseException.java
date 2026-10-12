package za.co.football;

/**
 * Thrown when a CSV file does not conform to the expected format.
 *
 * <p>Every instance carries the 1-based line number at which the problem was
 * detected, making it straightforward to report useful error messages to the
 * user without needing a stack trace.
 *
 * <p>This is an unchecked exception (extends {@link RuntimeException}) so that
 * callers are not forced to wrap every read call in a try-catch. Code that
 * needs to distinguish a parse error from an I/O error can catch it explicitly.
 *
 * <p>Example message: {@code "Line 4: expected 4 columns but found 3"}
 */
public class CsvParseException extends RuntimeException {

    private final int lineNumber;

    /**
     * Constructs a new {@code CsvParseException} with a description and the
     * line number at which the error occurred.
     *
     * @param lineNumber 1-based line number in the source CSV file
     * @param message    human-readable description of the problem
     */
    public CsvParseException(int lineNumber, String message) {
        super("Line " + lineNumber + ": " + message);
        this.lineNumber = lineNumber;
    }

    /**
     * Constructs a {@code CsvParseException} wrapping an underlying cause.
     *
     * @param lineNumber 1-based line number in the source CSV file
     * @param message    human-readable description of the problem
     * @param cause      the underlying exception (e.g. {@link NumberFormatException})
     */
    public CsvParseException(int lineNumber, String message, Throwable cause) {
        super("Line " + lineNumber + ": " + message, cause);
        this.lineNumber = lineNumber;
    }

    /**
     * @return the 1-based line number in the source CSV file where the error
     *         was detected
     */
    public int getLineNumber() {
        return lineNumber;
    }
}
