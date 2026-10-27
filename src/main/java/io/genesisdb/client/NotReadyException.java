package io.genesisdb.client;

/** The store is rebuilding or recovering (503). */
public class NotReadyException extends GenesisDbException {
    public NotReadyException(String operation, String body) {
        super(operation, 503, body);
    }
}
