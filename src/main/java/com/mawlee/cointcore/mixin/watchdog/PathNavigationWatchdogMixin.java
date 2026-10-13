package com.mawlee.cointcore.mixin.watchdog;

import com.mawlee.cointcore.watchdog.WatchdogHotFlags;
import com.mawlee.cointcore.watchdog.probes.ProbeHooks;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PathNavigation.class)
public abstract class PathNavigationWatchdogMixin {
    @Shadow
    @Final
    protected Mob mob;

    @Inject(method = "recomputePath()V", at = @At("HEAD"), require = 0)
    private void cointcore$watchdogPathRecompute(CallbackInfo ci) {
        if (!WatchdogHotFlags.PROBE_PATHFINDING) {
            return;
        }
        ProbeHooks.onPathRecompute(this.mob);
    }
}
