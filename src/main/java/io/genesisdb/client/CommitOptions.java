package io.genesisdb.client;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Per-event options for a commit. */
public final class CommitOptions {
    public static final CommitOptions STORE_AS_REFERENCE = new CommitOptions(true);

    private final boolean storeDataAsReference;

    public CommitOptions(boolean storeDataAsReference) {
        this.storeDataAsReference = storeDataAsReference;
    }

    @JsonProperty("storeDataAsReference")
    public boolean isStoreDataAsReference() {
        return storeDataAsReference;
    }
}
