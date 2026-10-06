package com.mawlee.cointcore.mixin.railcraft;

import com.mawlee.cointcore.config.ChunkLoaderRestrictConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Railcraft World Spike force-loads a 3×3 chunk area around the block.
 */
@Mixin(targets = "mods.railcraft.world.level.block.worldspike.WorldSpikeBlock", remap = false)
public abstract class WorldSpikeBlockMixin {
    @Inject(method = "forceChunk", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$disableWorldSpike(
            ServerLevel serverLevel,
            BlockPos pos,
            boolean add,
            CallbackInfo ci
    ) {
        if (add && ChunkLoaderRestrictConfig.isDisableRailcraftWorldSpike()) {
            ci.cancel();
        }
    }
}
