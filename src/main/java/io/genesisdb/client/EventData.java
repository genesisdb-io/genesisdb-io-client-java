package io.genesisdb.client;

/**
 * Represents an event to be committed to Genesis DB.
 */
public class EventData {
    private final String source;
    private final String subject;
    private final String type;
    private final Object data;
    private final EventOptions options;

    private EventData(Builder builder) {
        this.source = builder.source;
        this.subject = builder.subject;
        this.type = builder.type;
        this.data = builder.data;
        this.options = builder.options;
    }

    public String getSource() {
        return source;
    }

    public String getSubject() {
        return subject;
    }

    public String getType() {
        return type;
    }

    public Object getData() {
        return data;
    }

    public EventOptions getOptions() {
        return options;
    }

    /**
     * Creates a new builder for EventData.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for EventData.
     */
    public static class Builder {
        private String source;
        private String subject;
        private String type;
        private Object data;
        private EventOptions options;

        /**
         * Sets the event source.
         *
         * @param source the source identifier (e.g., "io.genesisdb.app")
         * @return this builder
         */
        public Builder source(String source) {
            this.source = source;
            return this;
        }

        /**
         * Sets the event subject.
         *
         * @param subject the subject path (e.g., "/customer")
         * @return this builder
         */
        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        /**
         * Sets the event type.
         *
         * @param type the event type (e.g., "io.genesisdb.app.customer-added")
         * @return this builder
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the event data payload.
         *
         * @param data the event data (will be serialized to JSON)
         * @return this builder
         */
        public Builder data(Object data) {
            this.data = data;
            return this;
        }

        /**
         * Sets additional options for the event.
         *
         * @param options the event options
         * @return this builder
         */
        public Builder options(EventOptions options) {
            this.options = options;
            return this;
        }

        /**
         * Builds the EventData instance.
         *
         * @return the configured EventData
         * @throws IllegalStateException if required fields are missing
         */
        public EventData build() {
            if (source == null || source.isEmpty()) {
                throw new IllegalStateException("source is required");
            }
            if (subject == null || subject.isEmpty()) {
                throw new IllegalStateException("subject is required");
            }
            if (type == null || type.isEmpty()) {
                throw new IllegalStateException("type is required");
            }
            return new EventData(this);
        }
    }
}
