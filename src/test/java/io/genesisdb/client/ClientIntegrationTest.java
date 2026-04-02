package io.genesisdb.client;

import io.cloudevents.CloudEvent;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * Integration tests that run against a real GenesisDB instance.
 *
 * These tests are disabled by default. To run them, set the following environment variables:
 * - GENESISDB_API_URL: The GenesisDB API URL (e.g., http://localhost:8080)
 * - GENESISDB_API_VERSION: The API version (e.g., v1)
 * - GENESISDB_AUTH_TOKEN: A valid authentication token
 *
 * Run with: mvn test -Dtest=ClientIntegrationTest
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClientIntegrationTest {

    private static Client client;
    private static String testSubject;
    private static String testEventId;

    @BeforeAll
    static void setUp() {
        // Use environment variables if set, otherwise fall back to defaults for local testing
        String apiUrl = System.getenv("GENESISDB_API_URL");
        String apiVersion = System.getenv("GENESISDB_API_VERSION");
        String authToken = System.getenv("GENESISDB_AUTH_TOKEN");

        if (apiUrl != null && !apiUrl.isEmpty()
                && apiVersion != null && !apiVersion.isEmpty()
                && authToken != null && !authToken.isEmpty()) {
            client = new Client();
        } else {
            client = new Client(ClientConfig.builder()
                    .apiUrl("http://localhost:8080")
                    .apiVersion("v1")
                    .authToken("secret")
                    .build());
        }

        testSubject = "/integration-test/" + UUID.randomUUID();
    }

    @AfterAll
    static void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    @Order(1)
    void ping_returnsSuccessfully() {
        String response = client.ping();
        assertNotNull(response);
        System.out.println("Ping response: " + response);
    }

    @Test
    @Order(2)
    void audit_returnsSuccessfully() {
        String response = client.audit();
        assertNotNull(response);
        System.out.println("Audit response: " + response);
    }

    @Test
    @Order(3)
    void commitEvents_createsEvent() {
        List<EventData> events = List.of(
                EventData.builder()
                        .source("io.genesisdb.integration-test")
                        .subject(testSubject)
                        .type("io.genesisdb.integration-test.test-event-created")
                        .data(Map.of(
                                "testId", UUID.randomUUID().toString(),
                                "timestamp", System.currentTimeMillis(),
                                "message", "Integration test event"
                        ))
                        .build()
        );

        assertDoesNotThrow(() -> client.commitEvents(events));
    }

    @Test
    @Order(4)
    void streamEvents_retrievesCommittedEvents() {
        List<CloudEvent> events = client.streamEvents(testSubject);

        assertFalse(events.isEmpty());
        assertEquals(testSubject, events.get(0).getSubject());
        assertEquals("io.genesisdb.integration-test.test-event-created", events.get(0).getType());

        testEventId = events.get(0).getId();
        System.out.println("Retrieved event ID: " + testEventId);
    }

    @Test
    @Order(5)
    void commitEvents_withIsSubjectExisting_succeeds() {
        List<EventData> events = List.of(
                EventData.builder()
                        .source("io.genesisdb.integration-test")
                        .subject(testSubject)
                        .type("io.genesisdb.integration-test.test-event-updated")
                        .data(Map.of("update", "true"))
                        .build()
        );

        List<Precondition> preconditions = List.of(
                Precondition.isSubjectExisting(testSubject)
        );

        assertDoesNotThrow(() -> client.commitEvents(events, preconditions));
    }

    @Test
    @Order(6)
    void commitEvents_withIsSubjectNew_failsForExistingSubject() {
        String existingSubject = testSubject;

        List<EventData> events = List.of(
                EventData.builder()
                        .source("io.genesisdb.integration-test")
                        .subject(existingSubject)
                        .type("io.genesisdb.integration-test.should-fail")
                        .data(Map.of("test", "data"))
                        .build()
        );

        List<Precondition> preconditions = List.of(
                Precondition.isSubjectNew(existingSubject)
        );

        GenesisDbException exception = assertThrows(GenesisDbException.class,
                () -> client.commitEvents(events, preconditions));

        assertTrue(exception.isPreconditionFailed());
    }

    @Test
    @Order(7)
    void commitEvents_withIsSubjectNew_succeedsForNewSubject() {
        String newSubject = "/integration-test/new-" + UUID.randomUUID();

        List<EventData> events = List.of(
                EventData.builder()
                        .source("io.genesisdb.integration-test")
                        .subject(newSubject)
                        .type("io.genesisdb.integration-test.new-subject-created")
                        .data(Map.of("test", "data"))
                        .build()
        );

        List<Precondition> preconditions = List.of(
                Precondition.isSubjectNew(newSubject)
        );

        assertDoesNotThrow(() -> client.commitEvents(events, preconditions));
    }

    @Test
    @Order(8)
    void streamEvents_withLowerBound_returnsEventsAfterBound() {
        assumeTrue(testEventId != null, "No event ID from previous test");

        StreamOptions options = StreamOptions.builder()
                .lowerBound(testEventId)
                .includeLowerBoundEvent(false)
                .build();

        List<CloudEvent> events = client.streamEvents(testSubject, options);

        // Should return events after the first one (the update event)
        for (CloudEvent event : events) {
            assertNotEquals(testEventId, event.getId());
        }
    }

    @Test
    @Order(9)
    void streamEvents_withLatestByEventType_returnsLatestOnly() {
        // Commit multiple events of the same type
        String typeTestSubject = "/integration-test/latest-" + UUID.randomUUID();

        for (int i = 0; i < 3; i++) {
            client.commitEvents(List.of(
                    EventData.builder()
                            .source("io.genesisdb.integration-test")
                            .subject(typeTestSubject)
                            .type("io.genesisdb.integration-test.versioned-event")
                            .data(Map.of("version", i))
                            .build()
            ));
        }

        StreamOptions options = StreamOptions.builder()
                .latestByEventType("io.genesisdb.integration-test.versioned-event")
                .build();

        List<CloudEvent> events = client.streamEvents(typeTestSubject, options);

        // Should return only the latest event
        assertEquals(1, events.size());
    }

    @Test
    @Order(10)
    void queryEvents_returnsResults() {
        List<Map<String, Object>> results = client.queryEvents(
                "STREAM e FROM events WHERE e.subject UNDER '" + testSubject + "' LIMIT 10"
        );

        assertNotNull(results);
        System.out.println("Query returned " + results.size() + " results");
    }

    @Test
    @Order(11)
    void queryEvents_withProjection_returnsProjectedFields() {
        List<Map<String, Object>> results = client.queryEvents(
                "STREAM e FROM events WHERE e.subject == '" + testSubject + "' LIMIT 1 MAP { subject: e.subject, type: e.type }"
        );

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).containsKey("subject"));
        assertTrue(results.get(0).containsKey("type"));
    }

    @Test
    @Order(12)
    void commitEvents_withGdprOptions_createsReferenceData() {
        String gdprSubject = "/integration-test/gdpr-" + UUID.randomUUID();

        List<EventData> events = List.of(
                EventData.builder()
                        .source("io.genesisdb.integration-test")
                        .subject(gdprSubject)
                        .type("io.genesisdb.integration-test.user-created")
                        .data(Map.of(
                                "firstName", "John",
                                "lastName", "Doe",
                                "email", "john.doe@example.com"
                        ))
                        .options(EventOptions.builder()
                                .storeDataAsReference(true)
                                .build())
                        .build()
        );

        assertDoesNotThrow(() -> client.commitEvents(events));

        // Verify the event was created
        List<CloudEvent> streamedEvents = client.streamEvents(gdprSubject);
        assertFalse(streamedEvents.isEmpty());
    }

    @Test
    @Order(13)
    void eraseData_removesReferencedData() {
        String gdprSubject = "/integration-test/gdpr-erase-" + UUID.randomUUID();

        // Create event with reference data
        client.commitEvents(List.of(
                EventData.builder()
                        .source("io.genesisdb.integration-test")
                        .subject(gdprSubject)
                        .type("io.genesisdb.integration-test.user-created")
                        .data(Map.of("personalData", "sensitive"))
                        .options(EventOptions.builder()
                                .storeDataAsReference(true)
                                .build())
                        .build()
        ));

        // Erase the data
        assertDoesNotThrow(() -> client.eraseData(gdprSubject));

        System.out.println("GDPR erase completed for subject: " + gdprSubject);
    }

    @Test
    @Order(14)
    void observeEvents_receivesNewEvents() throws InterruptedException {
        String observeSubject = "/integration-test/observe-" + UUID.randomUUID();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<CloudEvent> receivedEvent = new AtomicReference<>();

        // Start observing
        Client.ObserveHandle handle = client.observeEvents(observeSubject, event -> {
            receivedEvent.set(event);
            latch.countDown();
        });

        // Wait a bit for connection to establish
        Thread.sleep(500);

        // Commit a new event
        client.commitEvents(List.of(
                EventData.builder()
                        .source("io.genesisdb.integration-test")
                        .subject(observeSubject)
                        .type("io.genesisdb.integration-test.observed-event")
                        .data(Map.of("observed", true))
                        .build()
        ));

        // Wait for the event to be received
        boolean received = latch.await(10, TimeUnit.SECONDS);
        handle.stop();

        assertTrue(received, "Event was not received within timeout");
        assertNotNull(receivedEvent.get());
        assertEquals("io.genesisdb.integration-test.observed-event", receivedEvent.get().getType());
    }

    @Test
    @Order(15)
    void streamEvents_withUpperBound_excludesEventsAfterBound() {
        String boundsSubject = "/integration-test/upper-bound-" + UUID.randomUUID();

        // Commit 3 events
        for (int i = 1; i <= 3; i++) {
            client.commitEvents(List.of(
                    EventData.builder()
                            .source("io.genesisdb.integration-test")
                            .subject(boundsSubject)
                            .type("io.genesisdb.integration-test.event-" + i)
                            .data(Map.of("order", i))
                            .build()
            ));
        }

        // Get all events to know the IDs
        List<CloudEvent> allEvents = client.streamEvents(boundsSubject);
        assertEquals(3, allEvents.size());

        // Stream with upper bound excluding the last event
        StreamOptions options = StreamOptions.builder()
                .upperBound(allEvents.get(2).getId())
                .includeUpperBoundEvent(false)
                .build();

        List<CloudEvent> bounded = client.streamEvents(boundsSubject, options);
        assertEquals(2, bounded.size());
        assertEquals(allEvents.get(0).getId(), bounded.get(0).getId());
        assertEquals(allEvents.get(1).getId(), bounded.get(1).getId());

        System.out.println("Upper bound streaming works correctly");
    }

    @Test
    @Order(16)
    void streamEvents_withUpperBoundIncluded_includesBoundEvent() {
        String boundsSubject = "/integration-test/upper-bound-incl-" + UUID.randomUUID();

        // Commit 3 events
        for (int i = 1; i <= 3; i++) {
            client.commitEvents(List.of(
                    EventData.builder()
                            .source("io.genesisdb.integration-test")
                            .subject(boundsSubject)
                            .type("io.genesisdb.integration-test.event-" + i)
                            .data(Map.of("order", i))
                            .build()
            ));
        }

        List<CloudEvent> allEvents = client.streamEvents(boundsSubject);
        assertEquals(3, allEvents.size());

        // Stream with upper bound including the bound event
        StreamOptions options = StreamOptions.builder()
                .upperBound(allEvents.get(1).getId())
                .includeUpperBoundEvent(true)
                .build();

        List<CloudEvent> bounded = client.streamEvents(boundsSubject, options);
        assertEquals(2, bounded.size());
        assertEquals(allEvents.get(1).getId(), bounded.get(1).getId());

        System.out.println("Upper bound inclusive streaming works correctly");
    }

    @Test
    @Order(17)
    void streamEvents_withBothBounds_returnsMiddleEvents() {
        String boundsSubject = "/integration-test/both-bounds-" + UUID.randomUUID();

        // Commit 3 events
        for (int i = 1; i <= 3; i++) {
            client.commitEvents(List.of(
                    EventData.builder()
                            .source("io.genesisdb.integration-test")
                            .subject(boundsSubject)
                            .type("io.genesisdb.integration-test.event-" + i)
                            .data(Map.of("order", i))
                            .build()
            ));
        }

        List<CloudEvent> allEvents = client.streamEvents(boundsSubject);
        assertEquals(3, allEvents.size());

        // Stream with both bounds excluding both
        StreamOptions options = StreamOptions.builder()
                .lowerBound(allEvents.get(0).getId())
                .includeLowerBoundEvent(false)
                .upperBound(allEvents.get(2).getId())
                .includeUpperBoundEvent(false)
                .build();

        List<CloudEvent> middle = client.streamEvents(boundsSubject, options);
        assertEquals(1, middle.size());
        assertEquals(allEvents.get(1).getId(), middle.get(0).getId());

        System.out.println("Both bounds streaming works correctly");
    }

    @Test
    @Order(18)
    void commitEvents_withGenericPrecondition_isQueryResultTrue() {
        String genericSubject = "/integration-test/generic-precondition-" + UUID.randomUUID();

        // Use Precondition.of() as the generic escape hatch — same as isQueryResultTrue but via of()
        assertDoesNotThrow(() -> client.commitEvents(
                List.of(
                        EventData.builder()
                                .source("io.genesisdb.integration-test")
                                .subject(genericSubject)
                                .type("io.genesisdb.integration-test.generic-test")
                                .data(Map.of("test", "data"))
                                .build()
                ),
                List.of(
                        Precondition.of("isSubjectNew", Map.of("subject", genericSubject))
                )
        ));

        // Verify the event was committed
        List<CloudEvent> events = client.streamEvents(genericSubject);
        assertEquals(1, events.size());

        System.out.println("Generic precondition (Precondition.of) works correctly");
    }

    @Test
    @Order(19)
    void commitEvents_withMultipleEvents_commitsAtomically() {
        String atomicSubject = "/integration-test/atomic-" + UUID.randomUUID();

        List<EventData> events = List.of(
                EventData.builder()
                        .source("io.genesisdb.integration-test")
                        .subject(atomicSubject)
                        .type("io.genesisdb.integration-test.event-1")
                        .data(Map.of("order", 1))
                        .build(),
                EventData.builder()
                        .source("io.genesisdb.integration-test")
                        .subject(atomicSubject)
                        .type("io.genesisdb.integration-test.event-2")
                        .data(Map.of("order", 2))
                        .build(),
                EventData.builder()
                        .source("io.genesisdb.integration-test")
                        .subject(atomicSubject)
                        .type("io.genesisdb.integration-test.event-3")
                        .data(Map.of("order", 3))
                        .build()
        );

        assertDoesNotThrow(() -> client.commitEvents(events));

        List<CloudEvent> streamedEvents = client.streamEvents(atomicSubject);
        assertEquals(3, streamedEvents.size());
    }

    @Test
    @Order(20)
    void commitEvents_withIsQueryResultTrue_precondition() {
        String querySubject = "/integration-test/query-precondition-" + UUID.randomUUID();
        String uniqueEmail = "test-" + UUID.randomUUID() + "@example.com";

        // Should succeed: no events with this email exist yet
        assertDoesNotThrow(() -> client.commitEvents(
                List.of(
                        EventData.builder()
                                .source("io.genesisdb.integration-test")
                                .subject(querySubject)
                                .type("io.genesisdb.integration-test.user-created")
                                .data(Map.of("email", uniqueEmail))
                                .build()
                ),
                List.of(
                        Precondition.isQueryResultTrue(
                                "STREAM e FROM events WHERE e.data.email == '" + uniqueEmail + "' MAP COUNT() == 0"
                        )
                )
        ));

        // Should fail: an event with this email now exists
        GenesisDbException exception = assertThrows(GenesisDbException.class,
                () -> client.commitEvents(
                        List.of(
                                EventData.builder()
                                        .source("io.genesisdb.integration-test")
                                        .subject(querySubject)
                                        .type("io.genesisdb.integration-test.user-created")
                                        .data(Map.of("email", uniqueEmail))
                                        .build()
                        ),
                        List.of(
                                Precondition.isQueryResultTrue(
                                        "STREAM e FROM events WHERE e.data.email == '" + uniqueEmail + "' MAP COUNT() == 0"
                                )
                        )
                ));

        assertTrue(exception.isPreconditionFailed());
        System.out.println("isQueryResultTrue precondition works correctly");
    }

    @Test
    @Order(100)
    void cleanup_completesSuccessfully() {
        // This test runs last to confirm all integration tests completed
        System.out.println("Integration tests completed successfully");
        System.out.println("Test subject used: " + testSubject);
    }
}
