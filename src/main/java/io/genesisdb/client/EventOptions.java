package io.genesisdb.client;

/**
 * Options for event storage, including GDPR compliance settings.
 */
public class EventOptions {
    private final Boolean storeDataAsReference;

    private EventOptions(Builder builder) {
        this.storeDataAsReference = builder.storeDataAsReference;
    }

    public Boolean getStoreDataAsReference() {
        return storeDataAsReference;
    }

    /**
     * Creates a new builder for EventOptions.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for EventOptions.
     */
    public static class Builder {
        private Boolean storeDataAsReference;

        /**
         * Sets whether to store data as a reference for GDPR compliance.
         * When true, the data can be erased later using eraseData().
         *
         * @param storeDataAsReference true to store data as reference
         * @return this builder
         */
        public Builder storeDataAsReference(boolean storeDataAsReference) {
            this.storeDataAsReference = storeDataAsReference;
            return this;
        }

        /**
         * Builds the EventOptions instance.
         *
         * @return the configured EventOptions
         */
        public EventOptions build() {
            return new EventOptions(this);
        }
    }
}
