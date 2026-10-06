package com.mawlee.cointcore.mixin.ae2;

import appeng.server.services.ChunkLoadingService;
import com.mawlee.cointcore.config.ChunkLoaderRestrictConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Disables AE2 Spatial Anchor force-loading when configured.
 * Spatial IO / storage cells are unaffected.
 */
@Mixin(value = ChunkLoadingService.class, remap = false)
public abstract class ChunkLoadingServiceMixin {
    @Inject(method = "forceChunk", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$disableSpatialAnchorForce(
            ServerLevel level,
            BlockPos owner,
            ChunkPos position,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (ChunkLoaderRestrictConfig.isDisableAe2SpatialAnchor()) {
            cir.setReturnValue(false);
        }
    }
}
