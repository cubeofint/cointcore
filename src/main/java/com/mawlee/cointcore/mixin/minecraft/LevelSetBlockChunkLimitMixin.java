package com.mawlee.cointcore.mixin.minecraft;

import com.mawlee.cointcore.chunklimit.ChunkLimitIndex;
import com.mawlee.cointcore.config.ChunkLimitConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps {@link ChunkLimitIndex} in sync for all setBlock paths (player, piston, Create, etc.).
 */
@Mixin(Level.class)
public abstract class LevelSetBlockChunkLimitMixin {
    @Unique
    private static final ThreadLocal<BlockState> COINTCORE$OLD_STATE = new ThreadLocal<>();

    @Inject(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD")
    )
    private void cointcore$captureOldState(
            BlockPos pos,
            BlockState state,
            int flags,
            int recursionLeft,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!ChunkLimitConfig.isEnabled() || !ChunkLimitConfig.hasAnyBlockLimits()) {
            return;
        }
        if (!((Object) this instanceof ServerLevel level)) {
            return;
        }
        COINTCORE$OLD_STATE.set(level.getBlockState(pos));
    }

    @Inject(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("RETURN")
    )
    private void cointcore$updateChunkLimitIndex(
            BlockPos pos,
            BlockState state,
            int flags,
            int recursionLeft,
            CallbackInfoReturnable<Boolean> cir
    ) {
        BlockState oldState = COINTCORE$OLD_STATE.get();
        COINTCORE$OLD_STATE.remove();
        if (oldState == null || !cir.getReturnValueZ()) {
            return;
        }
        if (!ChunkLimitConfig.isEnabled() || !ChunkLimitConfig.hasAnyBlockLimits()) {
            return;
        }
        if (!((Object) this instanceof ServerLevel level)) {
            return;
        }
        ChunkLimitIndex.onBlockChange(level, pos, oldState, state);
    }
}
