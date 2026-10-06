package com.mawlee.cointcore.mixin.minecraft;

import com.mawlee.cointcore.environment.TimeWeatherCooldown;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

/**
 * Gates forced {@code setDayTime}/{@code setWeatherParameters} behind a shared cooldown.
 * Natural progression ({@code tickTime}, sleep skip, {@code advanceWeatherCycle}) is marked and
 * always allowed.
 * <p>
 * DE Celestial Manipulator time warp is detected by call stack (its tile class cannot be mixed on
 * dedicated servers — it embeds {@code ClientLevel} in common bytecode).
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelTimeWeatherMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void cointcore$celestialWarpWatchdog(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        TimeWeatherCooldown.tickCelestialWarpWatchdog();
    }

    @Inject(method = "tickTime", at = @At("HEAD"))
    private void cointcore$enterNaturalDayTime(CallbackInfo ci) {
        TimeWeatherCooldown.enterNatural();
    }

    @Inject(method = "tickTime", at = @At("RETURN"))
    private void cointcore$exitNaturalDayTime(CallbackInfo ci) {
        TimeWeatherCooldown.exitNatural();
    }

    @Inject(method = "advanceWeatherCycle", at = @At("HEAD"))
    private void cointcore$enterNaturalWeatherCycle(CallbackInfo ci) {
        TimeWeatherCooldown.enterNatural();
    }

    @Inject(method = "advanceWeatherCycle", at = @At("RETURN"))
    private void cointcore$exitNaturalWeatherCycle(CallbackInfo ci) {
        TimeWeatherCooldown.exitNatural();
    }

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;setDayTime(J)V"
            )
    )
    private void cointcore$enterNaturalSleepDayTime(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        TimeWeatherCooldown.enterNatural();
    }

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;setDayTime(J)V",
                    shift = At.Shift.AFTER
            )
    )
    private void cointcore$exitNaturalSleepDayTime(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        TimeWeatherCooldown.exitNatural();
    }

    @Inject(method = "setDayTime", at = @At("HEAD"), cancellable = true)
    private void cointcore$gateForcedDayTime(long time, CallbackInfo ci) {
        // Ops / console /time must always apply (cooldown is for votes & mod spam only).
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

    @Inject(method = "setWeatherParameters", at = @At("HEAD"), cancellable = true)
    private void cointcore$gateForcedWeather(int clearTime, int weatherTime, boolean raining, boolean thundering, CallbackInfo ci) {
        if (TimeWeatherCooldown.isVanillaTimeOrWeatherCommandCaller()) {
            TimeWeatherCooldown.enterPassThrough();
            return;
        }
        if (!TimeWeatherCooldown.tryAllowForcedChange()) {
            ci.cancel();
            return;
        }
        // Body writes ServerLevelData/LevelData fields — allow nested gated setters.
        TimeWeatherCooldown.enterPassThrough();
    }

    @Inject(method = "setWeatherParameters", at = @At("RETURN"))
    private void cointcore$exitForcedWeather(int clearTime, int weatherTime, boolean raining, boolean thundering, CallbackInfo ci) {
        if (TimeWeatherCooldown.isPassThrough()) {
            TimeWeatherCooldown.exitPassThrough();
        }
    }
}
