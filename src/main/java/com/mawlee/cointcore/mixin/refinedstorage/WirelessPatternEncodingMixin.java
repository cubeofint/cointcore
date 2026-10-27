package com.mawlee.cointcore.mixin.refinedstorage;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mawlee.cointcore.refinedstorage.wireless.WirelessPatternBackend;
import com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternGridBlockEntity;
import com.refinedmods.refinedstorage.common.support.FilteredContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = PatternGridBlockEntity.class, remap = false)
public abstract class WirelessPatternEncodingMixin {
    @WrapOperation(method = "createPattern", at = @At(value = "INVOKE",
            target = "Lcom/refinedmods/refinedstorage/common/support/FilteredContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V"),
            require = 1)
    private void cointcore$consumeBlankOnSuccessfulEncoding(FilteredContainer output, int slot, ItemStack result,
                                                          Operation<Void> original) {
        // RS has validated and built the recipe at this point. Only publishing a new output costs a blank;
        // re-encoding the existing output retains RS's normal behavior.
        if ((Object) this instanceof WirelessPatternBackend wireless && output.getItem(slot).isEmpty()
                && !wireless.consumeNetworkPattern()) return;
        original.call(output, slot, result);
    }
}
