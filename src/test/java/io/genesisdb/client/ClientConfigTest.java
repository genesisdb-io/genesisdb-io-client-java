package io.genesisdb.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientConfigTest {

    @Test
    void builder_withAllValues_createsConfig() {
        ClientConfig config = ClientConfig.builder()
                .apiUrl("http://localhost:8080")
                .apiVersion("v1")
                .authToken("secret")
                .build();

        assertEquals("http://localhost:8080", config.getApiUrl());
        assertEquals("v1", config.getApiVersion());
        assertEquals("secret", config.getAuthToken());
    }

    @Test
    void builder_withPartialValues_createsConfigWithNulls() {
        ClientConfig config = ClientConfig.builder()
                .apiUrl("http://localhost:8080")
                .build();

        assertEquals("http://localhost:8080", config.getApiUrl());
        assertNull(config.getApiVersion());
        assertNull(config.getAuthToken());
    }

    @Test
    void builder_withNoValues_createsEmptyConfig() {
        ClientConfig config = ClientConfig.builder().build();

        assertNull(config.getApiUrl());
        assertNull(config.getApiVersion());
        assertNull(config.getAuthToken());
    }

    @Test
    void builder_canBeReused() {
        ClientConfig.Builder builder = ClientConfig.builder()
                .apiUrl("http://localhost:8080")
                .apiVersion("v1")
                .authToken("secret");

        ClientConfig config1 = builder.build();

        builder.authToken("token2");
        ClientConfig config2 = builder.build();

        assertEquals("secret", config1.getAuthToken());
        assertEquals("token2", config2.getAuthToken());
    }

    @Test
    void builder_withDifferentApiVersions() {
        ClientConfig v1Config = ClientConfig.builder()
                .apiUrl("http://localhost:8080")
                .apiVersion("v1")
                .authToken("secret")
                .build();

        ClientConfig v2Config = ClientConfig.builder()
                .apiUrl("http://localhost:8080")
                .apiVersion("v2")
                .authToken("secret")
                .build();

        assertEquals("v1", v1Config.getApiVersion());
        assertEquals("v2", v2Config.getApiVersion());
    }
}
