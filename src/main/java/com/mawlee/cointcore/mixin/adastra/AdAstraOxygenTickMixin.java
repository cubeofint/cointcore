package com.mawlee.cointcore.mixin.adastra;

import com.mawlee.cointcore.adastra.AdAstraLocalOverrides;
import earth.terrarium.adastra.common.systems.OxygenApiImpl;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suffocation only happens where the dimension has no oxygen, or a block was
 * explicitly marked oxygen-false. Armor and tag scans stay on that path.
 */
@Mixin(value = OxygenApiImpl.class, remap = false)
public abstract class AdAstraOxygenTickMixin {
    @Inject(method = "entityTick", at = @At("HEAD"), cancellable = true)
    private void cointcore$skipBreathableDimension(ServerLevel level, LivingEntity entity, CallbackInfo ci) {
        if (!AdAstraLocalOverrides.dimensionHasOxygen(level)) {
            return;
        }
        if (AdAstraLocalOverrides.hasOxygenFalse(level, entity.blockPosition())) {
            return;
        }
        ci.cancel();
    }
}
