package com.mawlee.cointcore.mixin.adastra;

import com.mawlee.cointcore.adastra.AdAstraLocalOverrides;
import earth.terrarium.adastra.api.systems.OxygenApi;
import earth.terrarium.adastra.api.systems.TemperatureApi;
import earth.terrarium.adastra.common.systems.EnvironmentEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Planet climate samples keep the configured count. Chunks without a saved
 * oxygen or temperature override use the dimension value instead of planet data.
 */
@Mixin(value = EnvironmentEffects.class, remap = false)
public class AdAstraEnvironmentTickMixin {
    @Redirect(
            method = "tickChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Learth/terrarium/adastra/api/systems/TemperatureApi;getTemperature(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)S"
            )
    )
    private static short cointcore$sampleTemperature(TemperatureApi api, Level level, BlockPos pos) {
        if (level instanceof ServerLevel server && !AdAstraLocalOverrides.hasLocalClimate(server, pos)) {
            return AdAstraLocalOverrides.dimensionTemperature(level);
        }
        return api.getTemperature(level, pos);
    }

    @Redirect(
            method = "tickChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Learth/terrarium/adastra/api/systems/OxygenApi;hasOxygen(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z"
            )
    )
    private static boolean cointcore$sampleOxygen(OxygenApi api, Level level, BlockPos pos) {
        if (level instanceof ServerLevel server && !AdAstraLocalOverrides.hasLocalClimate(server, pos)) {
            return AdAstraLocalOverrides.dimensionHasOxygen(level);
        }
        return api.hasOxygen(level, pos);
    }
}
