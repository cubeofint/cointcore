package com.mawlee.cointcore.mixin.refinedstorage;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mawlee.cointcore.refinedstorage.wireless.WirelessPatternBackend;
import com.mawlee.cointcore.refinedstorage.wireless.BlockPatternSession;
import com.mawlee.cointcore.refinedstorage.wireless.NetworkPatternBlanks;
import com.mawlee.cointcore.refinedstorage.wireless.NetworkPatternOutput;
import com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternGridBlockEntity;
import com.refinedmods.refinedstorage.common.support.FilteredContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PatternGridBlockEntity.class, remap = false)
public abstract class WirelessPatternEncodingMixin implements NetworkPatternOutput {
    @Shadow @Final private FilteredContainer patternOutput;
    @Override public ItemStack cointcore$outputPattern() { return patternOutput.getItem(0); }

    @Inject(method = "isPatternAvailable", at = @At("HEAD"), cancellable = true, require = 1)
    private void cointcore$networkBlankAvailable(CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof WirelessPatternBackend wireless) {
            callback.setReturnValue(wireless.availableNetworkPatterns() > 0 || !patternOutput.isEmpty());
            return;
        }
        var session = BlockPatternSession.encoding(this);
        if (session != null) callback.setReturnValue(session.available() > 0
                || !patternOutput.isEmpty());
    }

    @WrapOperation(method = "createPattern", at = @At(value = "INVOKE",
            target = "Lcom/refinedmods/refinedstorage/common/support/FilteredContainer;removeItem(II)Lnet/minecraft/world/item/ItemStack;"), require = 1)
    private ItemStack cointcore$keepPhysicalInput(FilteredContainer input, int slot, int count, Operation<ItemStack> original) {
        return BlockPatternSession.encoding(this) == null ? original.call(input, slot, count) : ItemStack.EMPTY;
    }

    @WrapOperation(method = "createPattern", at = @At(value = "INVOKE",
            target = "Lcom/refinedmods/refinedstorage/common/support/FilteredContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V"),
            require = 1)
    private void cointcore$consumeBlankOnSuccessfulEncoding(FilteredContainer output, int slot, ItemStack result,
                                                          Operation<Void> original) {
        // RS has validated and built the recipe at this point. Only publishing a new output costs a blank;
        // re-encoding the existing output retains RS's normal behavior.
        if ((Object) this instanceof WirelessPatternBackend wireless) {
            if (output.getItem(slot).isEmpty() && !wireless.consumeNetworkPattern()) return;
            result = NetworkPatternBlanks.result(result, output.getItem(slot), wireless.networkPatternBlank());
        }
        var session = BlockPatternSession.encoding(this);
        if (session != null) {
            if (output.getItem(slot).isEmpty() && !session.consume()) return;
            result = NetworkPatternBlanks.result(result, output.getItem(slot), session.blank());
        }
        original.call(output, slot, result);
    }
}
