package com.mawlee.cointcore.mixin.draconicevolution;

import com.brandon3055.draconicevolution.lib.ExplosionHelper;
import com.mawlee.cointcore.config.ExplosionTerrainConfig;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Safety net for DE 3.1.4.x: {@code ExplosionHelper#removeBlock} still writes AIR into chunk
 * sections directly (private method, signature unchanged).
 */
@Mixin(value = ExplosionHelper.class, remap = false)
public abstract class ExplosionHelperMixin {

    @Inject(method = "removeBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$skipRemoveBlock(BlockPos pos, CallbackInfo ci) {
        if (ExplosionTerrainConfig.isEnabled()) {
            ci.cancel();
        }
    }
}
