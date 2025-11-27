package io.genesisdb.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GenesisDbExceptionTest {

    @Test
    void constructor_withStatusCodeAndText_createsException() {
        GenesisDbException exception = new GenesisDbException(404, "Not Found");

        assertEquals(404, exception.getStatusCode());
        assertEquals("Not Found", exception.getStatusText());
        assertEquals("API Error: 404 Not Found", exception.getMessage());
    }

    @Test
    void constructor_withMessage_createsException() {
        GenesisDbException exception = new GenesisDbException("Something went wrong");

        assertEquals(0, exception.getStatusCode());
        assertNull(exception.getStatusText());
        assertEquals("Something went wrong", exception.getMessage());
    }

    @Test
    void constructor_withMessageAndCause_createsException() {
        RuntimeException cause = new RuntimeException("Root cause");
        GenesisDbException exception = new GenesisDbException("Error occurred", cause);

        assertEquals(0, exception.getStatusCode());
        assertNull(exception.getStatusText());
        assertEquals("Error occurred", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void isPreconditionFailed_withStatus412_returnsTrue() {
        GenesisDbException exception = new GenesisDbException(412, "Precondition Failed");

        assertTrue(exception.isPreconditionFailed());
    }

    @Test
    void isPreconditionFailed_withOtherStatus_returnsFalse() {
        GenesisDbException exception400 = new GenesisDbException(400, "Bad Request");
        GenesisDbException exception404 = new GenesisDbException(404, "Not Found");
        GenesisDbException exception500 = new GenesisDbException(500, "Internal Server Error");

        assertFalse(exception400.isPreconditionFailed());
        assertFalse(exception404.isPreconditionFailed());
        assertFalse(exception500.isPreconditionFailed());
    }

    @Test
    void isPreconditionFailed_withMessageConstructor_returnsFalse() {
        GenesisDbException exception = new GenesisDbException("Some error");

        assertFalse(exception.isPreconditionFailed());
    }

    @Test
    void commonHttpStatusCodes() {
        // Test various HTTP status codes that might be returned by the API
        GenesisDbException badRequest = new GenesisDbException(400, "Bad Request");
        GenesisDbException unauthorized = new GenesisDbException(401, "Unauthorized");
        GenesisDbException forbidden = new GenesisDbException(403, "Forbidden");
        GenesisDbException notFound = new GenesisDbException(404, "Not Found");
        GenesisDbException serverError = new GenesisDbException(500, "Internal Server Error");

        assertEquals(400, badRequest.getStatusCode());
        assertEquals(401, unauthorized.getStatusCode());
        assertEquals(403, forbidden.getStatusCode());
        assertEquals(404, notFound.getStatusCode());
        assertEquals(500, serverError.getStatusCode());
    }

    @Test
    void exceptionMessage_isFormattedCorrectly() {
        GenesisDbException exception = new GenesisDbException(503, "Service Unavailable");

        assertTrue(exception.getMessage().contains("503"));
        assertTrue(exception.getMessage().contains("Service Unavailable"));
        assertTrue(exception.getMessage().startsWith("API Error:"));
    }

    @Test
    void exception_canBeCaughtAsRuntimeException() {
        assertThrows(RuntimeException.class, () -> {
            throw new GenesisDbException(500, "Server Error");
        });
    }

    @Test
    void exception_preservesCauseStackTrace() {
        IOException ioException = new IOException("Network error");
        GenesisDbException exception = new GenesisDbException("Connection failed", ioException);

        assertNotNull(exception.getCause());
        assertEquals("Network error", exception.getCause().getMessage());
    }

    private static class IOException extends Exception {
        IOException(String message) {
            super(message);
        }
    }
}
