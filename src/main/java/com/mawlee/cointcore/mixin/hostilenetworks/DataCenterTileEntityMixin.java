package com.mawlee.cointcore.mixin.hostilenetworks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mawlee.cointcore.config.ChunkLoaderRestrictConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hostile Neural Networks Data Center force-loads its multiblock shell chunks.
 * When disabled, allow releases ({@code add=false}) but block new force-loads.
 */
@Mixin(targets = "dev.shadowsoffire.hostilenetworks.tile.DataCenterTileEntity", remap = false)
public abstract class DataCenterTileEntityMixin {
    @WrapOperation(
            method = {"ensureShellChunksForced", "releaseAllForcedChunks"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/common/world/chunk/TicketController;forceChunk(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;IIZZ)Z"
            ),
            remap = false
    )
    private boolean cointcore$gateDataCenterForce(
            TicketController controller,
            ServerLevel level,
            BlockPos owner,
            int chunkX,
            int chunkZ,
            boolean add,
            boolean ticking,
            Operation<Boolean> original
    ) {
        if (add && ChunkLoaderRestrictConfig.isDisableHnnDataCenter()) {
            return false;
        }
        return original.call(controller, level, owner, chunkX, chunkZ, add, ticking);
    }
}
