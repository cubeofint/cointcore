package com.mawlee.cointcore.refinedstorage;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.StateTrackedStorage;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeAwareChild;
import com.refinedmods.refinedstorage.api.storage.composite.ParentComposite;

/** Preserves RS drive state tracking while reporting zero change to the network's cached resource list. */
public final class InfinityStateTrackedStorage extends StateTrackedStorage implements CompositeAwareChild {
    public InfinityStateTrackedStorage(InfinityResourceStorage storage, Listener listener) {
        super(storage, listener);
    }

    @Override
    public Amount compositeExtract(ResourceKey resource, long amount, Action action, Actor actor) {
        return new Amount(extract(resource, amount, action, actor), 0);
    }

    @Override
    public Amount compositeInsert(ResourceKey resource, long amount, Action action, Actor actor) {
        return Amount.ZERO;
    }

    @Override
    public void onAddedIntoComposite(ParentComposite parent) {
        // Constant contents; no asynchronous changes to propagate.
    }

    @Override
    public void onRemovedFromComposite(ParentComposite parent) {
    }
}
