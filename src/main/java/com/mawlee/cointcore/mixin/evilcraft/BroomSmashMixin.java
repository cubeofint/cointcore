package com.mawlee.cointcore.mixin.evilcraft;

import org.cyclops.evilcraft.entity.item.EntityBroom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Smash walks the blocks in front of a ridden broom and breaks them every tick.
 */
@Mixin(targets = "org.cyclops.evilcraft.api.broom.BroomModifiers$3", remap = false)
public abstract class BroomSmashMixin {

    @Inject(method = "onTick", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$noBroomBlockBreak(EntityBroom broom, float modifierLevel, CallbackInfo ci) {
        ci.cancel();
    }
}
