package com.mawlee.cointcore.mixin.refinedstorage;

import com.refinedmods.refinedstorage.common.RefinedStorageApiImpl;
import com.refinedmods.refinedstorage.common.support.slotreference.CompositeSlotReferenceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = RefinedStorageApiImpl.class, remap = false)
public interface TerminalSlotProviderAccessor {
    @Accessor("slotReferenceProvider")
    CompositeSlotReferenceProvider cointcore$slotReferences();
}
