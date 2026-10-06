package com.mawlee.cointcore.mixin.adastra;

import com.mawlee.cointcore.adastra.AdAstraLocalOverrides;
import earth.terrarium.adastra.common.systems.TemperatureApiImpl;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Burn and freeze only run for a hostile dimension temperature or a chunk that
 * received an extreme per-block temperature. Livable space still skips the
 * armor scan.
 */
@Mixin(value = TemperatureApiImpl.class, remap = false)
public abstract class AdAstraTemperatureTickMixin {
    @Inject(method = "entityTick", at = @At("HEAD"), cancellable = true)
    private void cointcore$skipLivableDimension(ServerLevel level, LivingEntity entity, CallbackInfo ci) {
        if (!AdAstraLocalOverrides.isLivable(AdAstraLocalOverrides.dimensionTemperature(level))) {
            return;
        }
        if (AdAstraLocalOverrides.hasExtremeTemperature(level, entity.blockPosition())) {
            return;
        }
        ci.cancel();
    }
}
