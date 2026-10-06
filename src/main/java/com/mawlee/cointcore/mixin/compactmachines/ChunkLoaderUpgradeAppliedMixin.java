package com.mawlee.cointcore.mixin.compactmachines;

import com.mawlee.cointcore.config.ChunkLoaderRestrictConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks Compact Machines room chunk-loader upgrade from force-loading room chunks.
 */
@Mixin(
        targets = "dev.compactmods.machines.room.upgrade.example.ChunkLoaderUpgradeComponent$AppliedHandler",
        remap = false
)
public abstract class ChunkLoaderUpgradeAppliedMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$disableChunkLoaderUpgrade(@Coerce Object instance, CallbackInfo ci) {
        if (ChunkLoaderRestrictConfig.isDisableCompactMachinesChunkloader()) {
            ci.cancel();
        }
    }
}
