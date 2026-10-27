package com.mawlee.cointcore.mixin.refinedstorage;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mawlee.cointcore.refinedstorage.InfinityResourceStorage;
import com.mawlee.cointcore.refinedstorage.InfinityStateTrackedStorage;
import com.refinedmods.refinedstorage.api.network.impl.node.AbstractStorageContainerNetworkNode;
import com.refinedmods.refinedstorage.api.storage.StateTrackedStorage;
import com.refinedmods.refinedstorage.api.storage.Storage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AbstractStorageContainerNetworkNode.class, remap = false)
public abstract class InfinityDiskStateTrackingMixin {
    @WrapOperation(
            method = "lambda$initializeStorage$0",
            at = @At(value = "NEW", target = "com/refinedmods/refinedstorage/api/storage/StateTrackedStorage"),
            require = 1,
            remap = false
    )
    private StateTrackedStorage cointcore$trackInfinitySource(Storage storage, StateTrackedStorage.Listener listener,
                                                           Operation<StateTrackedStorage> original) {
        if (storage instanceof InfinityResourceStorage infinite) {
            return new InfinityStateTrackedStorage(infinite, listener);
        }
        return original.call(storage, listener);
    }
}
