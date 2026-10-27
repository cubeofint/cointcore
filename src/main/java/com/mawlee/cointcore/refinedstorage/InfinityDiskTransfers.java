package com.mawlee.cointcore.refinedstorage;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Storage;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeAwareChild.Amount;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeStorage;

import java.util.function.Supplier;

public final class InfinityDiskTransfers {
    private InfinityDiskTransfers() {
    }

    /** The drive's configured proxy normally assumes every extracted item was removed from its list. */
    public static Amount extract(Storage delegate, ResourceKey resource, Supplier<Amount> transfer) {
        if (!(delegate instanceof CompositeStorage composite)
                || composite.getSources().stream().noneMatch(InfinityStateTrackedStorage.class::isInstance)) {
            return transfer.get();
        }
        long before = count(delegate, resource);
        Amount extracted = transfer.get();
        return new Amount(extracted.amount(), before - count(delegate, resource));
    }

    private static long count(Storage storage, ResourceKey resource) {
        return storage.getAll().stream().filter(entry -> entry.resource().equals(resource))
                .mapToLong(entry -> entry.amount()).sum();
    }
}
