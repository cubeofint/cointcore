package com.mawlee.cointcore.mixin.evilcraft;

import com.mawlee.cointcore.environment.TimeWeatherCooldown;
import net.minecraft.server.level.ServerLevel;
import org.cyclops.evilcraft.core.weather.WeatherTypeLightning;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Public helper used by lightning weather; writes rain/thunder times on {@code ServerLevelData}.
 */
@Mixin(value = WeatherTypeLightning.class, remap = false)
public abstract class WeatherTypeLightningMixin {

    @Inject(method = "activateThunder", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cointcore$gateActivateThunder(ServerLevel level, CallbackInfo ci) {
        if (!TimeWeatherCooldown.tryAllowForcedChange()) {
            ci.cancel();
        }
    }
}
