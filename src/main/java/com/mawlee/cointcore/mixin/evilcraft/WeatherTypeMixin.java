package com.mawlee.cointcore.mixin.evilcraft;

import com.mawlee.cointcore.environment.TimeWeatherCooldown;
import net.minecraft.server.level.ServerLevel;
import org.cyclops.evilcraft.core.weather.WeatherType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * EvilCraft weather rituals/machines call {@link WeatherType#activate(ServerLevel, boolean)},
 * which mutates {@code LevelData} directly instead of {@code setWeatherParameters}.
 */
@Mixin(value = WeatherType.class, remap = false)
public abstract class WeatherTypeMixin {

    @Inject(
            method = "activate(Lnet/minecraft/server/level/ServerLevel;Z)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void cointcore$gateWeatherToggle(ServerLevel level, boolean enable, CallbackInfo ci) {
        if (!TimeWeatherCooldown.tryAllowForcedChange()) {
            ci.cancel();
            return;
        }
        TimeWeatherCooldown.enterPassThrough();
    }

    @Inject(
            method = "activate(Lnet/minecraft/server/level/ServerLevel;Z)V",
            at = @At("RETURN"),
            remap = false
    )
    private void cointcore$exitWeatherToggle(ServerLevel level, boolean enable, CallbackInfo ci) {
        // Only paired with a successful HEAD that entered pass-through.
        if (TimeWeatherCooldown.isPassThrough()) {
            TimeWeatherCooldown.exitPassThrough();
        }
    }
}
