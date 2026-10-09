package com.mawlee.cointcore.mixin.neoforge;

import com.mawlee.cointcore.chunklimit.ItemPlacementGuard;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.common.CommonHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CommonHooks.class, remap = false)
public abstract class CommonHooksItemPlacementMixin {
    @Inject(method = "onPlaceItemIntoWorld", at = @At("HEAD"))
    private static void cointcore$enterItemPlacement(
            UseOnContext context,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        ItemPlacementGuard.enter();
    }

    @Inject(method = "onPlaceItemIntoWorld", at = @At("RETURN"))
    private static void cointcore$exitItemPlacement(
            UseOnContext context,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        ItemPlacementGuard.exit();
    }
}
