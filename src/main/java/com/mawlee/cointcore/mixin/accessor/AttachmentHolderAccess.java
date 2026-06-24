package com.mawlee.cointcore.mixin.accessor;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AttachmentHolder.class)
public interface AttachmentHolderAccess {
    @Invoker("deserializeAttachments")
    void cointcore$deserializeAttachments(HolderLookup.Provider registries, CompoundTag tag);
}
