package io.genesisdb.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** An event to commit. ID, time, hashes and the remaining CloudEvents attributes are assigned by the server. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class NewEvent {
    private final String source;
    private final String subject;
    private final String type;
    private final Object data;
    private final CommitOptions options;

    /**
     * @param source  the producing system
     * @param subject must start with "/". A single segment such as "/customer" gets a generated ID appended
     * @param type    the event type
     * @param data    a JSON object: a Map, a JsonNode or any bean Jackson serialises to an object
     */
    public NewEvent(String source, String subject, String type, Object data) {
        this(source, subject, type, data, null);
    }

    public NewEvent(String source, String subject, String type, Object data, CommitOptions options) {
        this.source = source;
        this.subject = subject;
        this.type = type;
        this.data = data;
        this.options = options;
    }

    /** Store the payload as reference data, so it can be erased later without breaking the hash chain. */
    public NewEvent asReference() {
        return new NewEvent(source, subject, type, data, CommitOptions.STORE_AS_REFERENCE);
    }

    @JsonProperty("source")
    public String getSource() {
        return source;
    }

    @JsonProperty("subject")
    public String getSubject() {
        return subject;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("data")
    public Object getData() {
        return data;
    }

    @JsonProperty("options")
    public CommitOptions getOptions() {
        return options;
    }
}
