package com.mawlee.cointcore.mixin.adastra;

import com.mawlee.cointcore.adastra.AdAstraDistributionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A changed block can open or close a sealed oxygen or gravity volume.
 * Unchanged writes, including the null return from an identical state, do not.
 */
@Mixin(LevelChunk.class)
public abstract class AdAstraChunkChangeMixin {
    @Inject(
            method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("RETURN")
    )
    private void cointcore$watchSealedVolume(
            BlockPos pos,
            BlockState state,
            boolean isMoving,
            CallbackInfoReturnable<BlockState> cir
    ) {
        BlockState previous = cir.getReturnValue();
        if (previous == null || previous.equals(state)) {
            return;
        }
        LevelChunk chunk = (LevelChunk) (Object) this;
        if (chunk.getLevel() instanceof ServerLevel level) {
            AdAstraDistributionCache.mark(level, pos);
        }
    }
}
