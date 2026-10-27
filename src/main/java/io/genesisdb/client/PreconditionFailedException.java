package io.genesisdb.client;

import com.fasterxml.jackson.databind.JsonNode;

/** A commit was rejected because a precondition did not hold (412). Nothing was written. */
public class PreconditionFailedException extends GenesisDbException {
    private final String preconditionType;
    private final String reason;

    public PreconditionFailedException(String operation, String body) {
        super(operation, 412, body);
        String type = "";
        String message = body == null ? "" : body.trim();
        try {
            JsonNode parsed = Json.MAPPER.readTree(body);
            if (parsed != null && parsed.isObject()) {
                type = parsed.path("type").asText("");
                message = parsed.path("message").asText(message);
            }
        } catch (Exception ignored) {
            // keep the raw body
        }
        this.preconditionType = type;
        this.reason = message;
    }

    /** The precondition type that failed, for example "isSubjectNew". */
    public String getPreconditionType() {
        return preconditionType;
    }

    /** The server's explanation. */
    public String getReason() {
        return reason;
    }
}
