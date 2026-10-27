package io.genesisdb.client;

/** A read budget was hit, or an erase found no reference payloads (422). */
public class UnprocessableException extends GenesisDbException {
    public UnprocessableException(String operation, String body) {
        super(operation, 422, body);
    }
}
