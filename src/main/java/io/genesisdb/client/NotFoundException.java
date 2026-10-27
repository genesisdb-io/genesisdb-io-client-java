package io.genesisdb.client;

/** The subject has no events (404). */
public class NotFoundException extends GenesisDbException {
    public NotFoundException(String operation, String body) {
        super(operation, 404, body);
    }
}
