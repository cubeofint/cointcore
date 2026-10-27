package com.mawlee.cointcore.refinedstorage.wireless;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.limited.LimitedStorageImpl;

/** Bounded transaction destination; reject fuzzy variants and publish to the player only once. */
public final class PickBuffer extends LimitedStorageImpl {
    private final ResourceKey requested;

    public PickBuffer(ResourceKey requested, long capacity) {
        super(capacity);
        this.requested = requested;
    }

    @Override public long insert(ResourceKey resource, long amount, Action action, Actor actor) {
        return requested.equals(resource) && amount > 0 ? super.insert(resource, amount, action, actor) : 0;
    }
}
