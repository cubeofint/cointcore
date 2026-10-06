package com.mawlee.cointcore.mixin.stevescarts;

import com.mawlee.cointcore.config.ChunkLoaderRestrictConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Steve's Carts chunk-loader module: block activation so carts cannot force-load chunks.
 */
@Mixin(targets = "vswe.stevescarts.modules.addons.ModuleChunkLoader", remap = false)
public abstract class ModuleChunkLoaderMixin {
    @Inject(method = "setChunkLoading", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$disableStevesChunkLoader(boolean val, CallbackInfo ci) {
        if (val && ChunkLoaderRestrictConfig.isDisableStevesCartsChunkLoader()) {
            ci.cancel();
        }
    }
}
