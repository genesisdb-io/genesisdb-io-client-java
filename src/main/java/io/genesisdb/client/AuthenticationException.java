package io.genesisdb.client;

/** The token was missing or wrong (401), or valid but not permitted for this operation (403). */
public class AuthenticationException extends GenesisDbException {
    public AuthenticationException(String operation, int status, String body) {
        super(operation, status, body);
    }
}
