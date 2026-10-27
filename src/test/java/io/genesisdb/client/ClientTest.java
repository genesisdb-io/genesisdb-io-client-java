package io.genesisdb.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/** Unit tests against a WireMock server. See ClientIntegrationTest for the live tests. */
class ClientTest {
    private static final String STORED = "{\"source\":\"app\",\"subject\":\"/customer/1\",\"type\":\"customer.created\",\"specversion\":\"1.0\","
            + "\"id\":\"0199c0e0-0000-7000-8000-000000000001\",\"time\":\"2026-10-24T10:00:00.123456789Z\",\"datacontenttype\":\"application/json\","
            + "\"predecessorhash\":\"" + "0".repeat(64) + "\",\"data\":{\"name\":\"Bruce\"},\"hash\":\"" + "a".repeat(64) + "\"}";

    private static WireMockServer server;
    private Client db;

    @BeforeAll
    static void start() {
        server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();
    }

    @AfterAll
    static void stop() {
        server.stop();
    }

    @BeforeEach
    void setUp() {
        server.resetAll();
        db = Client.builder().url("http://localhost:" + server.port() + "/").token("secret").build();
    }

    private static NewEvent event(String subject) {
        return new NewEvent("app", subject, "customer.created", Map.of("name", "Bruce"));
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "GENESISDB_URL", matches = ".+")
    @DisabledIfEnvironmentVariable(named = "GENESISDB_AUTH_TOKEN", matches = ".+")
    void configRequiresUrlAndToken() {
        assertThrows(IllegalArgumentException.class, () -> Client.builder().token("t").build());
        assertThrows(IllegalArgumentException.class, () -> Client.builder().url("http://x").build());
    }

    @Test
    void pingSendsHeaders() {
        server.stubFor(get("/ping").willReturn(ok("Pong")));
        assertEquals("Pong", db.ping());
        server.verify(getRequestedFor(urlEqualTo("/ping"))
                .withHeader("Authorization", equalTo("Bearer secret"))
                .withHeader("User-Agent", equalTo("genesisdb-java/2")));
    }

    @Test
    void commitSendsBodyAndParsesNdjson() {
        server.stubFor(post("/api/v1/commit").willReturn(ok(STORED + "\n")));
        List<Event> events = db.commit(List.of(event("/customer/1")), List.of(Precondition.isSubjectNew("/customer/1")));
        server.verify(postRequestedFor(urlEqualTo("/api/v1/commit")).withRequestBody(equalToJson(
                "{\"events\":[{\"source\":\"app\",\"subject\":\"/customer/1\",\"type\":\"customer.created\",\"data\":{\"name\":\"Bruce\"}}],"
                        + "\"preconditions\":[{\"type\":\"isSubjectNew\",\"payload\":{\"subject\":\"/customer/1\"}}]}")));
        assertEquals(1, events.size());
        Event e = events.get(0);
        assertEquals("0199c0e0-0000-7000-8000-000000000001", e.getId());
        assertEquals("Bruce", e.getData().get("name").asText());
        assertEquals(2026, e.getTime().getYear());
        assertEquals(123456789, e.getTime().getNano());
        assertEquals("a".repeat(64), e.getHash());
        assertEquals("Bruce", e.getDataAs(Map.class).get("name"));
    }

    @Test
    void commitOmitsEmptyPreconditionsAndSerialisesReferenceOption() {
        server.stubFor(post("/api/v1/commit").willReturn(ok(STORED + "\n")));
        db.commit(List.of(event("/customer/1").asReference()));
        server.verify(postRequestedFor(urlEqualTo("/api/v1/commit")).withRequestBody(equalToJson(
                "{\"events\":[{\"source\":\"app\",\"subject\":\"/customer/1\",\"type\":\"customer.created\",\"data\":{\"name\":\"Bruce\"},\"options\":{\"storeDataAsReference\":true}}]}")));
    }

    @Test
    void commitRejectsEmptyList() {
        assertThrows(IllegalArgumentException.class, () -> db.commit(Collections.emptyList()));
    }

    @Test
    void preconditionFailedIsParsed() {
        server.stubFor(post("/api/v1/commit").willReturn(status(412).withBody("{\"type\":\"isSubjectNew\",\"message\":\"subject /customer/1 already exists\"}")));
        PreconditionFailedException e = assertThrows(PreconditionFailedException.class, () -> db.commit(List.of(event("/customer/1"))));
        assertEquals(412, e.getStatus());
        assertEquals("isSubjectNew", e.getPreconditionType());
        assertEquals("subject /customer/1 already exists", e.getReason());
        assertEquals("commit", e.getOperation());
    }

    @Test
    void fetchWithAndWithoutOptions() {
        server.stubFor(post("/api/v1/fetch").willReturn(ok(STORED + "\n" + STORED + "\n")));
        assertEquals(2, db.fetch("/customer", new FetchOptions().lowerBound("x", true)).size());
        server.verify(postRequestedFor(urlEqualTo("/api/v1/fetch")).withRequestBody(equalToJson(
                "{\"subject\":\"/customer\",\"options\":{\"lowerBound\":\"x\",\"includeLowerBoundEvent\":true}}")));
        server.resetRequests();
        server.stubFor(post("/api/v1/fetch").willReturn(ok("")));
        assertTrue(db.fetch("/customer").isEmpty());
        assertTrue(db.fetch("/customer", new FetchOptions()).isEmpty());
        server.verify(2, postRequestedFor(urlEqualTo("/api/v1/fetch")).withRequestBody(equalToJson("{\"subject\":\"/customer\"}")));
    }

    public static final class Row {
        public String name;
    }

    @Test
    void querySubjectsTypes() {
        server.stubFor(post("/api/v1/query").willReturn(ok("{\"name\":\"Bruce\"}\n{\"name\":\"Selina\"}\n")));
        server.stubFor(get("/api/v1/subjects").willReturn(ok("{\"subject\":\"/customer\"}\n{\"subject\":\"/order\"}\n")));
        server.stubFor(get("/api/v1/types").willReturn(ok("{\"type\":\"customer.created\"}\n")));
        List<JsonNode> rows = db.query("STREAM e FROM events MAP { name: e.data.name }");
        assertEquals("Bruce", rows.get(0).get("name").asText());
        List<Row> typed = db.query("x", Row.class);
        assertEquals("Selina", typed.get(1).name);
        assertEquals(Arrays.asList("/customer", "/order"), db.subjects());
        assertEquals(List.of("customer.created"), db.types());
    }

    @Test
    void statusIsMapped() {
        server.stubFor(get("/api/v1/status").willReturn(okJson(
                "{\"engine\":{\"version\":\"2.0.0\",\"channel\":\"stable\"},\"host\":{\"hostname\":\"db\"},"
                        + "\"events\":{\"count\":3,\"subjects\":1,\"types\":2},\"storage\":{\"storageFormat\":\"v2\",\"ready\":true}}")));
        Status s = db.status();
        assertEquals("2.0.0", s.getVersion());
        assertEquals(3, s.getEventCount());
        assertTrue(s.isReady());
        assertEquals("db", s.getRaw().path("host").path("hostname").asText());
    }

    @Test
    void schemasAndAdmin() {
        server.stubFor(post("/api/v1/schemas").willReturn(status(201)));
        server.stubFor(get("/api/v1/schemas").willReturn(ok("{\"type\":\"customer.created\",\"schema\":{\"type\":\"object\"}}\n")));
        server.stubFor(get("/api/v1/status/audit").willReturn(ok("Audit passed")));
        server.stubFor(post("/api/v1/erase").willReturn(ok("Erased")));

        db.registerSchema("customer.created", Map.of("type", "object"));
        server.verify(postRequestedFor(urlEqualTo("/api/v1/schemas")).withRequestBody(equalToJson("{\"type\":\"customer.created\",\"schema\":{\"type\":\"object\"}}")));
        List<RegisteredSchema> schemas = db.schemas();
        assertEquals("customer.created", schemas.get(0).getType());
        assertEquals("object", schemas.get(0).getSchema().get("type").asText());
        assertEquals("Audit passed", db.audit());
        assertEquals("Erased", db.erase("/customer/1"));
        server.verify(postRequestedFor(urlEqualTo("/api/v1/erase")).withRequestBody(equalToJson("{\"subject\":\"/customer/1\"}")));
    }

    @ParameterizedTest
    @CsvSource({
            "401, io.genesisdb.client.AuthenticationException",
            "403, io.genesisdb.client.AuthenticationException",
            "404, io.genesisdb.client.NotFoundException",
            "409, io.genesisdb.client.ConflictException",
            "422, io.genesisdb.client.UnprocessableException",
            "503, io.genesisdb.client.NotReadyException",
            "500, io.genesisdb.client.GenesisDbException",
    })
    void errorClasses(int status, String className) throws Exception {
        server.stubFor(post("/api/v1/fetch").willReturn(status(status).withBody("nope")));
        GenesisDbException e = assertThrows(GenesisDbException.class, () -> db.fetch("/x"));
        assertEquals(Class.forName(className), e.getClass());
        assertEquals(status, e.getStatus());
        assertEquals("nope", e.getBody());
        assertEquals("fetch", e.getOperation());
        assertTrue(e.getMessage().contains("nope"));
    }

    @Test
    void subscribeSkipsHeartbeatAndStopsOnError() {
        String stream = "data: " + STORED + "\n\ndata: {\"payload\":\"\"}\n\ndata: " + STORED + "\n\nevent: error\ndata: {\"error\":\"store closed\"}\n\n";
        server.stubFor(post("/api/v1/subscribe").willReturn(ok(stream).withHeader("Content-Type", "text/event-stream")));
        List<Event> received = new ArrayList<>();
        GenesisDbException e = assertThrows(GenesisDbException.class,
                () -> db.subscribe("/customer", new SubscribeOptions().interval(Duration.ofMillis(500)), received::add));
        assertTrue(e.getMessage().contains("store closed"));
        assertEquals(2, received.size());
        server.verify(postRequestedFor(urlEqualTo("/api/v1/subscribe")).withRequestBody(equalToJson("{\"subject\":\"/customer\",\"interval\":0.5}")));
    }

    @Test
    void subscribeThrowsOnHttpError() {
        server.stubFor(post("/api/v1/subscribe").willReturn(status(401).withBody("denied")));
        assertThrows(AuthenticationException.class, () -> db.subscribe("/customer", null, e -> fail("no event expected")));
    }
}
