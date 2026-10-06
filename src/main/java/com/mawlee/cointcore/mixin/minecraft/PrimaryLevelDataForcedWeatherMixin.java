package com.mawlee.cointcore.mixin.minecraft;

import com.mawlee.cointcore.environment.TimeWeatherCooldown;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * DE Celestial Manipulator (3.1.4.x) writes weather via {@link PrimaryLevelData} directly
 * instead of {@code ServerLevel#setWeatherParameters}.
 * <p>
 * Must target the concrete class — {@code @Inject} into {@code LevelData}/{@code ServerLevelData}
 * interface methods has no code body and fails mixin apply (NPE in INJECT_PREPARE).
 */
@Mixin(PrimaryLevelData.class)
public abstract class PrimaryLevelDataForcedWeatherMixin {

    @Inject(method = "setRaining", at = @At("HEAD"), cancellable = true)
    private void cointcore$gateSetRaining(boolean raining, CallbackInfo ci) {
        cointcore$gate(ci);
    }

    @Inject(method = "setThundering", at = @At("HEAD"), cancellable = true)
    private void cointcore$gateSetThundering(boolean thundering, CallbackInfo ci) {
        cointcore$gate(ci);
    }

    @Inject(method = "setRainTime", at = @At("HEAD"), cancellable = true)
    private void cointcore$gateSetRainTime(int time, CallbackInfo ci) {
        cointcore$gate(ci);
    }

    @Inject(method = "setClearWeatherTime", at = @At("HEAD"), cancellable = true)
    private void cointcore$gateSetClearWeatherTime(int time, CallbackInfo ci) {
        cointcore$gate(ci);
    }

    @Inject(method = "setThunderTime", at = @At("HEAD"), cancellable = true)
    private void cointcore$gateSetThunderTime(int time, CallbackInfo ci) {
        cointcore$gate(ci);
    }

    private static void cointcore$gate(CallbackInfo ci) {
        if (TimeWeatherCooldown.isVanillaTimeOrWeatherCommandCaller()) {
            return;
        }
        if (TimeWeatherCooldown.isCelestialManipulatorCaller()) {
            if (!TimeWeatherCooldown.tryAllowCelestialForcedEnvironment()) {
                ci.cancel();
            }
            return;
        }
        if (!TimeWeatherCooldown.tryAllowForcedChange()) {
            ci.cancel();
        }
    }
}
