package io.genesisdb.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PreconditionTest {

    @Test
    void isSubjectNew_createsCorrectPrecondition() {
        Precondition precondition = Precondition.isSubjectNew("/user/456");

        assertEquals("isSubjectNew", precondition.getType());
        assertNotNull(precondition.getPayload());
        assertEquals("/user/456", precondition.getPayload().get("subject"));
    }

    @Test
    void isSubjectNew_withRootSubject() {
        Precondition precondition = Precondition.isSubjectNew("/");

        assertEquals("isSubjectNew", precondition.getType());
        assertEquals("/", precondition.getPayload().get("subject"));
    }

    @Test
    void isSubjectNew_withNestedSubject() {
        Precondition precondition = Precondition.isSubjectNew("/customer/fed2902d-0135-460d-8605-263a06308448/orders");

        assertEquals("isSubjectNew", precondition.getType());
        assertEquals("/customer/fed2902d-0135-460d-8605-263a06308448/orders", precondition.getPayload().get("subject"));
    }

    @Test
    void isSubjectExisting_createsCorrectPrecondition() {
        Precondition precondition = Precondition.isSubjectExisting("/user/456");

        assertEquals("isSubjectExisting", precondition.getType());
        assertNotNull(precondition.getPayload());
        assertEquals("/user/456", precondition.getPayload().get("subject"));
    }

    @Test
    void isSubjectExisting_withNestedSubject() {
        Precondition precondition = Precondition.isSubjectExisting("/customer/123/profile");

        assertEquals("isSubjectExisting", precondition.getType());
        assertEquals("/customer/123/profile", precondition.getPayload().get("subject"));
    }

    @Test
    void isQueryResultTrue_createsCorrectPrecondition() {
        String query = "STREAM e FROM events WHERE e.data.email == 'john.doe@example.com' MAP COUNT() == 0";
        Precondition precondition = Precondition.isQueryResultTrue(query);

        assertEquals("isQueryResultTrue", precondition.getType());
        assertNotNull(precondition.getPayload());
        assertEquals(query, precondition.getPayload().get("query"));
    }

    @Test
    void isQueryResultTrue_withComplexQuery() {
        String query = "STREAM e FROM events WHERE e.subject UNDER '/user/123' AND e.type == 'transaction-processed' AND e.time >= '2024-01-01T00:00:00Z' MAP SUM(e.data.amount) + 500 <= 10000";
        Precondition precondition = Precondition.isQueryResultTrue(query);

        assertEquals("isQueryResultTrue", precondition.getType());
        assertEquals(query, precondition.getPayload().get("query"));
    }

    @Test
    void isQueryResultTrue_withAggregationQuery() {
        String query = "STREAM e FROM events WHERE e.subject UNDER '/conference/2024/registrations' AND e.type == 'registration-created' GROUP BY e.data.ticketType HAVING e.data.ticketType == 'premium' MAP COUNT() < 50";
        Precondition precondition = Precondition.isQueryResultTrue(query);

        assertEquals("isQueryResultTrue", precondition.getType());
        assertEquals(query, precondition.getPayload().get("query"));
    }

    @Test
    void precondition_payloadIsImmutableConcept() {
        Precondition precondition = Precondition.isSubjectNew("/user/123");

        // Payload should contain expected values
        assertEquals(1, precondition.getPayload().size());
        assertEquals("/user/123", precondition.getPayload().get("subject"));
    }

    @Test
    void differentPreconditionTypes_areDistinct() {
        Precondition newPrecondition = Precondition.isSubjectNew("/user/123");
        Precondition existingPrecondition = Precondition.isSubjectExisting("/user/123");
        Precondition queryPrecondition = Precondition.isQueryResultTrue("COUNT() > 0");

        assertNotEquals(newPrecondition.getType(), existingPrecondition.getType());
        assertNotEquals(newPrecondition.getType(), queryPrecondition.getType());
        assertNotEquals(existingPrecondition.getType(), queryPrecondition.getType());
    }

    @Test
    void of_createsGenericPrecondition() {
        Precondition precondition = Precondition.of("someCustomFuturePrecondition",
                java.util.Map.of("foo", "bar", "baz", 123));

        assertEquals("someCustomFuturePrecondition", precondition.getType());
        assertEquals("bar", precondition.getPayload().get("foo"));
        assertEquals(123, precondition.getPayload().get("baz"));
    }

    @Test
    void of_withEmptyPayload_createsGenericPrecondition() {
        Precondition precondition = Precondition.of("customType", java.util.Map.of());

        assertEquals("customType", precondition.getType());
        assertTrue(precondition.getPayload().isEmpty());
    }
}
