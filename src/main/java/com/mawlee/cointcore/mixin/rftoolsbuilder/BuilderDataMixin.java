package com.mawlee.cointcore.mixin.rftoolsbuilder;

import mcjty.rftoolsbuilder.modules.builder.data.BuilderData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * GUI / packets call {@code withWaitMode(true)} to enable stalling. Keep don't-wait permanently.
 */
@Mixin(value = BuilderData.class, remap = false)
public abstract class BuilderDataMixin {
    @ModifyVariable(method = "withWaitMode", at = @At("HEAD"), argsOnly = true, remap = false)
    private boolean cointcore$forceDontWaitFlag(boolean waitMode) {
        return false;
    }
}
