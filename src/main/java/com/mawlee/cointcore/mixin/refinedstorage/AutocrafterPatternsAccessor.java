package com.mawlee.cointcore.mixin.refinedstorage;

import com.refinedmods.refinedstorage.common.autocrafting.PatternInventory;
import com.refinedmods.refinedstorage.common.autocrafting.autocrafter.AutocrafterBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AutocrafterBlockEntity.class, remap = false)
public interface AutocrafterPatternsAccessor {
    @Accessor("patternContainer") PatternInventory cointcore$patterns();
}
