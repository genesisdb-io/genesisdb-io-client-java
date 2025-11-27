package io.genesisdb.client;

import java.util.Map;

/**
 * Represents a precondition to be checked before committing events.
 */
public class Precondition {
    private final String type;
    private final Map<String, Object> payload;

    private Precondition(String type, Map<String, Object> payload) {
        this.type = type;
        this.payload = payload;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    /**
     * Creates an isSubjectNew precondition.
     * Ensures that a subject is new (has no existing events).
     *
     * @param subject the subject to check
     * @return the precondition
     */
    public static Precondition isSubjectNew(String subject) {
        return new Precondition("isSubjectNew", Map.of("subject", subject));
    }

    /**
     * Creates an isSubjectExisting precondition.
     * Ensures that events exist for the specified subject.
     *
     * @param subject the subject to check
     * @return the precondition
     */
    public static Precondition isSubjectExisting(String subject) {
        return new Precondition("isSubjectExisting", Map.of("subject", subject));
    }

    /**
     * Creates an isQueryResultTrue precondition.
     * Evaluates a GDBQL query and ensures the result is truthy.
     *
     * @param query the GDBQL query to evaluate
     * @return the precondition
     */
    public static Precondition isQueryResultTrue(String query) {
        return new Precondition("isQueryResultTrue", Map.of("query", query));
    }
}
