package io.genesisdb.client;

import com.fasterxml.jackson.databind.JsonNode;

/** The body of GET /api/v1/status. */
public final class Status {
    private final JsonNode raw;

    public Status(JsonNode raw) {
        this.raw = raw;
    }

    public String getVersion() {
        return raw.path("engine").path("version").asText();
    }

    public String getChannel() {
        return raw.path("engine").path("channel").asText();
    }

    public long getEventCount() {
        return raw.path("events").path("count").asLong();
    }

    public long getSubjectCount() {
        return raw.path("events").path("subjects").asLong();
    }

    public long getTypeCount() {
        return raw.path("events").path("types").asLong();
    }

    public String getStorageFormat() {
        return raw.path("storage").path("storageFormat").asText();
    }

    public boolean isReady() {
        return raw.path("storage").path("ready").asBoolean();
    }

    /** The whole document, for fields not mapped here (system, host). */
    public JsonNode getRaw() {
        return raw;
    }

    @Override
    public String toString() {
        return "Status{version=" + getVersion() + ", events=" + getEventCount() + ", ready=" + isReady() + "}";
    }
}
