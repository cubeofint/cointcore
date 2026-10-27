package com.mawlee.cointcore.refinedstorage;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.api.storage.SerializableStorage;
import com.refinedmods.refinedstorage.common.api.storage.StorageType;

import java.util.Collection;
import java.util.List;

/** Stateless source: the disk item defines its resource, so no disk UUID or mutable save data is needed. */
public final class InfinityResourceStorage implements SerializableStorage {
    // A finite advertised amount avoids overflow when multiple disks are combined in a network.
    public static final long DISPLAY_AMOUNT = Integer.MAX_VALUE;
    private final ResourceKey resource;
    private final StorageType type;

    public InfinityResourceStorage(ResourceKey resource, StorageType type) {
        this.resource = resource;
        this.type = type;
    }

    @Override
    public long extract(ResourceKey requested, long amount, Action action, Actor actor) {
        return resource.equals(requested) && amount > 0 ? amount : 0;
    }

    @Override
    public long insert(ResourceKey requested, long amount, Action action, Actor actor) {
        // Source only: never silently void items returned to the network.
        return 0;
    }

    @Override
    public Collection<ResourceAmount> getAll() {
        return List.of(new ResourceAmount(resource, DISPLAY_AMOUNT));
    }

    @Override
    public long getStored() {
        // Advertised resources are virtual: they occupy no physical storage in the drive.
        // getAll() independently supplies the amount displayed in the network's resource list.
        return 0;
    }

    @Override
    public StorageType getType() {
        return type;
    }
}
