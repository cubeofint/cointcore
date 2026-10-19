package com.mawlee.cointcore.mixin.justdirethings;

import com.direwolf20.justdirethings.common.blockentities.ClickerT1BE;
import com.direwolf20.justdirethings.common.blockentities.basebe.BaseMachineBE;
import com.mawlee.cointcore.config.JustDireThingsPerfConfig;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla clears the chunk place/break protection cache every tick, so area scans never
 * reuse {@code EventHooks.onBlockPlace} results. Allow clearing only every N game ticks.
 */
@Mixin(value = BaseMachineBE.class, remap = false)
public abstract class BaseMachineBEMixin {
    @Shadow(remap = false)
    protected int tickSpeed;

    @Inject(method = "clearProtectionCache", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$throttleProtectionCacheClear(CallbackInfo ci) {
        if (!JustDireThingsPerfConfig.isEnabled()) {
            return;
        }
        int interval = JustDireThingsPerfConfig.getProtectionCacheClearIntervalTicks();
        if (interval <= 1) {
            return;
        }
        Level level = ((BaseMachineBE) (Object) this).getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        if (Math.floorMod(level.getGameTime(), interval) != 0) {
            ci.cancel();
        }
    }

    /**
     * Clickers (T1 and T2, which extends T1) may not click faster than {@code clickerMinTickSpeed}.
     * {@code handleTicks} reads the field directly, so raising it here also fixes values loaded from NBT
     * or set in the GUI; the GUI then shows the clamped value.
     */
    @Inject(method = "handleTicks", at = @At("HEAD"), remap = false)
    private void cointcore$clampClickerTickSpeed(CallbackInfo ci) {
        int min = JustDireThingsPerfConfig.getClickerMinTickSpeed();
        if (min > 1 && tickSpeed < min && (Object) this instanceof ClickerT1BE) {
            tickSpeed = min;
        }
    }
}
