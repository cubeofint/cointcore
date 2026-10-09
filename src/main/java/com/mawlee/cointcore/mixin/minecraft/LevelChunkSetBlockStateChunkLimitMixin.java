package com.mawlee.cointcore.mixin.minecraft;

import com.mawlee.cointcore.chunklimit.ChunkLimitIndex;
import com.mawlee.cointcore.config.ChunkLimitConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps {@link ChunkLimitIndex} in sync for every block change (player, piston, Create, etc.).
 * The previous state comes from the return value, so nested neighbour updates cannot clobber it,
 * and the current state is re-read in case onPlace/onRemove already replaced the block again.
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkSetBlockStateChunkLimitMixin {
    @Inject(
            method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("RETURN")
    )
    private void cointcore$updateChunkLimitIndex(
            BlockPos pos,
            BlockState state,
            boolean isMoving,
            CallbackInfoReturnable<BlockState> cir
    ) {
        BlockState oldState = cir.getReturnValue();
        if (oldState == null || !ChunkLimitConfig.isEnabled() || !ChunkLimitConfig.hasAnyBlockLimits()) {
            return;
        }
        LevelChunk chunk = (LevelChunk) (Object) this;
        if (!(chunk.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ChunkLimitIndex.onBlockChange(level, pos, oldState, chunk.getBlockState(pos));
    }
}
