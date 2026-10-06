package com.mawlee.cointcore.mixin.adastra;

import com.mawlee.cointcore.adastra.AdAstraLocalOverrides;
import earth.terrarium.adastra.common.handlers.PlanetHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

/**
 * Records chunks whose saved planet data can disagree with the dimension, so
 * entity ticks keep the original oxygen, temperature, and gravity checks there.
 */
@Mixin(value = PlanetHandler.class, remap = false)
public abstract class AdAstraPlanetHandlerMixin {
    @Inject(method = "setGravity(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;F)V", at = @At("HEAD"))
    private static void cointcore$gravityPos(ServerLevel level, BlockPos pos, float gravity, CallbackInfo ci) {
        if (gravity != AdAstraLocalOverrides.dimensionGravity(level)) {
            AdAstraLocalOverrides.markCustomGravity(level, pos);
        }
    }

    @Inject(
            method = "setGravity(Lnet/minecraft/server/level/ServerLevel;Ljava/util/Collection;F)V",
            at = @At("HEAD")
    )
    private static void cointcore$gravityVolume(
            ServerLevel level,
            Collection<BlockPos> positions,
            float gravity,
            CallbackInfo ci
    ) {
        if (gravity == AdAstraLocalOverrides.dimensionGravity(level)) {
            return;
        }
        for (BlockPos pos : positions) {
            AdAstraLocalOverrides.markCustomGravity(level, pos);
        }
    }

    @Inject(method = "setOxygen(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Z)V", at = @At("HEAD"))
    private static void cointcore$oxygenPos(ServerLevel level, BlockPos pos, boolean oxygen, CallbackInfo ci) {
        if (oxygen != AdAstraLocalOverrides.dimensionHasOxygen(level)) {
            AdAstraLocalOverrides.markLocalClimate(level, pos);
        }
        if (!oxygen) {
            AdAstraLocalOverrides.markOxygenFalse(level, pos);
        }
    }

    @Inject(
            method = "setOxygen(Lnet/minecraft/server/level/ServerLevel;Ljava/util/Collection;Z)V",
            at = @At("HEAD")
    )
    private static void cointcore$oxygenVolume(
            ServerLevel level,
            Collection<BlockPos> positions,
            boolean oxygen,
            CallbackInfo ci
    ) {
        boolean differs = oxygen != AdAstraLocalOverrides.dimensionHasOxygen(level);
        if (!differs && oxygen) {
            return;
        }
        for (BlockPos pos : positions) {
            if (differs) {
                AdAstraLocalOverrides.markLocalClimate(level, pos);
            }
            if (!oxygen) {
                AdAstraLocalOverrides.markOxygenFalse(level, pos);
            }
        }
    }

    @Inject(method = "setTemperature(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;S)V", at = @At("HEAD"))
    private static void cointcore$temperaturePos(ServerLevel level, BlockPos pos, short temperature, CallbackInfo ci) {
        if (temperature != AdAstraLocalOverrides.dimensionTemperature(level)) {
            AdAstraLocalOverrides.markLocalClimate(level, pos);
        }
        if (AdAstraLocalOverrides.isExtreme(temperature)) {
            AdAstraLocalOverrides.markExtremeTemperature(level, pos);
        }
    }

    @Inject(
            method = "setTemperature(Lnet/minecraft/server/level/ServerLevel;Ljava/util/Collection;S)V",
            at = @At("HEAD")
    )
    private static void cointcore$temperatureVolume(
            ServerLevel level,
            Collection<BlockPos> positions,
            short temperature,
            CallbackInfo ci
    ) {
        boolean differs = temperature != AdAstraLocalOverrides.dimensionTemperature(level);
        boolean extreme = AdAstraLocalOverrides.isExtreme(temperature);
        if (!differs && !extreme) {
            return;
        }
        for (BlockPos pos : positions) {
            if (differs) {
                AdAstraLocalOverrides.markLocalClimate(level, pos);
            }
            if (extreme) {
                AdAstraLocalOverrides.markExtremeTemperature(level, pos);
            }
        }
    }
}
