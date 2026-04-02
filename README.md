# Java/Kotlin SDK

This is the official Java/Kotlin SDK for GenesisDB, an awesome and production ready event store database system for building event-driven apps.

## GenesisDB Advantages

* Incredibly fast when reading, fast when writing
* Easy backup creation and recovery
* [CloudEvents](https://cloudevents.io/) compatible
* GDPR-ready
* Easily accessible via the HTTP interface
* Auditable. Guarantee database consistency
* Logging and metrics for Prometheus
* SQL like query language called GenesisDB Query Language (GDBQL)
* ...

## Installation

### Maven

```xml
<dependency>
    <groupId>io.genesisdb</groupId>
    <artifactId>genesisdb</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.genesisdb:genesisdb:1.0.0'
```

### Gradle (Kotlin DSL)

```kotlin
implementation("io.genesisdb:genesisdb:1.0.0")
```

## Configuration

### Environment Variables
The following environment variables are required:
```
GENESISDB_AUTH_TOKEN=<secret>
GENESISDB_API_URL=http://localhost:8080
GENESISDB_API_VERSION=v1
```

### Basic Setup

**Java:**
```java
import io.genesisdb.client.Client;

// Initialize the GenesisDB client (using environment variables)
Client client = new Client();
```

**Kotlin:**
```kotlin
import io.genesisdb.client.Client

// Initialize the GenesisDB client (using environment variables)
val client = Client()
```

### Explicit Configuration

**Java:**
```java
import io.genesisdb.client.Client;
import io.genesisdb.client.ClientConfig;

Client client = new Client(ClientConfig.builder()
    .apiUrl("http://localhost:8080")
    .apiVersion("v1")
    .authToken("your-token")
    .build());
```

**Kotlin:**
```kotlin
import io.genesisdb.client.Client
import io.genesisdb.client.ClientConfig

val client = Client(ClientConfig.builder()
    .apiUrl("http://localhost:8080")
    .apiVersion("v1")
    .authToken("your-token")
    .build())
```

## Streaming Events

### Basic Event Streaming

**Java:**
```java
// Stream all events for a subject
List<CloudEvent> events = client.streamEvents("/customer");
```

**Kotlin:**
```kotlin
// Stream all events for a subject
val events = client.streamEvents("/customer")
```

### Stream Events from Lower Bound

**Java:**
```java
List<CloudEvent> events = client.streamEvents("/", StreamOptions.builder()
    .lowerBound("2d6d4141-6107-4fb2-905f-445730f4f2a9")
    .includeLowerBoundEvent(true)
    .build());
```

**Kotlin:**
```kotlin
val events = client.streamEvents("/", StreamOptions.builder()
    .lowerBound("2d6d4141-6107-4fb2-905f-445730f4f2a9")
    .includeLowerBoundEvent(true)
    .build())
```

### Stream Events with Upper Bound

**Java:**
```java
List<CloudEvent> events = client.streamEvents("/", StreamOptions.builder()
    .upperBound("9f3e4141-7208-4fb2-905f-445730f4f3b1")
    .includeUpperBoundEvent(false)
    .build());
```

**Kotlin:**
```kotlin
val events = client.streamEvents("/", StreamOptions.builder()
    .upperBound("9f3e4141-7208-4fb2-905f-445730f4f3b1")
    .includeUpperBoundEvent(false)
    .build())
```

### Stream Events with Both Lower and Upper Bounds

**Java:**
```java
List<CloudEvent> events = client.streamEvents("/", StreamOptions.builder()
    .lowerBound("2d6d4141-6107-4fb2-905f-445730f4f2a9")
    .includeLowerBoundEvent(true)
    .upperBound("9f3e4141-7208-4fb2-905f-445730f4f3b1")
    .includeUpperBoundEvent(false)
    .build());
```

**Kotlin:**
```kotlin
val events = client.streamEvents("/", StreamOptions.builder()
    .lowerBound("2d6d4141-6107-4fb2-905f-445730f4f2a9")
    .includeLowerBoundEvent(true)
    .upperBound("9f3e4141-7208-4fb2-905f-445730f4f3b1")
    .includeUpperBoundEvent(false)
    .build())
```

### Stream Latest Events by Event Type

**Java:**
```java
List<CloudEvent> events = client.streamEvents("/", StreamOptions.builder()
    .latestByEventType("io.genesisdb.app.customer-updated")
    .build());
```

**Kotlin:**
```kotlin
val events = client.streamEvents("/", StreamOptions.builder()
    .latestByEventType("io.genesisdb.app.customer-updated")
    .build())
```

This feature allows you to stream only the latest event of a specific type for each subject. Useful for getting the current state of entities.

## Committing Events

### Basic Event Committing

**Java:**
```java
import io.genesisdb.client.EventData;
import java.util.List;
import java.util.Map;

client.commitEvents(List.of(
    EventData.builder()
        .source("io.genesisdb.app")
        .subject("/customer")
        .type("io.genesisdb.app.customer-added")
        .data(Map.of(
            "firstName", "Bruce",
            "lastName", "Wayne",
            "emailAddress", "bruce.wayne@enterprise.wayne"
        ))
        .build(),
    EventData.builder()
        .source("io.genesisdb.app")
        .subject("/customer")
        .type("io.genesisdb.app.customer-added")
        .data(Map.of(
            "firstName", "Alfred",
            "lastName", "Pennyworth",
            "emailAddress", "alfred.pennyworth@enterprise.wayne"
        ))
        .build(),
    EventData.builder()
        .source("io.genesisdb.store")
        .subject("/article")
        .type("io.genesisdb.store.article-added")
        .data(Map.of(
            "name", "Tumbler",
            "color", "black",
            "price", 2990000.00
        ))
        .build(),
    EventData.builder()
        .source("io.genesisdb.app")
        .subject("/customer/fed2902d-0135-460d-8605-263a06308448")
        .type("io.genesisdb.app.customer-personaldata-changed")
        .data(Map.of(
            "firstName", "Angus",
            "lastName", "MacGyver",
            "emailAddress", "angus.macgyer@phoenix.foundation"
        ))
        .build()
));
```

**Kotlin:**
```kotlin
import io.genesisdb.client.EventData

client.commitEvents(listOf(
    EventData.builder()
        .source("io.genesisdb.app")
        .subject("/customer")
        .type("io.genesisdb.app.customer-added")
        .data(mapOf(
            "firstName" to "Bruce",
            "lastName" to "Wayne",
            "emailAddress" to "bruce.wayne@enterprise.wayne"
        ))
        .build(),
    EventData.builder()
        .source("io.genesisdb.app")
        .subject("/customer")
        .type("io.genesisdb.app.customer-added")
        .data(mapOf(
            "firstName" to "Alfred",
            "lastName" to "Pennyworth",
            "emailAddress" to "alfred.pennyworth@enterprise.wayne"
        ))
        .build(),
    EventData.builder()
        .source("io.genesisdb.store")
        .subject("/article")
        .type("io.genesisdb.store.article-added")
        .data(mapOf(
            "name" to "Tumbler",
            "color" to "black",
            "price" to 2990000.00
        ))
        .build(),
    EventData.builder()
        .source("io.genesisdb.app")
        .subject("/customer/fed2902d-0135-460d-8605-263a06308448")
        .type("io.genesisdb.app.customer-personaldata-changed")
        .data(mapOf(
            "firstName" to "Angus",
            "lastName" to "MacGyver",
            "emailAddress" to "angus.macgyer@phoenix.foundation"
        ))
        .build()
))
```

## Preconditions

Preconditions allow you to enforce certain checks on the server before committing events. GenesisDB supports multiple precondition types:

### isSubjectNew
Ensures that a subject is new (has no existing events):

**Java:**
```java
import io.genesisdb.client.Precondition;

client.commitEvents(List.of(
    EventData.builder()
        .source("io.genesisdb.app")
        .subject("/user/456")
        .type("io.genesisdb.app.user-created")
        .data(Map.of(
            "firstName", "John",
            "lastName", "Doe",
            "email", "john.doe@example.com"
        ))
        .build()
), List.of(
    Precondition.isSubjectNew("/user/456")
));
```

**Kotlin:**
```kotlin
import io.genesisdb.client.Precondition

client.commitEvents(listOf(
    EventData.builder()
        .source("io.genesisdb.app")
        .subject("/user/456")
        .type("io.genesisdb.app.user-created")
        .data(mapOf(
            "firstName" to "John",
            "lastName" to "Doe",
            "email" to "john.doe@example.com"
        ))
        .build()
), listOf(
    Precondition.isSubjectNew("/user/456")
))
```

### isSubjectExisting
Ensures that events exist for the specified subject:

**Java:**
```java
client.commitEvents(List.of(
    EventData.builder()
        .source("io.genesisdb.app")
        .subject("/user/456")
        .type("io.genesisdb.app.user-created")
        .data(Map.of(
            "firstName", "John",
            "lastName", "Doe",
            "email", "john.doe@example.com"
        ))
        .build()
), List.of(
    Precondition.isSubjectExisting("/user/456")
));
```

**Kotlin:**
```kotlin
client.commitEvents(listOf(
    EventData.builder()
        .source("io.genesisdb.app")
        .subject("/user/456")
        .type("io.genesisdb.app.user-created")
        .data(mapOf(
            "firstName" to "John",
            "lastName" to "Doe",
            "email" to "john.doe@example.com"
        ))
        .build()
), listOf(
    Precondition.isSubjectExisting("/user/456")
))
```

### isQueryResultTrue
Evaluates a query and ensures the result is truthy. Supports the full GDBQL feature set including complex WHERE clauses, aggregations, and calculated fields.

**Basic uniqueness check (Java):**
```java
client.commitEvents(List.of(
    EventData.builder()
        .source("io.genesisdb.app")
        .subject("/user/456")
        .type("io.genesisdb.app.user-created")
        .data(Map.of(
            "firstName", "John",
            "lastName", "Doe",
            "email", "john.doe@example.com"
        ))
        .build()
), List.of(
    Precondition.isQueryResultTrue(
        "STREAM e FROM events WHERE e.data.email == 'john.doe@example.com' MAP COUNT() == 0"
    )
));
```

**Business rule enforcement (transaction limits) (Java):**
```java
client.commitEvents(List.of(
    EventData.builder()
        .source("io.genesisdb.banking")
        .subject("/user/123/transactions")
        .type("io.genesisdb.banking.transaction-processed")
        .data(Map.of(
            "amount", 500.00,
            "currency", "EUR"
        ))
        .build()
), List.of(
    Precondition.isQueryResultTrue(
        "STREAM e FROM events WHERE e.subject UNDER '/user/123' AND e.type == 'transaction-processed' AND e.time >= '2024-01-01T00:00:00Z' MAP SUM(e.data.amount) + 500 <= 10000"
    )
));
```

**Complex validation with aggregations (Kotlin):**
```kotlin
client.commitEvents(listOf(
    EventData.builder()
        .source("io.genesisdb.events")
        .subject("/conference/2024/registrations")
        .type("io.genesisdb.events.registration-created")
        .data(mapOf(
            "attendeeId" to "att-789",
            "ticketType" to "premium"
        ))
        .build()
), listOf(
    Precondition.isQueryResultTrue(
        "STREAM e FROM events WHERE e.subject UNDER '/conference/2024/registrations' AND e.type == 'registration-created' GROUP BY e.data.ticketType HAVING e.data.ticketType == 'premium' MAP COUNT() < 50"
    )
))
```

**Supported GDBQL Features in Preconditions:**
- WHERE conditions with AND/OR/IN/BETWEEN operators
- Hierarchical subject queries (UNDER, DESCENDANTS)
- Aggregation functions (COUNT, SUM, AVG, MIN, MAX)
- GROUP BY with HAVING clauses
- ORDER BY and LIMIT clauses
- Calculated fields and expressions
- Nested field access (e.data.address.city)
- String concatenation and arithmetic operations

If a precondition fails, the commit throws a `GenesisDbException` with HTTP 412 (Precondition Failed). You can check this with `exception.isPreconditionFailed()`.

### Generic Precondition (Forward Compatibility)

For precondition types not yet covered by the SDK (forward-compatible with future server releases), use `Precondition.of()`:

**Java:**
```java
client.commitEvents(List.of(
    EventData.builder()
        .source("io.genesisdb.app")
        .subject("/test")
        .type("io.genesisdb.app.test-created")
        .data(Map.of("key", "value"))
        .build()
), List.of(
    Precondition.of("customPreconditionType", Map.of("param", "value"))
));
```

**Kotlin:**
```kotlin
client.commitEvents(listOf(
    EventData.builder()
        .source("io.genesisdb.app")
        .subject("/test")
        .type("io.genesisdb.app.test-created")
        .data(mapOf("key" to "value"))
        .build()
), listOf(
    Precondition.of("customPreconditionType", mapOf("param" to "value"))
))
```

## GDPR Compliance

### Store Data as Reference

**Java:**
```java
import io.genesisdb.client.EventOptions;

client.commitEvents(List.of(
    EventData.builder()
        .source("io.genesisdb.app")
        .subject("/user/456")
        .type("io.genesisdb.app.user-created")
        .data(Map.of(
            "firstName", "John",
            "lastName", "Doe",
            "email", "john.doe@example.com"
        ))
        .options(EventOptions.builder()
            .storeDataAsReference(true)
            .build())
        .build()
));
```

**Kotlin:**
```kotlin
import io.genesisdb.client.EventOptions

client.commitEvents(listOf(
    EventData.builder()
        .source("io.genesisdb.app")
        .subject("/user/456")
        .type("io.genesisdb.app.user-created")
        .data(mapOf(
            "firstName" to "John",
            "lastName" to "Doe",
            "email" to "john.doe@example.com"
        ))
        .options(EventOptions.builder()
            .storeDataAsReference(true)
            .build())
        .build()
))
```

### Delete Referenced Data

**Java:**
```java
client.eraseData("/user/456");
```

**Kotlin:**
```kotlin
client.eraseData("/user/456")
```

## Observing Events

### Basic Event Observation

**Java:**
```java
import io.genesisdb.client.EventObserver;

Client.ObserveHandle handle = client.observeEvents("/customer", event -> {
    System.out.println("Received event: " + event);
});

// Later, to stop observing:
handle.stop();
```

**Kotlin:**
```kotlin
val handle = client.observeEvents("/customer") { event ->
    println("Received event: $event")
}

// Later, to stop observing:
handle.stop()
```

### Observe Events from Lower Bound (Message Queue)

**Java:**
```java
client.observeEvents("/customer", StreamOptions.builder()
    .lowerBound("2d6d4141-6107-4fb2-905f-445730f4f2a9")
    .includeLowerBoundEvent(true)
    .build(),
    event -> System.out.println("Received event: " + event)
);
```

**Kotlin:**
```kotlin
client.observeEvents("/customer", StreamOptions.builder()
    .lowerBound("2d6d4141-6107-4fb2-905f-445730f4f2a9")
    .includeLowerBoundEvent(true)
    .build()
) { event ->
    println("Received event: $event")
}
```

### Observe Events with Upper Bound (Message Queue)

**Java:**
```java
client.observeEvents("/customer", StreamOptions.builder()
    .upperBound("9f3e4141-7208-4fb2-905f-445730f4f3b1")
    .includeUpperBoundEvent(false)
    .build(),
    event -> System.out.println("Received event: " + event)
);
```

**Kotlin:**
```kotlin
client.observeEvents("/customer", StreamOptions.builder()
    .upperBound("9f3e4141-7208-4fb2-905f-445730f4f3b1")
    .includeUpperBoundEvent(false)
    .build()
) { event ->
    println("Received event: $event")
}
```

### Observe Events with Both Bounds (Message Queue)

**Java:**
```java
client.observeEvents("/customer", StreamOptions.builder()
    .lowerBound("2d6d4141-6107-4fb2-905f-445730f4f2a9")
    .includeLowerBoundEvent(true)
    .upperBound("9f3e4141-7208-4fb2-905f-445730f4f3b1")
    .includeUpperBoundEvent(false)
    .build(),
    event -> System.out.println("Received event: " + event)
);
```

**Kotlin:**
```kotlin
client.observeEvents("/customer", StreamOptions.builder()
    .lowerBound("2d6d4141-6107-4fb2-905f-445730f4f2a9")
    .includeLowerBoundEvent(true)
    .upperBound("9f3e4141-7208-4fb2-905f-445730f4f3b1")
    .includeUpperBoundEvent(false)
    .build()
) { event ->
    println("Received event: $event")
}
```

### Observe Latest Events by Event Type (Message Queue)

**Java:**
```java
client.observeEvents("/customer", StreamOptions.builder()
    .latestByEventType("io.genesisdb.app.customer-updated")
    .build(),
    event -> System.out.println("Received latest event: " + event)
);
```

**Kotlin:**
```kotlin
client.observeEvents("/customer", StreamOptions.builder()
    .latestByEventType("io.genesisdb.app.customer-updated")
    .build()
) { event ->
    println("Received latest event: $event")
}
```

## Querying Events

**Java:**
```java
List<Map<String, Object>> results = client.queryEvents(
    "STREAM e FROM events WHERE e.type == \"io.genesisdb.app.customer-added\" ORDER BY e.time DESC LIMIT 20 MAP { subject: e.subject, firstName: e.data.firstName }"
);
System.out.println("Query results: " + results);
```

**Kotlin:**
```kotlin
val results = client.queryEvents(
    """STREAM e FROM events WHERE e.type == "io.genesisdb.app.customer-added" ORDER BY e.time DESC LIMIT 20 MAP { subject: e.subject, firstName: e.data.firstName }"""
)
println("Query results: $results")
```

## Health Checks

**Java:**
```java
// Check API status
String pingResponse = client.ping();
System.out.println("Ping response: " + pingResponse);

// Run audit to check event consistency
String auditResponse = client.audit();
System.out.println("Audit response: " + auditResponse);
```

**Kotlin:**
```kotlin
// Check API status
val pingResponse = client.ping()
println("Ping response: $pingResponse")

// Run audit to check event consistency
val auditResponse = client.audit()
println("Audit response: $auditResponse")
```

## Error Handling

**Java:**
```java
import io.genesisdb.client.GenesisDbException;

try {
    client.commitEvents(events, preconditions);
} catch (GenesisDbException e) {
    if (e.isPreconditionFailed()) {
        System.out.println("Precondition failed: " + e.getMessage());
    } else {
        System.out.println("API error: " + e.getStatusCode() + " " + e.getStatusText());
    }
}
```

**Kotlin:**
```kotlin
import io.genesisdb.client.GenesisDbException

try {
    client.commitEvents(events, preconditions)
} catch (e: GenesisDbException) {
    if (e.isPreconditionFailed) {
        println("Precondition failed: ${e.message}")
    } else {
        println("API error: ${e.statusCode} ${e.statusText}")
    }
}
```

## Resource Cleanup

**Java:**
```java
// When done with the client
client.close();
```

**Kotlin:**
```kotlin
// When done with the client
client.close()
```

## License

MIT

## Author

* E-Mail: mail@genesisdb.io
* URL: https://www.genesisdb.io
* Docs: https://docs.genesisdb.io
