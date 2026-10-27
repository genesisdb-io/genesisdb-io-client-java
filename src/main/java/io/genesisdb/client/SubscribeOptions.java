package io.genesisdb.client;

import java.time.Duration;

/** Configure a subscription. Immutable, every method returns a copy. */
public final class SubscribeOptions {
    private final FetchOptions fetch;
    private final Duration interval;

    public SubscribeOptions() {
        this(new FetchOptions(), null);
    }

    private SubscribeOptions(FetchOptions fetch, Duration interval) {
        this.fetch = fetch;
        this.interval = interval;
    }

    /** Poll interval. Null uses the server default of one second. Clamped by the server to 100 ms .. 24 h. */
    public SubscribeOptions interval(Duration interval) {
        return new SubscribeOptions(fetch, interval);
    }

    /** Bounds, see {@link FetchOptions}. latestByEventType is ignored by subscriptions. */
    public SubscribeOptions fetch(FetchOptions fetch) {
        return new SubscribeOptions(fetch == null ? new FetchOptions() : fetch, interval);
    }

    public FetchOptions getFetch() {
        return fetch;
    }

    public Duration getInterval() {
        return interval;
    }
}
