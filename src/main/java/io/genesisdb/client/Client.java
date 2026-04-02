package io.genesisdb.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client for interacting with GenesisDB.
 * <p>
 * This client provides methods for streaming, committing, observing, and querying events
 * in GenesisDB. It supports both synchronous and asynchronous operations.
 * </p>
 *
 * <h2>Configuration</h2>
 * <p>
 * The client can be configured either through environment variables or programmatically:
 * </p>
 * <ul>
 *   <li>GENESISDB_API_URL - The GenesisDB API URL</li>
 *   <li>GENESISDB_API_VERSION - The API version (e.g., "v1")</li>
 *   <li>GENESISDB_AUTH_TOKEN - The authentication token</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Using environment variables
 * Client client = new Client();
 *
 * // Using explicit configuration
 * Client client = new Client(ClientConfig.builder()
 *     .apiUrl("http://localhost:8080")
 *     .apiVersion("v1")
 *     .authToken("your-token")
 *     .build());
 * }</pre>
 */
public class Client {
    private static final String USER_AGENT = "genesisdb-sdk";
    private static final String ENV_API_URL = "GENESISDB_API_URL";
    private static final String ENV_API_VERSION = "GENESISDB_API_VERSION";
    private static final String ENV_AUTH_TOKEN = "GENESISDB_AUTH_TOKEN";

    private final String apiUrl;
    private final String apiVersion;
    private final String authToken;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;

    /**
     * Creates a new client using environment variables for configuration.
     *
     * @throws IllegalStateException if required environment variables are missing
     */
    public Client() {
        this(null);
    }

    /**
     * Creates a new client with the specified configuration.
     * Falls back to environment variables for any missing configuration values.
     *
     * @param config the client configuration, or null to use only environment variables
     * @throws IllegalStateException if required configuration is missing
     */
    public Client(ClientConfig config) {
        String envApiUrl = System.getenv(ENV_API_URL);
        String envApiVersion = System.getenv(ENV_API_VERSION);
        String envAuthToken = System.getenv(ENV_AUTH_TOKEN);

        if (config != null) {
            this.apiUrl = config.getApiUrl() != null ? config.getApiUrl() : envApiUrl;
            this.apiVersion = config.getApiVersion() != null ? config.getApiVersion() : envApiVersion;
            this.authToken = config.getAuthToken() != null ? config.getAuthToken() : envAuthToken;
        } else {
            this.apiUrl = envApiUrl;
            this.apiVersion = envApiVersion;
            this.authToken = envAuthToken;
        }

        checkRequiredConfig();
        this.objectMapper = new ObjectMapper();
        this.executor = Executors.newCachedThreadPool();
    }

    private void checkRequiredConfig() {
        List<String> missing = new ArrayList<>();
        if (apiUrl == null || apiUrl.isEmpty()) {
            missing.add(ENV_API_URL);
        }
        if (apiVersion == null || apiVersion.isEmpty()) {
            missing.add(ENV_API_VERSION);
        }
        if (authToken == null || authToken.isEmpty()) {
            missing.add(ENV_AUTH_TOKEN);
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing required configuration: " + String.join(", ", missing));
        }
    }

    /**
     * Streams all events for a subject.
     *
     * @param subject the subject to stream events for
     * @return a list of CloudEvents
     * @throws GenesisDbException if the API request fails
     */
    public List<CloudEvent> streamEvents(String subject) {
        return streamEvents(subject, null);
    }

    /**
     * Streams events for a subject with options.
     *
     * @param subject the subject to stream events for
     * @param options streaming options (lowerBound, includeLowerBoundEvent, latestByEventType)
     * @return a list of CloudEvents
     * @throws GenesisDbException if the API request fails
     */
    public List<CloudEvent> streamEvents(String subject, StreamOptions options) {
        String url = apiUrl + "/api/" + apiVersion + "/stream";

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("subject", subject);

        if (options != null) {
            ObjectNode optionsNode = objectMapper.createObjectNode();
            if (options.getLowerBound() != null) {
                optionsNode.put("lowerBound", options.getLowerBound());
            }
            if (options.getIncludeLowerBoundEvent() != null) {
                optionsNode.put("includeLowerBoundEvent", options.getIncludeLowerBoundEvent());
            }
            if (options.getUpperBound() != null) {
                optionsNode.put("upperBound", options.getUpperBound());
            }
            if (options.getIncludeUpperBoundEvent() != null) {
                optionsNode.put("includeUpperBoundEvent", options.getIncludeUpperBoundEvent());
            }
            if (options.getLatestByEventType() != null) {
                optionsNode.put("latestByEventType", options.getLatestByEventType());
            }
            requestBody.set("options", optionsNode);
        }

        try {
            String response = doPost(url, requestBody.toString(), "application/x-ndjson");
            return parseNdjsonEvents(response);
        } catch (IOException e) {
            throw new GenesisDbException("Error while streaming events", e);
        }
    }

    /**
     * Commits events to GenesisDB.
     *
     * @param events the events to commit
     * @throws GenesisDbException if the API request fails
     */
    public void commitEvents(List<EventData> events) {
        commitEvents(events, null);
    }

    /**
     * Commits events to GenesisDB with preconditions.
     *
     * @param events the events to commit
     * @param preconditions optional preconditions to check before committing
     * @throws GenesisDbException if the API request fails or preconditions fail (HTTP 412)
     */
    public void commitEvents(List<EventData> events, List<Precondition> preconditions) {
        String url = apiUrl + "/api/" + apiVersion + "/commit";

        ObjectNode requestBody = objectMapper.createObjectNode();
        ArrayNode eventsArray = objectMapper.createArrayNode();

        for (EventData event : events) {
            ObjectNode eventNode = objectMapper.createObjectNode();
            eventNode.put("source", event.getSource());
            eventNode.put("subject", event.getSubject());
            eventNode.put("type", event.getType());
            eventNode.set("data", objectMapper.valueToTree(event.getData()));

            if (event.getOptions() != null && event.getOptions().getStoreDataAsReference() != null) {
                ObjectNode optionsNode = objectMapper.createObjectNode();
                optionsNode.put("storeDataAsReference", event.getOptions().getStoreDataAsReference());
                eventNode.set("options", optionsNode);
            }

            eventsArray.add(eventNode);
        }
        requestBody.set("events", eventsArray);

        if (preconditions != null && !preconditions.isEmpty()) {
            ArrayNode preconditionsArray = objectMapper.createArrayNode();
            for (Precondition precondition : preconditions) {
                ObjectNode preconditionNode = objectMapper.createObjectNode();
                preconditionNode.put("type", precondition.getType());
                preconditionNode.set("payload", objectMapper.valueToTree(precondition.getPayload()));
                preconditionsArray.add(preconditionNode);
            }
            requestBody.set("preconditions", preconditionsArray);
        }

        try {
            doPost(url, requestBody.toString(), null);
        } catch (IOException e) {
            throw new GenesisDbException("Error while committing events", e);
        }
    }

    /**
     * Erases data for a subject (GDPR compliance).
     * Only works for events that were committed with storeDataAsReference option.
     *
     * @param subject the subject to erase data for
     * @throws GenesisDbException if the API request fails
     */
    public void eraseData(String subject) {
        String url = apiUrl + "/api/" + apiVersion + "/erase";

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("subject", subject);

        try {
            doPost(url, requestBody.toString(), null);
        } catch (IOException e) {
            throw new GenesisDbException("Error while erasing data", e);
        }
    }

    /**
     * Observes events for a subject in real-time.
     * This method blocks and calls the observer for each received event.
     *
     * @param subject the subject to observe
     * @param observer the callback to receive events
     * @return an ObserveHandle to control the observation
     */
    public ObserveHandle observeEvents(String subject, EventObserver observer) {
        return observeEvents(subject, null, observer);
    }

    /**
     * Observes events for a subject in real-time with options.
     * This method blocks and calls the observer for each received event.
     *
     * @param subject the subject to observe
     * @param options streaming options (lowerBound, includeLowerBoundEvent, latestByEventType)
     * @param observer the callback to receive events
     * @return an ObserveHandle to control the observation
     */
    public ObserveHandle observeEvents(String subject, StreamOptions options, EventObserver observer) {
        AtomicBoolean running = new AtomicBoolean(true);

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            String url = apiUrl + "/api/" + apiVersion + "/observe";

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("subject", subject);

            if (options != null) {
                ObjectNode optionsNode = objectMapper.createObjectNode();
                if (options.getLowerBound() != null) {
                    optionsNode.put("lowerBound", options.getLowerBound());
                }
                if (options.getIncludeLowerBoundEvent() != null) {
                    optionsNode.put("includeLowerBoundEvent", options.getIncludeLowerBoundEvent());
                }
                if (options.getUpperBound() != null) {
                    optionsNode.put("upperBound", options.getUpperBound());
                }
                if (options.getIncludeUpperBoundEvent() != null) {
                    optionsNode.put("includeUpperBoundEvent", options.getIncludeUpperBoundEvent());
                }
                if (options.getLatestByEventType() != null) {
                    optionsNode.put("latestByEventType", options.getLatestByEventType());
                }
                requestBody.set("options", optionsNode);
            }

            HttpURLConnection conn = null;
            try {
                URL urlObj = new URL(url);
                conn = (HttpURLConnection) urlObj.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/x-ndjson");
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setReadTimeout(0); // No timeout for streaming

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new GenesisDbException(responseCode, conn.getResponseMessage());
                }

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while (running.get() && (line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty()) continue;

                        // Handle SSE format
                        if (line.startsWith("data: ")) {
                            line = line.substring(6);
                        }

                        try {
                            JsonNode json = objectMapper.readTree(line);
                            // Skip heartbeat messages
                            if (json.has("payload") && json.get("payload").asText().isEmpty()
                                    && json.size() == 1) {
                                continue;
                            }
                            CloudEvent event = jsonToCloudEvent(json);
                            observer.onEvent(event);
                        } catch (JsonProcessingException e) {
                            System.err.println("Error parsing event: " + e.getMessage());
                            System.err.println("Problem with JSON: " + line);
                        }
                    }
                }
            } catch (IOException e) {
                if (running.get()) {
                    throw new GenesisDbException("Error while observing events", e);
                }
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }, executor);

        return new ObserveHandle() {
            @Override
            public void stop() {
                running.set(false);
            }

            @Override
            public CompletableFuture<Void> getFuture() {
                return future;
            }
        };
    }

    /**
     * Queries events using GDBQL (GenesisDB Query Language).
     *
     * @param query the GDBQL query string
     * @return a list of query results as Maps
     * @throws GenesisDbException if the API request fails
     */
    public List<Map<String, Object>> queryEvents(String query) {
        return q(query);
    }

    /**
     * Queries events using GDBQL (GenesisDB Query Language).
     * Alias for queryEvents().
     *
     * @param query the GDBQL query string
     * @return a list of query results as Maps
     * @throws GenesisDbException if the API request fails
     */
    public List<Map<String, Object>> q(String query) {
        String url = apiUrl + "/api/" + apiVersion + "/q";

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("query", query);

        try {
            String response = doPost(url, requestBody.toString(), "application/x-ndjson");
            return parseNdjsonResults(response);
        } catch (IOException e) {
            throw new GenesisDbException("Error while querying events", e);
        }
    }

    /**
     * Pings the GenesisDB API to check connectivity.
     *
     * @return the ping response
     * @throws GenesisDbException if the API request fails
     */
    public String ping() {
        String url = apiUrl + "/api/" + apiVersion + "/status/ping";
        try {
            return doGet(url, "text/plain");
        } catch (IOException e) {
            throw new GenesisDbException("Error while pinging", e);
        }
    }

    /**
     * Runs an audit to check event consistency.
     *
     * @return the audit response
     * @throws GenesisDbException if the API request fails
     */
    public String audit() {
        String url = apiUrl + "/api/" + apiVersion + "/status/audit";
        try {
            return doGet(url, "text/plain");
        } catch (IOException e) {
            throw new GenesisDbException("Error while running audit", e);
        }
    }

    /**
     * Closes the client and releases resources.
     */
    public void close() {
        executor.shutdown();
    }

    private String doGet(String urlString, String accept) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + authToken);
        conn.setRequestProperty("Content-Type", accept);
        conn.setRequestProperty("User-Agent", USER_AGENT);

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new GenesisDbException(responseCode, conn.getResponseMessage());
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } finally {
            conn.disconnect();
        }
    }

    private String doPost(String urlString, String body, String accept) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + authToken);
        conn.setRequestProperty("Content-Type", "application/json");
        if (accept != null) {
            conn.setRequestProperty("Accept", accept);
        }
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new GenesisDbException(responseCode, conn.getResponseMessage());
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
            return response.toString();
        } finally {
            conn.disconnect();
        }
    }

    private List<CloudEvent> parseNdjsonEvents(String ndjson) {
        List<CloudEvent> events = new ArrayList<>();
        if (ndjson == null || ndjson.trim().isEmpty()) {
            return events;
        }

        String[] lines = ndjson.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            try {
                JsonNode json = objectMapper.readTree(line);
                CloudEvent event = jsonToCloudEvent(json);
                events.add(event);
            } catch (JsonProcessingException e) {
                System.err.println("Error parsing event: " + e.getMessage());
                System.err.println("Problem with JSON: " + line);
            }
        }

        return events;
    }

    private CloudEvent jsonToCloudEvent(JsonNode json) {
        CloudEventBuilder builder = CloudEventBuilder.v1()
                .withId(json.has("id") ? json.get("id").asText() : "")
                .withSource(URI.create(json.has("source") ? json.get("source").asText() : ""))
                .withType(json.has("type") ? json.get("type").asText() : "");

        if (json.has("subject")) {
            builder.withSubject(json.get("subject").asText());
        }
        if (json.has("time")) {
            builder.withTime(OffsetDateTime.parse(json.get("time").asText()));
        }
        if (json.has("datacontenttype")) {
            builder.withDataContentType(json.get("datacontenttype").asText());
        }
        if (json.has("data")) {
            try {
                byte[] dataBytes = objectMapper.writeValueAsBytes(json.get("data"));
                builder.withData("application/json", dataBytes);
            } catch (JsonProcessingException e) {
                // Ignore data serialization errors
            }
        }

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseNdjsonResults(String ndjson) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (ndjson == null || ndjson.trim().isEmpty()) {
            return results;
        }

        String[] lines = ndjson.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            try {
                Map<String, Object> result = objectMapper.readValue(line, Map.class);
                results.add(result);
            } catch (JsonProcessingException e) {
                System.err.println("Error parsing result: " + e.getMessage());
                System.err.println("Problem with JSON: " + line);
            }
        }

        return results;
    }

    /**
     * Handle for controlling event observation.
     */
    public interface ObserveHandle {
        /**
         * Stops the event observation.
         */
        void stop();

        /**
         * Gets the future representing the observation task.
         *
         * @return the CompletableFuture for the observation
         */
        CompletableFuture<Void> getFuture();
    }
}
