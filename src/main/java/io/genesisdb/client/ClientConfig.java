package io.genesisdb.client;

/**
 * Configuration for the Genesis DB client.
 * Values can be provided directly or will be read from environment variables.
 */
public class ClientConfig {
    private final String apiUrl;
    private final String apiVersion;
    private final String authToken;

    private ClientConfig(Builder builder) {
        this.apiUrl = builder.apiUrl;
        this.apiVersion = builder.apiVersion;
        this.authToken = builder.authToken;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public String getAuthToken() {
        return authToken;
    }

    /**
     * Creates a new builder for ClientConfig.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ClientConfig.
     */
    public static class Builder {
        private String apiUrl;
        private String apiVersion;
        private String authToken;

        /**
         * Sets the API URL.
         *
         * @param apiUrl the Genesis DB API URL
         * @return this builder
         */
        public Builder apiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
            return this;
        }

        /**
         * Sets the API version.
         *
         * @param apiVersion the API version (e.g., "v1")
         * @return this builder
         */
        public Builder apiVersion(String apiVersion) {
            this.apiVersion = apiVersion;
            return this;
        }

        /**
         * Sets the authentication token.
         *
         * @param authToken the Bearer authentication token
         * @return this builder
         */
        public Builder authToken(String authToken) {
            this.authToken = authToken;
            return this;
        }

        /**
         * Builds the ClientConfig instance.
         *
         * @return the configured ClientConfig
         */
        public ClientConfig build() {
            return new ClientConfig(this);
        }
    }
}
