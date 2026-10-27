package io.genesisdb.client;

/** A schema that already exists (409). */
public class ConflictException extends GenesisDbException {
    public ConflictException(String operation, String body) {
        super(operation, 409, body);
    }
}
