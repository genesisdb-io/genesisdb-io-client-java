package io.genesisdb.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Narrow a fetch or a subscription. Bounds are event IDs. Immutable, every method returns a copy. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class FetchOptions {
    private final String lowerBound;
    private final Boolean includeLowerBoundEvent;
    private final String upperBound;
    private final Boolean includeUpperBoundEvent;
    private final String latestByEventType;

    public FetchOptions() {
        this(null, null, null, null, null);
    }

    private FetchOptions(String lowerBound, Boolean includeLowerBoundEvent, String upperBound, Boolean includeUpperBoundEvent, String latestByEventType) {
        this.lowerBound = lowerBound;
        this.includeLowerBoundEvent = includeLowerBoundEvent;
        this.upperBound = upperBound;
        this.includeUpperBoundEvent = includeUpperBoundEvent;
        this.latestByEventType = latestByEventType;
    }

    /** Skip every event before this ID. An ID that does not occur below the subject yields an empty result. */
    public FetchOptions lowerBound(String id, boolean inclusive) {
        return new FetchOptions(id, inclusive, upperBound, includeUpperBoundEvent, latestByEventType);
    }

    /** Stop at this ID. */
    public FetchOptions upperBound(String id, boolean inclusive) {
        return new FetchOptions(lowerBound, includeLowerBoundEvent, id, inclusive, latestByEventType);
    }

    /** Return only the newest event of this type below the subject. Ignored by subscriptions. */
    public FetchOptions latestByEventType(String eventType) {
        return new FetchOptions(lowerBound, includeLowerBoundEvent, upperBound, includeUpperBoundEvent, eventType);
    }

    @JsonProperty("lowerBound")
    public String getLowerBound() {
        return lowerBound;
    }

    @JsonProperty("includeLowerBoundEvent")
    public Boolean getIncludeLowerBoundEvent() {
        return includeLowerBoundEvent;
    }

    @JsonProperty("upperBound")
    public String getUpperBound() {
        return upperBound;
    }

    @JsonProperty("includeUpperBoundEvent")
    public Boolean getIncludeUpperBoundEvent() {
        return includeUpperBoundEvent;
    }

    @JsonProperty("latestByEventType")
    public String getLatestByEventType() {
        return latestByEventType;
    }

    boolean isEmpty() {
        return lowerBound == null && includeLowerBoundEvent == null && upperBound == null && includeUpperBoundEvent == null && latestByEventType == null;
    }
}
