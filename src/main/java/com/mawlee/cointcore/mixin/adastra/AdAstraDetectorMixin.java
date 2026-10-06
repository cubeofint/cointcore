package com.mawlee.cointcore.mixin.adastra;

import com.mawlee.cointcore.adastra.AdAstraLocalOverrides;
import earth.terrarium.adastra.common.blockentities.machines.DetectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A detector whose neighboring blocks still match the dimension skips the six
 * planet-data reads. A saved override keeps the original neighbor walk.
 */
@Mixin(value = DetectorBlockEntity.class, remap = false)
public class AdAstraDetectorMixin {
    @Inject(method = "hasOxygen", at = @At("HEAD"), cancellable = true)
    private void cointcore$dimensionOxygen(
            ServerLevel level,
            BlockPos pos,
            boolean inverted,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (AdAstraLocalOverrides.areaHasLocalClimate(level, pos)) {
            return;
        }
        cir.setReturnValue(AdAstraLocalOverrides.dimensionHasOxygen(level) != inverted);
    }

    @Inject(method = "hasNormalGravity", at = @At("HEAD"), cancellable = true)
    private void cointcore$dimensionGravity(
            ServerLevel level,
            BlockPos pos,
            boolean inverted,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (AdAstraLocalOverrides.areaHasCustomGravity(level, pos)) {
            return;
        }
        float meters = AdAstraLocalOverrides.dimensionGravity(level) * 9.807F;
        boolean normal = meters > 8.807F && meters < 10.807F;
        cir.setReturnValue(normal != inverted);
    }

    @Inject(method = "hasSafeTemperature", at = @At("HEAD"), cancellable = true)
    private void cointcore$dimensionTemperature(
            ServerLevel level,
            BlockPos pos,
            boolean inverted,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (AdAstraLocalOverrides.areaHasLocalClimate(level, pos)) {
            return;
        }
        cir.setReturnValue(AdAstraLocalOverrides.isLivable(AdAstraLocalOverrides.dimensionTemperature(level)) != inverted);
    }
}
