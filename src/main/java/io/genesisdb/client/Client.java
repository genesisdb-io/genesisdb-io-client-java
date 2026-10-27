package io.genesisdb.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Client for GenesisDB.
 *
 * <pre>{@code
 * Client db = Client.builder().url("http://localhost:8080").token(System.getenv("GENESISDB_AUTH_TOKEN")).build();
 * db.commit(List.of(new NewEvent("app", "/customer/1", "customer.created", Map.of("name", "Bruce"))));
 * List<Event> events = db.fetch("/customer");
 * }</pre>
 *
 * {@code url} and {@code token} fall back to GENESISDB_URL and GENESISDB_AUTH_TOKEN. Every method throws
 * {@link GenesisDbException} (or a subclass) on a non-2xx answer. Thread-safe.
 */
public final class Client {
    static final String USER_AGENT = "genesisdb-java/2";
    private static final String HEARTBEAT = "{\"payload\":\"\"}";

    private final String url;
    private final String token;
    private final Duration timeout;
    private final HttpClient http;

    private Client(Builder b) {
        String url = firstNonEmpty(b.url, System.getenv("GENESISDB_URL"));
        String token = firstNonEmpty(b.token, System.getenv("GENESISDB_AUTH_TOKEN"));
        if (url == null) {
            throw new IllegalArgumentException("GenesisDB URL missing: call url() or set GENESISDB_URL");
        }
        if (token == null) {
            throw new IllegalArgumentException("GenesisDB token missing: call token() or set GENESISDB_AUTH_TOKEN");
        }
        this.url = url.replaceAll("/+$", "");
        this.token = token;
        this.timeout = b.timeout;
        this.http = b.http != null ? b.http : HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    private static String firstNonEmpty(String a, String b) {
        if (a != null && !a.isEmpty()) return a;
        if (b != null && !b.isEmpty()) return b;
        return null;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Client from GENESISDB_URL and GENESISDB_AUTH_TOKEN. */
    public static Client fromEnv() {
        return builder().build();
    }

    public static final class Builder {
        private String url;
        private String token;
        private Duration timeout = Duration.ofSeconds(30);
        private HttpClient http;

        /** Base URL, for example http://localhost:8080. Falls back to GENESISDB_URL. */
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        /** Bearer token. Falls back to GENESISDB_AUTH_TOKEN. */
        public Builder token(String token) {
            this.token = token;
            return this;
        }

        /** Timeout for non-streaming calls. Default 30 s. Null disables. Subscriptions never time out. */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /** Bring your own HttpClient (proxies, TLS, executor). */
        public Builder http(HttpClient http) {
            this.http = http;
            return this;
        }

        public Client build() {
            return new Client(this);
        }
    }

    // Health and status

    /** Liveness check. Needs no token on the server. Returns "Pong". */
    public String ping() {
        return text("ping", "GET", "/api/v2/ping", null);
    }

    /** Engine version, host, event counts, storage format and readiness. */
    public Status status() {
        return new Status(json("status", "GET", "/api/v2/status", null));
    }

    /** Verify the hash chain of the whole store. Administrator token. Returns the audit text. */
    public String audit() {
        return text("audit", "GET", "/api/v2/admin/audit", null);
    }

    // Events

    /** Commit events atomically. Returns the stored events. */
    public List<Event> commit(List<NewEvent> events) {
        return commit(events, Collections.emptyList());
    }

    /**
     * Commit events atomically after checking the preconditions. Returns the stored events.
     * A failed precondition throws {@link PreconditionFailedException} and writes nothing.
     */
    public List<Event> commit(List<NewEvent> events, List<Precondition> preconditions) {
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException("commit requires at least one event");
        }
        ObjectNode body = Json.MAPPER.createObjectNode();
        body.set("events", Json.MAPPER.valueToTree(events));
        if (preconditions != null && !preconditions.isEmpty()) {
            body.set("preconditions", Json.MAPPER.valueToTree(preconditions));
        }
        return ndjson("commit", "POST", "/api/v2/commit", body, Event.class);
    }

    /** Fetch the events of a subject and all of its descendants, in commit order. */
    public List<Event> fetch(String subject) {
        return fetch(subject, null);
    }

    public List<Event> fetch(String subject, FetchOptions options) {
        return ndjson("fetch", "POST", "/api/v2/fetch", fetchBody(subject, options), Event.class);
    }

    /**
     * Subscribe to a subject and its descendants. Calls the consumer with the history first, then with new events as
     * they are committed. Blocks until the server ends the stream or the calling thread is interrupted. A stream the
     * server ends with an error throws {@link GenesisDbException}. Run it on its own thread and interrupt it to stop.
     */
    public void subscribe(String subject, SubscribeOptions options, Consumer<Event> consumer) {
        SubscribeOptions resolved = options == null ? new SubscribeOptions() : options;
        ObjectNode body = fetchBody(subject, resolved.getFetch());
        if (resolved.getInterval() != null) {
            body.put("interval", resolved.getInterval().toNanos() / 1_000_000_000.0);
        }
        HttpRequest request = request("POST", "/api/v2/subscribe", body, null);
        HttpResponse<InputStream> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException e) {
            throw new GenesisDbException("subscribe", e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        try (InputStream in = response.body()) {
            if (response.statusCode() != 200) {
                throw GenesisDbException.forResponse("subscribe", response.statusCode(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String eventName = "";
            String line;
            while ((line = reader.readLine()) != null) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                if (line.startsWith("event: ")) {
                    eventName = line.substring(7).trim();
                    continue;
                }
                if (!line.startsWith("data: ")) {
                    eventName = "";
                    continue;
                }
                String payload = line.substring(6);
                if (eventName.equals("error")) {
                    String message = payload;
                    try {
                        JsonNode parsed = Json.MAPPER.readTree(payload);
                        if (parsed.hasNonNull("error")) message = parsed.get("error").asText();
                    } catch (IOException ignored) {
                        // keep the raw payload
                    }
                    throw new GenesisDbException("subscribe ended", 200, message);
                }
                eventName = "";
                if (payload.trim().equals(HEARTBEAT)) {
                    continue;
                }
                consumer.accept(Json.MAPPER.readValue(payload, Event.class));
            }
        } catch (IOException e) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            throw new GenesisDbException("subscribe", e.getMessage(), e);
        }
    }

    /** Run a GDBQL query. Returns the result rows, or a single-element list for a bare aggregate. */
    public List<JsonNode> query(String query) {
        return ndjson("query", "POST", "/api/v2/query", Json.MAPPER.createObjectNode().put("query", query), JsonNode.class);
    }

    /** Run a GDBQL query and decode every row into the given type. */
    public <T> List<T> query(String query, Class<T> rowType) {
        return ndjson("query", "POST", "/api/v2/query", Json.MAPPER.createObjectNode().put("query", query), rowType);
    }

    /** Distinct top level subjects, sorted. */
    public List<String> subjects() {
        return catalog("subjects", "/api/v2/subjects", "subject");
    }

    /** Distinct event types, sorted. */
    public List<String> types() {
        return catalog("types", "/api/v2/types", "type");
    }

    private List<String> catalog(String operation, String path, String field) {
        List<String> out = new ArrayList<>();
        for (JsonNode row : ndjson(operation, "GET", path, null, JsonNode.class)) {
            out.add(row.path(field).asText());
        }
        return out;
    }

    // Schemas

    /** Register a schema for an event type. Throws {@link ConflictException} when the type already has one. Administrator token. */
    public void registerSchema(String eventType, Object schema) {
        ObjectNode body = Json.MAPPER.createObjectNode();
        body.put("type", eventType);
        body.set("schema", Json.MAPPER.valueToTree(schema));
        raw("register_schema", "POST", "/api/v2/schemas", body);
    }

    /** Every registered schema. */
    public List<RegisteredSchema> schemas() {
        return ndjson("schemas", "GET", "/api/v2/schemas", null, RegisteredSchema.class);
    }

    // Administration

    /**
     * Erase every reference payload below a subject. Inline event data is immutable and stays. Throws
     * {@link NotFoundException} when the subject has no events, {@link UnprocessableException} when it has no
     * reference payloads. Administrator token. Returns the server's confirmation text.
     */
    public String erase(String subject) {
        return text("erase", "POST", "/api/v2/admin/erase", Json.MAPPER.createObjectNode().put("subject", subject));
    }

    // Transport

    private static ObjectNode fetchBody(String subject, FetchOptions options) {
        ObjectNode body = Json.MAPPER.createObjectNode().put("subject", subject);
        if (options != null && !options.isEmpty()) {
            body.set("options", Json.MAPPER.valueToTree(options));
        }
        return body;
    }

    private HttpRequest request(String method, String path, Object body, Duration timeout) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url + path))
                .header("Authorization", "Bearer " + token)
                .header("User-Agent", USER_AGENT);
        if (timeout != null) {
            b.timeout(timeout);
        }
        if (body == null) {
            b.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            try {
                b.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(Json.MAPPER.writeValueAsString(body)));
            } catch (IOException e) {
                throw new IllegalArgumentException("body does not serialise to JSON", e);
            }
        }
        return b.build();
    }

    private String raw(String operation, String method, String path, Object body) {
        HttpResponse<String> response;
        try {
            response = http.send(request(method, path, body, timeout), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new GenesisDbException(operation, e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GenesisDbException(operation, "interrupted", e);
        }
        if (response.statusCode() < 200 || response.statusCode() > 299) {
            throw GenesisDbException.forResponse(operation, response.statusCode(), response.body());
        }
        return response.body();
    }

    private String text(String operation, String method, String path, Object body) {
        return raw(operation, method, path, body);
    }

    private JsonNode json(String operation, String method, String path, Object body) {
        try {
            return Json.MAPPER.readTree(raw(operation, method, path, body));
        } catch (IOException e) {
            throw new GenesisDbException(operation, "invalid JSON in response", e);
        }
    }

    private <T> List<T> ndjson(String operation, String method, String path, Object body, Class<T> type) {
        String text = raw(operation, method, path, body);
        List<T> out = new ArrayList<>();
        for (String line : text.split("\n")) {
            if (line.trim().isEmpty()) continue;
            try {
                out.add(Json.MAPPER.readValue(line, type));
            } catch (IOException e) {
                throw new GenesisDbException(operation, "invalid NDJSON line in response", e);
            }
        }
        return out;
    }
}
