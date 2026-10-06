package com.mawlee.cointcore.mixin.rootsclassic;

import com.mawlee.cointcore.environment.TimeWeatherCooldown;
import elucent.rootsclassic.ritual.rituals.RitualCauseRain;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = RitualCauseRain.class, remap = false)
public abstract class RitualCauseRainMixin {

    @Inject(method = "doEffect", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$gateCauseRain(
            Level level,
            BlockPos pos,
            Container inventory,
            List<ItemStack> ingredients,
            CallbackInfo ci
    ) {
        if (!TimeWeatherCooldown.tryAllowForcedChange()) {
            ci.cancel();
        }
    }
}
