package com.mawlee.cointcore.mixin.refinedstorage;

import com.refinedmods.refinedstorage.common.autocrafting.preview.AutocraftingPreviewContainerMenu;
import com.refinedmods.refinedstorage.common.autocrafting.preview.AutocraftingRequest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AutocraftingPreviewContainerMenu.class, remap = false)
public interface BatchPreviewMenuAccessor {
    @Accessor("currentRequest") AutocraftingRequest cointcore$currentRequest();
}
