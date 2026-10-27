package io.genesisdb.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;

/** A precondition that is checked atomically with the commit. Build one with the static factories. */
public final class Precondition {
    private final String type;
    private final Map<String, String> payload;

    private Precondition(String type, String key, String value) {
        this.type = type;
        this.payload = Collections.singletonMap(key, value);
    }

    /** Holds when neither the subject nor a descendant has events. */
    public static Precondition isSubjectNew(String subject) {
        return new Precondition("isSubjectNew", "subject", subject);
    }

    /** Holds when the subject or a descendant has events. */
    public static Precondition isSubjectExisting(String subject) {
        return new Precondition("isSubjectExisting", "subject", subject);
    }

    /** Holds when the GDBQL query result is true. */
    public static Precondition isQueryResultTrue(String query) {
        return new Precondition("isQueryResultTrue", "query", query);
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("payload")
    public Map<String, String> getPayload() {
        return payload;
    }
}
