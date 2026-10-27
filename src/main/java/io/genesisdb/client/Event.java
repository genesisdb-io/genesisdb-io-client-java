package io.genesisdb.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.Objects;

/** An event as stored and returned by GenesisDB. The property names follow CloudEvents. */
public final class Event {
    private final String source;
    private final String subject;
    private final String type;
    private final String specversion;
    private final String id;
    private final OffsetDateTime time;
    private final String datacontenttype;
    private final String predecessorhash;
    private final JsonNode data;
    private final String hash;

    public Event(
            @JsonProperty("source") String source,
            @JsonProperty("subject") String subject,
            @JsonProperty("type") String type,
            @JsonProperty("specversion") String specversion,
            @JsonProperty("id") String id,
            @JsonProperty("time") OffsetDateTime time,
            @JsonProperty("datacontenttype") String datacontenttype,
            @JsonProperty("predecessorhash") String predecessorhash,
            @JsonProperty("data") JsonNode data,
            @JsonProperty("hash") String hash) {
        this.source = source;
        this.subject = subject;
        this.type = type;
        this.specversion = specversion == null ? "1.0" : specversion;
        this.id = id;
        this.time = time;
        this.datacontenttype = datacontenttype == null ? "application/json" : datacontenttype;
        this.predecessorhash = predecessorhash == null ? "" : predecessorhash;
        this.data = data == null ? Json.MAPPER.nullNode() : data;
        this.hash = hash == null ? "" : hash;
    }

    public String getSource() {
        return source;
    }

    public String getSubject() {
        return subject;
    }

    public String getType() {
        return type;
    }

    public String getSpecversion() {
        return specversion;
    }

    public String getId() {
        return id;
    }

    public OffsetDateTime getTime() {
        return time;
    }

    public String getDatacontenttype() {
        return datacontenttype;
    }

    public String getPredecessorhash() {
        return predecessorhash;
    }

    /** The payload as a JSON tree. */
    public JsonNode getData() {
        return data;
    }

    /** Decode the payload into a typed value. */
    public <T> T getDataAs(Class<T> type) {
        try {
            return Json.MAPPER.treeToValue(data, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("payload does not decode to " + type.getSimpleName(), e);
        }
    }

    public String getHash() {
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Event)) return false;
        Event other = (Event) o;
        return Objects.equals(id, other.id) && Objects.equals(hash, other.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, hash);
    }

    @Override
    public String toString() {
        return "Event{id=" + id + ", subject=" + subject + ", type=" + type + "}";
    }
}
