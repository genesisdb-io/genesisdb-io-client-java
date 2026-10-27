# GenesisDB client for Java

The official client for [GenesisDB](https://www.genesisdb.io), the open source event sourcing database. Java 11+, built on `java.net.http`, Jackson for JSON, works from Kotlin and Scala as well.

> **Version 2.0.0 targets GenesisDB 2.0 and is not compatible with GenesisDB 1.x servers or with the 1.x client API.** Routes, configuration, type names and method signatures have changed. Keep a 1.x client for a 1.x server.
>
> Existing 1.x clients keep working against a GenesisDB 2.0 server, which still serves the unchanged 1.x API under `/api/v1`, so the server can be upgraded first and the clients at your own pace. New 2.0 features (stored events returned by commit, upper bounds, schemas, typed errors) need this 2.0.0 client.

```xml
<dependency>
    <groupId>io.genesisdb</groupId>
    <artifactId>genesisdb</artifactId>
    <version>2.0.0</version>
</dependency>
```

```kotlin
implementation("io.genesisdb:genesisdb:2.0.0")
```

## Quick start

```java
import io.genesisdb.client.*;

Client db = Client.builder().url("http://localhost:8080").token(System.getenv("GENESISDB_AUTH_TOKEN")).build();

db.commit(List.of(
    new NewEvent("io.example.shop", "/customer/42", "io.example.shop.customer-registered", Map.of("name", "Bruce Wayne"))
));

List<Event> events = db.fetch("/customer/42");
```

`url` and `token` fall back to `GENESISDB_URL` and `GENESISDB_AUTH_TOKEN`, so `Client.fromEnv()` works in an environment that sets both. The client is thread-safe and shares one connection pool.

## Events

```java
// Commit a batch atomically. The server assigns ID, time and hashes and returns the stored events.
List<Event> stored = db.commit(List.of(
    new NewEvent("io.example.shop", "/order/1", "io.example.shop.order-placed", Map.of("total", 99.5)),
    new NewEvent("io.example.shop", "/order/1/item/1", "io.example.shop.item-added", Map.of("sku", "A1", "qty", 2))
));
stored.get(0).getId();    // "3f2c..."
stored.get(0).getHash();  // "a9b1..."
stored.get(0).getTime();  // OffsetDateTime

// A subject with one segment gets a generated ID appended.
db.commit(List.of(new NewEvent("io.example.shop", "/customer", "io.example.shop.customer-registered", Map.of("name", "Selina"))));

// Fetch a subject with all descendants, in commit order.
List<Event> all = db.fetch("/order/1");

// Resume after a known event, or stop at one.
List<Event> since = db.fetch("/order", new FetchOptions().lowerBound(lastSeenId, false));
List<Event> until = db.fetch("/order", new FetchOptions().upperBound(someId, true));

// Only the newest event of a type below the subject.
List<Event> latest = db.fetch("/order/1", new FetchOptions().latestByEventType("io.example.shop.order-placed"));

// Typed payloads
record Order(double total) {}
Order order = stored.get(0).getDataAs(Order.class);
```

`data` accepts a `Map`, a Jackson `JsonNode` or any bean that serialises to a JSON object. `Event.getData()` returns a `JsonNode`.

## Preconditions

Preconditions are checked atomically with the commit. A failed precondition throws `PreconditionFailedException` and writes nothing.

```java
try {
    db.commit(
        List.of(new NewEvent("io.example.shop", "/customer/42", "io.example.shop.customer-registered", Map.of("email", "bruce@example.com"))),
        List.of(
            Precondition.isSubjectNew("/customer/42"),
            Precondition.isQueryResultTrue("STREAM e FROM events WHERE e.data.email == 'bruce@example.com' MAP COUNT() == 0")
        )
    );
} catch (PreconditionFailedException e) {
    System.out.println(e.getPreconditionType() + ": " + e.getReason()); // "isSubjectNew: subject /customer/42 already has events"
}
```

Factories: `isSubjectNew`, `isSubjectExisting`, `isQueryResultTrue`.

## Subscriptions

`subscribe` consumes Server-Sent Events. It delivers the existing history first, then new events as they are committed, and blocks until the server ends the stream or the thread is interrupted. Run it on its own thread.

```java
Thread consumer = new Thread(() ->
    db.subscribe("/order", new SubscribeOptions().interval(Duration.ofMillis(500)), event -> {
        System.out.println(event.getType() + " " + event.getData());
    })
);
consumer.start();
// ...
consumer.interrupt(); // stops the subscription
```

`interval` is the poll interval (default 1 s). The bound options of `fetch` work here as well through `SubscribeOptions.fetch(...)`, so a consumer that persists the last processed event ID resumes with `lowerBound`. A stream the server ends with an error throws `GenesisDbException`.

## Queries

[GDBQL](https://github.com/genesisdb-io/genesisdb/blob/main/docs/gdbql.md) runs on the server. Results come back as rows, a bare aggregate as a single-element list.

```java
List<JsonNode> rows = db.query("STREAM e FROM events WHERE e.subject UNDER '/customer' MAP { name: e.data.name, total: e.data.total }");

record Row(String name, double total) {}
List<Row> typed = db.query("STREAM e FROM events WHERE e.subject UNDER '/customer' MAP { name: e.data.name, total: e.data.total }", Row.class);

List<Integer> count = db.query("STREAM e FROM events WHERE e.type == 'io.example.shop.order-placed' MAP COUNT()", Integer.class);

List<JsonNode> byCategory = db.query(
    "STREAM e FROM events WHERE e.type == 'io.example.shop.item-added' GROUP BY e.data.category MAP { category: e.data.category, items: COUNT(), revenue: SUM(e.data.total) }"
);
```

## GDPR: reference payloads and erasure

Store a payload as reference data and it can be erased later. The event, its hash and the chain stay intact, the payload becomes `{}`.

```java
db.commit(List.of(
    new NewEvent("io.example.shop", "/customer/42", "io.example.shop.customer-registered", Map.of("name", "Bruce", "email", "bruce@example.com")).asReference()
));

db.erase("/customer/42"); // administrator token
```

Inline payloads are immutable. `erase` on a subject without reference payloads throws `UnprocessableException` (422).

## Schemas

```java
db.registerSchema("io.example.shop.order-placed", Map.of(
    "type", "object",
    "properties", Map.of("total", Map.of("type", "number"), "currency", Map.of("type", "string")),
    "required", List.of("total")
));

List<RegisteredSchema> schemas = db.schemas();
```

Commits of that type are validated. A violation throws `GenesisDbException` with status 400.

## Catalog, status, administration

```java
db.subjects();          // ["/customer", "/order"]
db.types();             // ["io.example.shop.customer-registered", ...]
db.status();            // version, event counts, storage format, readiness, raw document
db.ping();              // "Pong", no token required

db.audit();             // "Audit passed. All 1234 events integrity checks succeeded."
```

Audit, erase and schema registration need the administrator token. Everything else works with a reader token (`GENESISDB_READ_TOKEN`), commit additionally with a writer token.

## Errors

Every non-2xx answer throws `GenesisDbException` (unchecked) with `getStatus()`, `getBody()` and `getOperation()`. Subclasses: `PreconditionFailedException` (412, with `getPreconditionType()` and `getReason()`), `AuthenticationException` (401, 403), `NotFoundException` (404), `ConflictException` (409), `UnprocessableException` (422), `NotReadyException` (503). Transport failures throw `GenesisDbException` with status 0 and the cause attached.

## Configuration

```java
Client.builder()
    .url("http://localhost:8080")       // or GENESISDB_URL
    .token("...")                       // or GENESISDB_AUTH_TOKEN
    .timeout(Duration.ofSeconds(30))    // for non-streaming calls, null disables
    .http(HttpClient.newHttpClient())   // optional, bring your own HttpClient (proxies, TLS, executor)
    .build();
```

## Development

```bash
mvn test                                                                        # unit tests against WireMock
GENESISDB_URL=http://localhost:8080 GENESISDB_AUTH_TOKEN=... mvn test           # also runs ClientIntegrationTest against a running GenesisDB
```

## License

MIT
