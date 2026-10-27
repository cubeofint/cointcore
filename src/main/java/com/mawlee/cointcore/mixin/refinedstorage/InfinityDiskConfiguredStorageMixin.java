package com.mawlee.cointcore.mixin.refinedstorage;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mawlee.cointcore.refinedstorage.InfinityDiskTransfers;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.impl.storage.AbstractConfiguredProxyStorage;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.Storage;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeAwareChild.Amount;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AbstractConfiguredProxyStorage.class, remap = false)
public abstract class InfinityDiskConfiguredStorageMixin {
    @Shadow
    protected abstract Storage getUnsafeDelegate();

    @WrapMethod(method = "compositeExtract", remap = false)
    private Amount cointcore$keepSourceAmount(ResourceKey resource, long amount, Action action, Actor actor,
                                             Operation<Amount> original) {
        // Simulation must retain RS's original accounting: it does not change the underlying list.
        if (action != Action.EXECUTE) {
            return original.call(resource, amount, action, actor);
        }
        return InfinityDiskTransfers.extract(getUnsafeDelegate(), resource,
                () -> original.call(resource, amount, action, actor));
    }
}
