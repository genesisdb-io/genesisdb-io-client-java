package io.genesisdb.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StreamOptionsTest {

    @Test
    void builder_withLowerBound_createsOptions() {
        StreamOptions options = StreamOptions.builder()
                .lowerBound("2d6d4141-6107-4fb2-905f-445730f4f2a9")
                .build();

        assertEquals("2d6d4141-6107-4fb2-905f-445730f4f2a9", options.getLowerBound());
        assertNull(options.getIncludeLowerBoundEvent());
        assertNull(options.getLatestByEventType());
    }

    @Test
    void builder_withLowerBoundAndInclude_createsOptions() {
        StreamOptions options = StreamOptions.builder()
                .lowerBound("2d6d4141-6107-4fb2-905f-445730f4f2a9")
                .includeLowerBoundEvent(true)
                .build();

        assertEquals("2d6d4141-6107-4fb2-905f-445730f4f2a9", options.getLowerBound());
        assertTrue(options.getIncludeLowerBoundEvent());
        assertNull(options.getLatestByEventType());
    }

    @Test
    void builder_withIncludeLowerBoundEventFalse_createsOptions() {
        StreamOptions options = StreamOptions.builder()
                .lowerBound("some-event-id")
                .includeLowerBoundEvent(false)
                .build();

        assertFalse(options.getIncludeLowerBoundEvent());
    }

    @Test
    void builder_withLatestByEventType_createsOptions() {
        StreamOptions options = StreamOptions.builder()
                .latestByEventType("io.genesisdb.app.customer-updated")
                .build();

        assertNull(options.getLowerBound());
        assertNull(options.getIncludeLowerBoundEvent());
        assertEquals("io.genesisdb.app.customer-updated", options.getLatestByEventType());
    }

    @Test
    void builder_withUpperBound_createsOptions() {
        StreamOptions options = StreamOptions.builder()
                .upperBound("abc-456")
                .build();

        assertEquals("abc-456", options.getUpperBound());
        assertNull(options.getIncludeUpperBoundEvent());
        assertNull(options.getLowerBound());
        assertNull(options.getLatestByEventType());
    }

    @Test
    void builder_withUpperBoundAndInclude_createsOptions() {
        StreamOptions options = StreamOptions.builder()
                .upperBound("abc-456")
                .includeUpperBoundEvent(true)
                .build();

        assertEquals("abc-456", options.getUpperBound());
        assertTrue(options.getIncludeUpperBoundEvent());
    }

    @Test
    void builder_withIncludeUpperBoundEventFalse_createsOptions() {
        StreamOptions options = StreamOptions.builder()
                .upperBound("some-event-id")
                .includeUpperBoundEvent(false)
                .build();

        assertFalse(options.getIncludeUpperBoundEvent());
    }

    @Test
    void builder_withBothBounds_createsOptions() {
        StreamOptions options = StreamOptions.builder()
                .lowerBound("event-id-123")
                .includeLowerBoundEvent(true)
                .upperBound("event-id-456")
                .includeUpperBoundEvent(false)
                .build();

        assertEquals("event-id-123", options.getLowerBound());
        assertTrue(options.getIncludeLowerBoundEvent());
        assertEquals("event-id-456", options.getUpperBound());
        assertFalse(options.getIncludeUpperBoundEvent());
    }

    @Test
    void builder_withAllOptions_createsOptions() {
        StreamOptions options = StreamOptions.builder()
                .lowerBound("event-id-123")
                .includeLowerBoundEvent(true)
                .upperBound("event-id-456")
                .includeUpperBoundEvent(true)
                .latestByEventType("io.genesisdb.app.event-type")
                .build();

        assertEquals("event-id-123", options.getLowerBound());
        assertTrue(options.getIncludeLowerBoundEvent());
        assertEquals("event-id-456", options.getUpperBound());
        assertTrue(options.getIncludeUpperBoundEvent());
        assertEquals("io.genesisdb.app.event-type", options.getLatestByEventType());
    }

    @Test
    void builder_withNoOptions_createsEmptyOptions() {
        StreamOptions options = StreamOptions.builder().build();

        assertNull(options.getLowerBound());
        assertNull(options.getIncludeLowerBoundEvent());
        assertNull(options.getUpperBound());
        assertNull(options.getIncludeUpperBoundEvent());
        assertNull(options.getLatestByEventType());
    }

    @Test
    void builder_canBeChained() {
        StreamOptions options = StreamOptions.builder()
                .lowerBound("id1")
                .lowerBound("id2")
                .build();

        assertEquals("id2", options.getLowerBound());
    }
}
