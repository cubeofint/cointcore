package com.mawlee.cointcore.mixin.refinedstorage;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.autocrafting.preview.AutocraftingRequest;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AutocraftingRequest.class, remap = false)
public interface AutocraftingRequestAccessor {
    @Accessor("id") UUID cointcore$requestId();
    @Accessor("resource") ResourceKey cointcore$requestedResource();
}
