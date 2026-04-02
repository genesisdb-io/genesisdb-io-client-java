package io.genesisdb.client;

import io.cloudevents.CloudEvent;

/**
 * Callback interface for observing events from GenesisDB.
 * Implement this interface to receive events as they are streamed.
 */
@FunctionalInterface
public interface EventObserver {
    /**
     * Called when an event is received.
     *
     * @param event the received CloudEvent
     */
    void onEvent(CloudEvent event);
}
