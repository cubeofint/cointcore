package com.mawlee.cointcore.mixin.refinedstorage;

import com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternGridBlockEntity;
import com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternGridContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = PatternGridContainerMenu.class, remap = false)
public interface WirelessPatternMenuConstructor {
    @Invoker("<init>")
    static PatternGridContainerMenu cointcore$create(int id, Inventory inventory, PatternGridBlockEntity backend) {
        throw new AssertionError("Mixin constructor invoker was not applied");
    }
}
