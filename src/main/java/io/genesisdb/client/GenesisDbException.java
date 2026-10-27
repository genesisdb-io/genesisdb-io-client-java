package io.genesisdb.client;

/**
 * A non-2xx answer from GenesisDB, or a transport failure. Subclasses classify the status.
 */
public class GenesisDbException extends RuntimeException {
    private final String operation;
    private final int status;
    private final String body;

    public GenesisDbException(String operation, int status, String body) {
        super(operation + ": " + status + " " + (body == null || body.trim().isEmpty() ? "no response body" : body.trim()));
        this.operation = operation;
        this.status = status;
        this.body = body == null ? "" : body;
    }

    public GenesisDbException(String operation, String message, Throwable cause) {
        super(operation + ": " + message, cause);
        this.operation = operation;
        this.status = 0;
        this.body = "";
    }

    /** The client operation that failed, for example "commit". */
    public String getOperation() {
        return operation;
    }

    /** The HTTP status, 0 for client-side failures. */
    public int getStatus() {
        return status;
    }

    /** The response body as text. */
    public String getBody() {
        return body;
    }

    static GenesisDbException forResponse(String operation, int status, String body) {
        switch (status) {
            case 401:
            case 403:
                return new AuthenticationException(operation, status, body);
            case 404:
                return new NotFoundException(operation, body);
            case 409:
                return new ConflictException(operation, body);
            case 412:
                return new PreconditionFailedException(operation, body);
            case 422:
                return new UnprocessableException(operation, body);
            case 503:
                return new NotReadyException(operation, body);
            default:
                return new GenesisDbException(operation, status, body);
        }
    }
}
