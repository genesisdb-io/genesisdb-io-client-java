package io.genesisdb.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs against a live GenesisDB. Skipped unless GENESISDB_URL and GENESISDB_AUTH_TOKEN (an administrator token)
 * are set. Writes under /sdk-test-java/<run id>.
 */
@EnabledIfEnvironmentVariable(named = "GENESISDB_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "GENESISDB_AUTH_TOKEN", matches = ".+")
class ClientIntegrationTest {
    private static final String RUN = Long.toHexString(System.nanoTime());
    private static final String ROOT = "/sdk-test-java/" + RUN;
    private static Client db;

    @BeforeAll
    static void connect() {
        db = Client.fromEnv();
        assertEquals("Pong", db.ping());
    }

    private static NewEvent event(String subject, String type, Map<String, ?> data) {
        return new NewEvent("sdk-test", subject, type, data);
    }

    @Test
    void fullRoundTrip() throws Exception {
        // commit, fetch, query
        List<Event> stored = db.commit(List.of(
                event(ROOT + "/customer/1", "sdk.customer-added", Map.of("name", "Bruce", "age", 40)),
                event(ROOT + "/customer/2", "sdk.customer-added", Map.of("name", "Selina", "age", 35))));
        assertEquals(2, stored.size());
        assertEquals(36, stored.get(0).getId().length());
        assertEquals(64, stored.get(0).getHash().length());
        assertEquals(stored.get(0).getHash(), stored.get(1).getPredecessorhash());

        List<Event> fetched = db.fetch(ROOT);
        assertEquals(List.of("Bruce", "Selina"), fetched.stream().map(e -> e.getData().get("name").asText()).collect(java.util.stream.Collectors.toList()));

        List<Event> bounded = db.fetch(ROOT, new FetchOptions().lowerBound(stored.get(0).getId(), false));
        assertEquals(List.of(stored.get(1).getId()), bounded.stream().map(Event::getId).collect(java.util.stream.Collectors.toList()));

        List<JsonNode> rows = db.query("STREAM e FROM events WHERE e.subject DESCENDANTS '" + ROOT + "' ORDER BY e.data.age MAP { name: e.data.name }");
        assertEquals(List.of("Selina", "Bruce"), rows.stream().map(r -> r.get("name").asText()).collect(java.util.stream.Collectors.toList()));
        List<Integer> count = db.query("STREAM e FROM events WHERE e.subject DESCENDANTS '" + ROOT + "' MAP COUNT()", Integer.class);
        assertEquals(List.of(2), count);

        // preconditions
        PreconditionFailedException failed = assertThrows(PreconditionFailedException.class, () -> db.commit(
                List.of(event(ROOT + "/customer/1", "sdk.noop", Map.of())),
                List.of(Precondition.isSubjectNew(ROOT + "/customer/1"))));
        assertEquals("isSubjectNew", failed.getPreconditionType());

        // catalog
        assertTrue(db.subjects().contains("/sdk-test-java"));
        assertTrue(db.types().contains("sdk.customer-added"));

        // reference payloads and erase
        String user = ROOT + "/user/1";
        List<Event> committed = db.commit(List.of(event(user, "sdk.user-added", Map.of("email", "bruce@example.com")).asReference()));
        assertEquals("bruce@example.com", committed.get(0).getData().get("email").asText());
        assertEquals("bruce@example.com", db.fetch(user).get(0).getData().get("email").asText());
        assertTrue(db.erase(user).contains("Erased 1 ref data"));
        assertTrue(db.fetch(user).get(0).getData().isEmpty());
        assertThrows(UnprocessableException.class, () -> db.erase(ROOT + "/customer/1"));

        // schemas
        String schemaType = "sdk.schema-test-" + RUN;
        db.registerSchema(schemaType, Map.of("type", "object", "properties", Map.of("n", Map.of("type", "number")), "required", List.of("n")));
        assertThrows(ConflictException.class, () -> db.registerSchema(schemaType, Map.of("type", "object")));
        assertTrue(db.schemas().stream().anyMatch(s -> s.getType().equals(schemaType)));
        GenesisDbException violation = assertThrows(GenesisDbException.class,
                () -> db.commit(List.of(event(ROOT + "/s/1", schemaType, Map.of("n", "not a number")))));
        assertEquals(400, violation.getStatus());

        // subscribe
        String live = ROOT + "/live";
        List<String> received = new CopyOnWriteArrayList<>();
        CountDownLatch two = new CountDownLatch(2);
        Thread consumer = new Thread(() -> db.subscribe(live, new SubscribeOptions().interval(Duration.ofMillis(100)), e -> {
            received.add(e.getId());
            two.countDown();
        }));
        consumer.start();
        Thread.sleep(300);
        List<Event> liveEvents = db.commit(List.of(
                event(live + "/1", "sdk.live", Map.of("n", 1)),
                event(live + "/2", "sdk.live", Map.of("n", 2))));
        assertTrue(two.await(5, TimeUnit.SECONDS));
        consumer.interrupt();
        consumer.join(5000);
        assertEquals(List.of(liveEvents.get(0).getId(), liveEvents.get(1).getId()), received);

        // status, audit
        Status status = db.status();
        assertTrue(status.isReady());
        assertTrue(status.getEventCount() >= 5);
        assertTrue(db.audit().startsWith("Audit passed"));
    }
}
