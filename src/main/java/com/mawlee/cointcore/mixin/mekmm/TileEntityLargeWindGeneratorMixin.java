package com.mawlee.cointcore.mixin.mekmm;

import com.mawlee.cointcore.config.MekmmWindGeneratorScanConfig;
import com.mawlee.cointcore.mekmm.MekmmWindGeneratorNearbyScan;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Soft mixin: only applied when {@code mekmm} is loaded. Replaces
 * {@code checkSameBlockNearby()} (full world-height {@code getBlockState} scan
 * over a 5×5 chunk square) with a walk of loaded-chunk block entities.
 *
 * <p>Method name verified against mekmm-1.21.1-1.4.1
 * ({@code com.jerry.meklg.common.tile.generator.TileEntityLargeWindGenerator}).
 */
@Pseudo
@Mixin(
        targets = "com.jerry.meklg.common.tile.generator.TileEntityLargeWindGenerator",
        remap = false
)
public abstract class TileEntityLargeWindGeneratorMixin {
    @Unique
    private boolean cointcore$cachedNearby;

    @Unique
    private long cointcore$cachedAt = Long.MIN_VALUE;

    @Inject(method = "checkSameBlockNearby", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$scanLoadedBlockEntities(CallbackInfoReturnable<Boolean> cir) {
        if (!MekmmWindGeneratorScanConfig.isEnabled()) {
            return;
        }
        BlockEntity self = (BlockEntity) (Object) this;
        Level level = self.getLevel();
        long now = level != null ? level.getGameTime() : 0L;
        int interval = MekmmWindGeneratorScanConfig.getCacheIntervalTicks();
        if (interval > 0
                && cointcore$cachedAt != Long.MIN_VALUE
                && now - cointcore$cachedAt < interval) {
            cir.setReturnValue(cointcore$cachedNearby);
            return;
        }
        boolean found = MekmmWindGeneratorNearbyScan.hasSameBlockNearby(
                level,
                self.getBlockPos(),
                self.getBlockState().getBlock()
        );
        cointcore$cachedNearby = found;
        cointcore$cachedAt = now;
        cir.setReturnValue(found);
    }
}
