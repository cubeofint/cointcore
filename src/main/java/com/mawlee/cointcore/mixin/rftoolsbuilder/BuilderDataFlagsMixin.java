package com.mawlee.cointcore.mixin.rftoolsbuilder;

import mcjty.rftoolsbuilder.modules.builder.data.BuilderData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Default Flags have {@code waitMode=true}. Always report false so synced GUI and logic stay on don't-wait.
 */
@Mixin(value = BuilderData.Flags.class, remap = false)
public abstract class BuilderDataFlagsMixin {
    @Inject(method = "waitMode", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$forceDontWait(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
