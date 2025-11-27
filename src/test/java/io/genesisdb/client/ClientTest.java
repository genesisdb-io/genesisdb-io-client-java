package io.genesisdb.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.cloudevents.CloudEvent;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class ClientTest {

    private static WireMockServer wireMockServer;
    private Client client;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
        client = new Client(ClientConfig.builder()
                .apiUrl("http://localhost:" + wireMockServer.port())
                .apiVersion("v1")
                .authToken("secret")
                .build());
    }

    @AfterEach
    void tearDown() {
        client.close();
    }

    // ==================== Constructor Tests ====================

    @Test
    void constructor_withConfig_createsClient() {
        Client testClient = new Client(ClientConfig.builder()
                .apiUrl("http://localhost:8080")
                .apiVersion("v1")
                .authToken("secret")
                .build());

        assertNotNull(testClient);
        testClient.close();
    }

    @Test
    void constructor_withMissingConfig_throwsException() {
        assertThrows(IllegalStateException.class, () -> new Client(ClientConfig.builder().build()));
    }

    @Test
    void constructor_withPartialConfig_throwsException() {
        assertThrows(IllegalStateException.class, () ->
                new Client(ClientConfig.builder()
                        .apiUrl("http://localhost:8080")
                        .build()));
    }

    // ==================== streamEvents Tests ====================

    @Test
    void streamEvents_withSubject_returnsEvents() {
        String ndjsonResponse =
                "{\"specversion\":\"1.0\",\"id\":\"event-1\",\"source\":\"io.genesisdb.app\",\"type\":\"io.genesisdb.app.customer-added\",\"subject\":\"/customer\",\"time\":\"2024-01-01T00:00:00Z\",\"data\":{\"firstName\":\"Bruce\",\"lastName\":\"Wayne\"}}\n" +
                "{\"specversion\":\"1.0\",\"id\":\"event-2\",\"source\":\"io.genesisdb.app\",\"type\":\"io.genesisdb.app.customer-added\",\"subject\":\"/customer\",\"time\":\"2024-01-01T00:01:00Z\",\"data\":{\"firstName\":\"Alfred\",\"lastName\":\"Pennyworth\"}}\n";

        stubFor(post(urlEqualTo("/api/v1/stream"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/x-ndjson")
                        .withBody(ndjsonResponse)));

        List<CloudEvent> events = client.streamEvents("/customer");

        assertEquals(2, events.size());
        assertEquals("event-1", events.get(0).getId());
        assertEquals("event-2", events.get(1).getId());
        assertEquals("/customer", events.get(0).getSubject());

        verify(postRequestedFor(urlEqualTo("/api/v1/stream"))
                .withHeader("Authorization", equalTo("Bearer secret"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(containing("\"subject\":\"/customer\"")));
    }

    @Test
    void streamEvents_withLowerBound_sendsOptions() {
        stubFor(post(urlEqualTo("/api/v1/stream"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/x-ndjson")
                        .withBody("")));

        StreamOptions options = StreamOptions.builder()
                .lowerBound("2d6d4141-6107-4fb2-905f-445730f4f2a9")
                .includeLowerBoundEvent(true)
                .build();

        client.streamEvents("/", options);

        verify(postRequestedFor(urlEqualTo("/api/v1/stream"))
                .withRequestBody(containing("\"lowerBound\":\"2d6d4141-6107-4fb2-905f-445730f4f2a9\""))
                .withRequestBody(containing("\"includeLowerBoundEvent\":true")));
    }

    @Test
    void streamEvents_withLatestByEventType_sendsOptions() {
        stubFor(post(urlEqualTo("/api/v1/stream"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));

        StreamOptions options = StreamOptions.builder()
                .latestByEventType("io.genesisdb.app.customer-updated")
                .build();

        client.streamEvents("/", options);

        verify(postRequestedFor(urlEqualTo("/api/v1/stream"))
                .withRequestBody(containing("\"latestByEventType\":\"io.genesisdb.app.customer-updated\"")));
    }

    @Test
    void streamEvents_withEmptyResponse_returnsEmptyList() {
        stubFor(post(urlEqualTo("/api/v1/stream"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));

        List<CloudEvent> events = client.streamEvents("/customer");

        assertTrue(events.isEmpty());
    }

    @Test
    void streamEvents_withServerError_throwsException() {
        stubFor(post(urlEqualTo("/api/v1/stream"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withStatusMessage("Internal Server Error")));

        GenesisDbException exception = assertThrows(GenesisDbException.class,
                () -> client.streamEvents("/customer"));

        assertEquals(500, exception.getStatusCode());
    }

    // ==================== commitEvents Tests ====================

    @Test
    void commitEvents_withSingleEvent_sendsRequest() {
        stubFor(post(urlEqualTo("/api/v1/commit"))
                .willReturn(aResponse().withStatus(200)));

        client.commitEvents(List.of(
                EventData.builder()
                        .source("io.genesisdb.app")
                        .subject("/customer")
                        .type("io.genesisdb.app.customer-added")
                        .data(Map.of("firstName", "Bruce", "lastName", "Wayne"))
                        .build()
        ));

        verify(postRequestedFor(urlEqualTo("/api/v1/commit"))
                .withHeader("Authorization", equalTo("Bearer secret"))
                .withRequestBody(containing("\"source\":\"io.genesisdb.app\""))
                .withRequestBody(containing("\"subject\":\"/customer\""))
                .withRequestBody(containing("\"type\":\"io.genesisdb.app.customer-added\""))
                .withRequestBody(containing("\"firstName\":\"Bruce\"")));
    }

    @Test
    void commitEvents_withMultipleEvents_sendsAllEvents() {
        stubFor(post(urlEqualTo("/api/v1/commit"))
                .willReturn(aResponse().withStatus(200)));

        client.commitEvents(List.of(
                EventData.builder()
                        .source("io.genesisdb.app")
                        .subject("/customer")
                        .type("io.genesisdb.app.customer-added")
                        .data(Map.of("firstName", "Bruce"))
                        .build(),
                EventData.builder()
                        .source("io.genesisdb.app")
                        .subject("/customer")
                        .type("io.genesisdb.app.customer-added")
                        .data(Map.of("firstName", "Alfred"))
                        .build()
        ));

        verify(postRequestedFor(urlEqualTo("/api/v1/commit"))
                .withRequestBody(containing("Bruce"))
                .withRequestBody(containing("Alfred")));
    }

    @Test
    void commitEvents_withStoreDataAsReference_sendsOptions() {
        stubFor(post(urlEqualTo("/api/v1/commit"))
                .willReturn(aResponse().withStatus(200)));

        client.commitEvents(List.of(
                EventData.builder()
                        .source("io.genesisdb.app")
                        .subject("/user/456")
                        .type("io.genesisdb.app.user-created")
                        .data(Map.of("email", "john@example.com"))
                        .options(EventOptions.builder().storeDataAsReference(true).build())
                        .build()
        ));

        verify(postRequestedFor(urlEqualTo("/api/v1/commit"))
                .withRequestBody(containing("\"storeDataAsReference\":true")));
    }

    @Test
    void commitEvents_withIsSubjectNewPrecondition_sendsPrecondition() {
        stubFor(post(urlEqualTo("/api/v1/commit"))
                .willReturn(aResponse().withStatus(200)));

        client.commitEvents(
                List.of(
                        EventData.builder()
                                .source("io.genesisdb.app")
                                .subject("/user/456")
                                .type("io.genesisdb.app.user-created")
                                .data(Map.of("name", "John"))
                                .build()
                ),
                List.of(Precondition.isSubjectNew("/user/456"))
        );

        verify(postRequestedFor(urlEqualTo("/api/v1/commit"))
                .withRequestBody(containing("\"preconditions\""))
                .withRequestBody(containing("\"type\":\"isSubjectNew\""))
                .withRequestBody(containing("\"subject\":\"/user/456\"")));
    }

    @Test
    void commitEvents_withIsSubjectExistingPrecondition_sendsPrecondition() {
        stubFor(post(urlEqualTo("/api/v1/commit"))
                .willReturn(aResponse().withStatus(200)));

        client.commitEvents(
                List.of(
                        EventData.builder()
                                .source("io.genesisdb.app")
                                .subject("/user/456")
                                .type("io.genesisdb.app.user-updated")
                                .data(Map.of("name", "John Updated"))
                                .build()
                ),
                List.of(Precondition.isSubjectExisting("/user/456"))
        );

        verify(postRequestedFor(urlEqualTo("/api/v1/commit"))
                .withRequestBody(containing("\"type\":\"isSubjectExisting\"")));
    }

    @Test
    void commitEvents_withIsQueryResultTruePrecondition_sendsPrecondition() {
        stubFor(post(urlEqualTo("/api/v1/commit"))
                .willReturn(aResponse().withStatus(200)));

        String query = "STREAM e FROM events WHERE e.data.email == 'john@example.com' MAP COUNT() == 0";

        client.commitEvents(
                List.of(
                        EventData.builder()
                                .source("io.genesisdb.app")
                                .subject("/user/456")
                                .type("io.genesisdb.app.user-created")
                                .data(Map.of("email", "john@example.com"))
                                .build()
                ),
                List.of(Precondition.isQueryResultTrue(query))
        );

        verify(postRequestedFor(urlEqualTo("/api/v1/commit"))
                .withRequestBody(containing("\"type\":\"isQueryResultTrue\""))
                .withRequestBody(containing("\"query\"")));
    }

    @Test
    void commitEvents_withPreconditionFailed_throwsException() {
        stubFor(post(urlEqualTo("/api/v1/commit"))
                .willReturn(aResponse()
                        .withStatus(412)
                        .withStatusMessage("Precondition Failed")));

        GenesisDbException exception = assertThrows(GenesisDbException.class, () ->
                client.commitEvents(
                        List.of(
                                EventData.builder()
                                        .source("io.genesisdb.app")
                                        .subject("/user/456")
                                        .type("io.genesisdb.app.user-created")
                                        .data(Map.of("name", "John"))
                                        .build()
                        ),
                        List.of(Precondition.isSubjectNew("/user/456"))
                )
        );

        assertTrue(exception.isPreconditionFailed());
        assertEquals(412, exception.getStatusCode());
    }

    @Test
    void commitEvents_withMultiplePreconditions_sendsAllPreconditions() {
        stubFor(post(urlEqualTo("/api/v1/commit"))
                .willReturn(aResponse().withStatus(200)));

        client.commitEvents(
                List.of(
                        EventData.builder()
                                .source("io.genesisdb.app")
                                .subject("/user/456")
                                .type("io.genesisdb.app.user-created")
                                .data(Map.of("name", "John"))
                                .build()
                ),
                List.of(
                        Precondition.isSubjectNew("/user/456"),
                        Precondition.isQueryResultTrue("COUNT() == 0")
                )
        );

        verify(postRequestedFor(urlEqualTo("/api/v1/commit"))
                .withRequestBody(containing("isSubjectNew"))
                .withRequestBody(containing("isQueryResultTrue")));
    }

    // ==================== eraseData Tests ====================

    @Test
    void eraseData_withSubject_sendsRequest() {
        stubFor(post(urlEqualTo("/api/v1/erase"))
                .willReturn(aResponse().withStatus(200)));

        client.eraseData("/user/456");

        verify(postRequestedFor(urlEqualTo("/api/v1/erase"))
                .withHeader("Authorization", equalTo("Bearer secret"))
                .withRequestBody(containing("\"subject\":\"/user/456\"")));
    }

    @Test
    void eraseData_withServerError_throwsException() {
        stubFor(post(urlEqualTo("/api/v1/erase"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withStatusMessage("Not Found")));

        GenesisDbException exception = assertThrows(GenesisDbException.class,
                () -> client.eraseData("/nonexistent"));

        assertEquals(404, exception.getStatusCode());
    }

    // ==================== queryEvents Tests ====================

    @Test
    void queryEvents_withQuery_returnsResults() {
        String ndjsonResponse =
                "{\"subject\":\"/customer/1\",\"firstName\":\"Bruce\"}\n" +
                "{\"subject\":\"/customer/2\",\"firstName\":\"Alfred\"}\n";

        stubFor(post(urlEqualTo("/api/v1/q"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/x-ndjson")
                        .withBody(ndjsonResponse)));

        List<Map<String, Object>> results = client.queryEvents(
                "STREAM e FROM events WHERE e.type == \"io.genesisdb.app.customer-added\" ORDER BY e.time DESC LIMIT 20 MAP { subject: e.subject, firstName: e.data.firstName }"
        );

        assertEquals(2, results.size());
        assertEquals("/customer/1", results.get(0).get("subject"));
        assertEquals("Bruce", results.get(0).get("firstName"));
        assertEquals("Alfred", results.get(1).get("firstName"));

        verify(postRequestedFor(urlEqualTo("/api/v1/q"))
                .withRequestBody(containing("\"query\"")));
    }

    @Test
    void queryEvents_withEmptyResult_returnsEmptyList() {
        stubFor(post(urlEqualTo("/api/v1/q"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));

        List<Map<String, Object>> results = client.queryEvents("STREAM e FROM events LIMIT 0");

        assertTrue(results.isEmpty());
    }

    @Test
    void q_isAliasForQueryEvents() {
        String ndjsonResponse = "{\"count\":42}\n";

        stubFor(post(urlEqualTo("/api/v1/q"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(ndjsonResponse)));

        List<Map<String, Object>> results = client.q("STREAM e FROM events MAP COUNT()");

        assertEquals(1, results.size());
        assertEquals(42, results.get(0).get("count"));
    }

    // ==================== ping Tests ====================

    @Test
    void ping_returnsResponse() {
        stubFor(get(urlEqualTo("/api/v1/status/ping"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("pong")));

        String response = client.ping();

        assertEquals("pong", response);

        verify(getRequestedFor(urlEqualTo("/api/v1/status/ping"))
                .withHeader("Authorization", equalTo("Bearer secret")));
    }

    @Test
    void ping_withServerDown_throwsException() {
        stubFor(get(urlEqualTo("/api/v1/status/ping"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withStatusMessage("Service Unavailable")));

        GenesisDbException exception = assertThrows(GenesisDbException.class,
                () -> client.ping());

        assertEquals(503, exception.getStatusCode());
    }

    // ==================== audit Tests ====================

    @Test
    void audit_returnsResponse() {
        stubFor(get(urlEqualTo("/api/v1/status/audit"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("OK - All events consistent")));

        String response = client.audit();

        assertEquals("OK - All events consistent", response);

        verify(getRequestedFor(urlEqualTo("/api/v1/status/audit"))
                .withHeader("Authorization", equalTo("Bearer secret")));
    }

    @Test
    void audit_withInconsistency_returnsDetails() {
        stubFor(get(urlEqualTo("/api/v1/status/audit"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("WARNING - Found 2 inconsistent events")));

        String response = client.audit();

        assertTrue(response.contains("WARNING"));
    }

    // ==================== observeEvents Tests ====================

    @Test
    void observeEvents_receivesEvents() throws InterruptedException {
        String ndjsonResponse =
                "{\"specversion\":\"1.0\",\"id\":\"event-1\",\"source\":\"io.genesisdb.app\",\"type\":\"io.genesisdb.app.customer-added\",\"subject\":\"/customer\",\"time\":\"2024-01-01T00:00:00Z\",\"data\":{\"firstName\":\"Bruce\"}}\n";

        stubFor(post(urlEqualTo("/api/v1/observe"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/x-ndjson")
                        .withBody(ndjsonResponse)));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<CloudEvent> receivedEvent = new AtomicReference<>();

        Client.ObserveHandle handle = client.observeEvents("/customer", event -> {
            receivedEvent.set(event);
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNotNull(receivedEvent.get());
        assertEquals("event-1", receivedEvent.get().getId());

        handle.stop();
    }

    @Test
    void observeEvents_withOptions_sendsOptions() throws InterruptedException {
        stubFor(post(urlEqualTo("/api/v1/observe"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));

        StreamOptions options = StreamOptions.builder()
                .lowerBound("event-id-123")
                .includeLowerBoundEvent(true)
                .build();

        Client.ObserveHandle handle = client.observeEvents("/customer", options, event -> {});

        // Give it a moment to make the request
        Thread.sleep(100);
        handle.stop();

        verify(postRequestedFor(urlEqualTo("/api/v1/observe"))
                .withRequestBody(containing("\"lowerBound\":\"event-id-123\""))
                .withRequestBody(containing("\"includeLowerBoundEvent\":true")));
    }

    @Test
    void observeEvents_canBeStopped() throws InterruptedException {
        stubFor(post(urlEqualTo("/api/v1/observe"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));

        Client.ObserveHandle handle = client.observeEvents("/customer", event -> {});

        Thread.sleep(50);
        handle.stop();

        // Should not throw
        assertNotNull(handle.getFuture());
    }

    @Test
    void observeEvents_withServerError_throwsException() throws InterruptedException {
        stubFor(post(urlEqualTo("/api/v1/observe"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withStatusMessage("Internal Server Error")));

        Client.ObserveHandle handle = client.observeEvents("/customer", event -> {});

        // The exception will be thrown in the async task
        try {
            handle.getFuture().get(5, TimeUnit.SECONDS);
            fail("Expected exception");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof GenesisDbException);
        }
    }

    @Test
    void observeEvents_skipsHeartbeatMessages() throws InterruptedException {
        String ndjsonResponse =
                "{\"payload\":\"\"}\n" +
                "{\"specversion\":\"1.0\",\"id\":\"event-1\",\"source\":\"io.genesisdb.app\",\"type\":\"test\",\"subject\":\"/test\",\"time\":\"2024-01-01T00:00:00Z\",\"data\":{}}\n";

        stubFor(post(urlEqualTo("/api/v1/observe"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(ndjsonResponse)));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<CloudEvent> receivedEvent = new AtomicReference<>();

        Client.ObserveHandle handle = client.observeEvents("/test", event -> {
            receivedEvent.set(event);
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("event-1", receivedEvent.get().getId());

        handle.stop();
    }

    @Test
    void observeEvents_handlesSseFormat() throws InterruptedException {
        String sseResponse =
                "data: {\"specversion\":\"1.0\",\"id\":\"event-1\",\"source\":\"io.genesisdb.app\",\"type\":\"test\",\"subject\":\"/test\",\"time\":\"2024-01-01T00:00:00Z\",\"data\":{}}\n";

        stubFor(post(urlEqualTo("/api/v1/observe"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(sseResponse)));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<CloudEvent> receivedEvent = new AtomicReference<>();

        Client.ObserveHandle handle = client.observeEvents("/test", event -> {
            receivedEvent.set(event);
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("event-1", receivedEvent.get().getId());

        handle.stop();
    }

    // ==================== Authorization Tests ====================

    @Test
    void allEndpoints_sendAuthorizationHeader() {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody("")));

        client.streamEvents("/test");
        client.commitEvents(List.of(
                EventData.builder()
                        .source("test")
                        .subject("/test")
                        .type("test")
                        .build()
        ));
        client.eraseData("/test");
        client.queryEvents("STREAM e FROM events");
        client.ping();
        client.audit();

        // 4 POST requests (stream, commit, erase, query)
        verify(4, postRequestedFor(anyUrl()).withHeader("Authorization", equalTo("Bearer secret")));
        // 2 GET requests (ping, audit)
        verify(2, getRequestedFor(anyUrl()).withHeader("Authorization", equalTo("Bearer secret")));
    }

    // ==================== User-Agent Tests ====================

    @Test
    void allEndpoints_sendUserAgentHeader() {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody("")));

        client.ping();

        verify(getRequestedFor(anyUrl()).withHeader("User-Agent", equalTo("genesisdb-sdk")));
    }
}
