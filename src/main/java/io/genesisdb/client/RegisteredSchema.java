package io.genesisdb.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/** One entry of the schema list. */
public final class RegisteredSchema {
    private final String type;
    private final JsonNode schema;

    public RegisteredSchema(@JsonProperty("type") String type, @JsonProperty("schema") JsonNode schema) {
        this.type = type;
        this.schema = schema;
    }

    public String getType() {
        return type;
    }

    public JsonNode getSchema() {
        return schema;
    }
}
