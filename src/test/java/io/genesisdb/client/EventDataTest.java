package io.genesisdb.client;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EventDataTest {

    @Test
    void builder_withRequiredFields_createsEvent() {
        EventData event = EventData.builder()
                .source("io.genesisdb.app")
                .subject("/customer")
                .type("io.genesisdb.app.customer-added")
                .build();

        assertEquals("io.genesisdb.app", event.getSource());
        assertEquals("/customer", event.getSubject());
        assertEquals("io.genesisdb.app.customer-added", event.getType());
        assertNull(event.getData());
        assertNull(event.getOptions());
    }

    @Test
    void builder_withAllFields_createsEvent() {
        Map<String, Object> data = Map.of(
                "firstName", "Bruce",
                "lastName", "Wayne",
                "emailAddress", "bruce.wayne@enterprise.wayne"
        );

        EventOptions options = EventOptions.builder()
                .storeDataAsReference(true)
                .build();

        EventData event = EventData.builder()
                .source("io.genesisdb.app")
                .subject("/customer")
                .type("io.genesisdb.app.customer-added")
                .data(data)
                .options(options)
                .build();

        assertEquals("io.genesisdb.app", event.getSource());
        assertEquals("/customer", event.getSubject());
        assertEquals("io.genesisdb.app.customer-added", event.getType());
        assertEquals(data, event.getData());
        assertNotNull(event.getOptions());
        assertTrue(event.getOptions().getStoreDataAsReference());
    }

    @Test
    void builder_withoutSource_throwsException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                EventData.builder()
                        .subject("/customer")
                        .type("io.genesisdb.app.customer-added")
                        .build()
        );
        assertEquals("source is required", exception.getMessage());
    }

    @Test
    void builder_withEmptySource_throwsException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                EventData.builder()
                        .source("")
                        .subject("/customer")
                        .type("io.genesisdb.app.customer-added")
                        .build()
        );
        assertEquals("source is required", exception.getMessage());
    }

    @Test
    void builder_withoutSubject_throwsException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                EventData.builder()
                        .source("io.genesisdb.app")
                        .type("io.genesisdb.app.customer-added")
                        .build()
        );
        assertEquals("subject is required", exception.getMessage());
    }

    @Test
    void builder_withEmptySubject_throwsException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                EventData.builder()
                        .source("io.genesisdb.app")
                        .subject("")
                        .type("io.genesisdb.app.customer-added")
                        .build()
        );
        assertEquals("subject is required", exception.getMessage());
    }

    @Test
    void builder_withoutType_throwsException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                EventData.builder()
                        .source("io.genesisdb.app")
                        .subject("/customer")
                        .build()
        );
        assertEquals("type is required", exception.getMessage());
    }

    @Test
    void builder_withEmptyType_throwsException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                EventData.builder()
                        .source("io.genesisdb.app")
                        .subject("/customer")
                        .type("")
                        .build()
        );
        assertEquals("type is required", exception.getMessage());
    }

    @Test
    void builder_withNestedData_createsEvent() {
        Map<String, Object> address = Map.of(
                "street", "1007 Mountain Drive",
                "city", "Gotham"
        );
        Map<String, Object> data = Map.of(
                "name", "Bruce Wayne",
                "address", address
        );

        EventData event = EventData.builder()
                .source("io.genesisdb.app")
                .subject("/customer")
                .type("io.genesisdb.app.customer-added")
                .data(data)
                .build();

        @SuppressWarnings("unchecked")
        Map<String, Object> eventData = (Map<String, Object>) event.getData();
        assertEquals("Bruce Wayne", eventData.get("name"));

        @SuppressWarnings("unchecked")
        Map<String, Object> eventAddress = (Map<String, Object>) eventData.get("address");
        assertEquals("Gotham", eventAddress.get("city"));
    }

    @Test
    void builder_withSubjectHierarchy_createsEvent() {
        EventData event = EventData.builder()
                .source("io.genesisdb.app")
                .subject("/customer/fed2902d-0135-460d-8605-263a06308448")
                .type("io.genesisdb.app.customer-updated")
                .build();

        assertEquals("/customer/fed2902d-0135-460d-8605-263a06308448", event.getSubject());
    }

    @Test
    void builder_withPrimitiveData_createsEvent() {
        EventData event = EventData.builder()
                .source("io.genesisdb.app")
                .subject("/counter")
                .type("io.genesisdb.app.counter-incremented")
                .data(42)
                .build();

        assertEquals(42, event.getData());
    }

    @Test
    void builder_withStringData_createsEvent() {
        EventData event = EventData.builder()
                .source("io.genesisdb.app")
                .subject("/message")
                .type("io.genesisdb.app.message-sent")
                .data("Hello, World!")
                .build();

        assertEquals("Hello, World!", event.getData());
    }
}
