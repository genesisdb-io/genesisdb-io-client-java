package io.genesisdb.client;

/**
 * Exception thrown when Genesis DB API operations fail.
 */
public class GenesisDbException extends RuntimeException {
    private final int statusCode;
    private final String statusText;

    /**
     * Creates a new GenesisDbException.
     *
     * @param statusCode the HTTP status code
     * @param statusText the HTTP status text
     */
    public GenesisDbException(int statusCode, String statusText) {
        super("API Error: " + statusCode + " " + statusText);
        this.statusCode = statusCode;
        this.statusText = statusText;
    }

    /**
     * Creates a new GenesisDbException with a message.
     *
     * @param message the error message
     */
    public GenesisDbException(String message) {
        super(message);
        this.statusCode = 0;
        this.statusText = null;
    }

    /**
     * Creates a new GenesisDbException with a cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public GenesisDbException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
        this.statusText = null;
    }

    /**
     * Gets the HTTP status code.
     *
     * @return the status code, or 0 if not applicable
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Gets the HTTP status text.
     *
     * @return the status text, or null if not applicable
     */
    public String getStatusText() {
        return statusText;
    }

    /**
     * Checks if this exception represents a precondition failure (HTTP 412).
     *
     * @return true if this is a precondition failure
     */
    public boolean isPreconditionFailed() {
        return statusCode == 412;
    }
}
