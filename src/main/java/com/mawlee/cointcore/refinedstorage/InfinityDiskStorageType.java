package com.mawlee.cointcore.refinedstorage;

import com.mojang.serialization.MapCodec;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.api.storage.SerializableStorage;
import com.refinedmods.refinedstorage.common.api.storage.StorageType;

import java.util.function.Supplier;

/** Each fixed-resource disk has its own type and a unit codec; nothing can be consumed or changed. */
final class InfinityDiskStorageType implements StorageType {
    private final Supplier<ResourceKey> resource;
    private final Supplier<StorageType> delegate;

    InfinityDiskStorageType(Supplier<ResourceKey> resource, Supplier<StorageType> delegate) {
        this.resource = resource;
        this.delegate = delegate;
    }

    InfinityResourceStorage createSource() {
        return new InfinityResourceStorage(java.util.Objects.requireNonNull(resource.get(), "Infinity disk resource is unavailable"), this);
    }

    boolean isAvailable() {
        return resource.get() != null;
    }

    @Override
    public SerializableStorage create(Long capacity, Runnable listener) {
        return createSource();
    }

    @Override
    public MapCodec<SerializableStorage> getMapCodec(Runnable listener) {
        return MapCodec.unit(this::createSource);
    }

    @Override
    public boolean isAllowed(ResourceKey key) {
        return key != null && key.equals(resource.get());
    }

    @Override
    public long getDiskInterfaceTransferQuota(boolean fast) {
        return delegate.get().getDiskInterfaceTransferQuota(fast);
    }
}
