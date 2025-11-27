package io.genesisdb.client;

/**
 * Options for streaming and observing events.
 */
public class StreamOptions {
    private final String lowerBound;
    private final Boolean includeLowerBoundEvent;
    private final String latestByEventType;

    private StreamOptions(Builder builder) {
        this.lowerBound = builder.lowerBound;
        this.includeLowerBoundEvent = builder.includeLowerBoundEvent;
        this.latestByEventType = builder.latestByEventType;
    }

    public String getLowerBound() {
        return lowerBound;
    }

    public Boolean getIncludeLowerBoundEvent() {
        return includeLowerBoundEvent;
    }

    public String getLatestByEventType() {
        return latestByEventType;
    }

    /**
     * Creates a new builder for StreamOptions.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for StreamOptions.
     */
    public static class Builder {
        private String lowerBound;
        private Boolean includeLowerBoundEvent;
        private String latestByEventType;

        /**
         * Sets the lower bound event ID for streaming.
         *
         * @param lowerBound the event ID to start streaming from
         * @return this builder
         */
        public Builder lowerBound(String lowerBound) {
            this.lowerBound = lowerBound;
            return this;
        }

        /**
         * Sets whether to include the lower bound event in results.
         *
         * @param includeLowerBoundEvent true to include the lower bound event
         * @return this builder
         */
        public Builder includeLowerBoundEvent(boolean includeLowerBoundEvent) {
            this.includeLowerBoundEvent = includeLowerBoundEvent;
            return this;
        }

        /**
         * Sets the event type to filter for latest events only.
         *
         * @param latestByEventType the event type to filter by
         * @return this builder
         */
        public Builder latestByEventType(String latestByEventType) {
            this.latestByEventType = latestByEventType;
            return this;
        }

        /**
         * Builds the StreamOptions instance.
         *
         * @return the configured StreamOptions
         */
        public StreamOptions build() {
            return new StreamOptions(this);
        }
    }
}
