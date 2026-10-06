package com.mawlee.cointcore.mixin.immersiveengineering;

import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.ChunkLoaderLogic;
import com.mawlee.cointcore.config.ChunkLoaderRestrictConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Immersive Engineering Resonanz Observer (chunk loader multiblock).
 * Cancel force-load additions; releases still run when {@code add=false}.
 */
@Mixin(value = ChunkLoaderLogic.class, remap = false)
public abstract class ChunkLoaderLogicMixin {
    @Inject(method = "forceChunks", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$disableIeChunkLoader(IMultiblockContext<?> ctx, boolean add, CallbackInfo ci) {
        if (add && ChunkLoaderRestrictConfig.isDisableIeChunkLoader()) {
            ci.cancel();
        }
    }
}
