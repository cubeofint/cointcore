package com.mawlee.cointcore.mixin.ae2;

import appeng.me.helpers.InterestManager;
import appeng.me.service.StorageService;
import com.mawlee.cointcore.config.Ae2PerfConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ThreadLocalRandom;

/**
 * AE2 rebuilds a grid's cached inventory every server tick while anything watches it (level emitters, open
 * terminals, …): a full {@code getAvailableStacks} over every cell and storage bus, 6–7.6% of the server thread in
 * profiles. Rebuild every {@code storage_cache_interval_ticks} instead; each grid starts at a random phase so big
 * grids do not land on the same tick. Watchers and terminals see changes up to that many ticks later; extraction,
 * insertion and crafting go to the storage directly and are not delayed.
 *
 * <p>Verified against AE2 19.2.18 ({@code StorageService#onServerEndTick}).
 */
@Mixin(value = StorageService.class, remap = false)
public abstract class StorageServiceCacheThrottleMixin {
    @Shadow
    @Final
    private InterestManager<?> interestManager;

    @Unique
    private int cointcore$ticksUntilRefresh = ThreadLocalRandom.current().nextInt(5);

    @Inject(method = "onServerEndTick", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$throttleCacheRebuild(CallbackInfo ci) {
        int interval = Ae2PerfConfig.getStorageCacheIntervalTicks();
        // Without watchers AE2 only marks the cache dirty and rebuilds lazily on demand: leave that alone.
        if (interval <= 1 || interestManager.isEmpty()) {
            return;
        }
        if (cointcore$ticksUntilRefresh > 0) {
            cointcore$ticksUntilRefresh--;
            ci.cancel();
            return;
        }
        cointcore$ticksUntilRefresh = interval - 1;
    }
}
