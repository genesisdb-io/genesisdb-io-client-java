package io.genesisdb.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventOptionsTest {

    @Test
    void builder_withStoreDataAsReferenceTrue_createsOptions() {
        EventOptions options = EventOptions.builder()
                .storeDataAsReference(true)
                .build();

        assertTrue(options.getStoreDataAsReference());
    }

    @Test
    void builder_withStoreDataAsReferenceFalse_createsOptions() {
        EventOptions options = EventOptions.builder()
                .storeDataAsReference(false)
                .build();

        assertFalse(options.getStoreDataAsReference());
    }

    @Test
    void builder_withNoOptions_createsEmptyOptions() {
        EventOptions options = EventOptions.builder().build();

        assertNull(options.getStoreDataAsReference());
    }

    @Test
    void storeDataAsReference_forGdprCompliance() {
        // Simulate GDPR use case - storing personal data as reference
        EventOptions gdprOptions = EventOptions.builder()
                .storeDataAsReference(true)
                .build();

        assertTrue(gdprOptions.getStoreDataAsReference());
    }
}
