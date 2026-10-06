package com.mawlee.cointcore.mixin.railcraft;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mawlee.cointcore.config.ChunkLoaderRestrictConfig;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

/**
 * Railcraft World Spike minecart uses the same chunk controller as the block form.
 */
@Mixin(targets = "mods.railcraft.world.entity.vehicle.WorldSpikeMinecart", remap = false)
public abstract class WorldSpikeMinecartMixin {
    @WrapOperation(
            method = {"tick", "remove"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/common/world/chunk/TicketController;forceChunk(Lnet/minecraft/server/level/ServerLevel;Ljava/util/UUID;IIZZ)Z"
            ),
            remap = false
    )
    private boolean cointcore$gateMinecartForce(
            TicketController controller,
            ServerLevel level,
            UUID owner,
            int chunkX,
            int chunkZ,
            boolean add,
            boolean ticking,
            Operation<Boolean> original
    ) {
        if (add && ChunkLoaderRestrictConfig.isDisableRailcraftWorldSpike()) {
            return false;
        }
        return original.call(controller, level, owner, chunkX, chunkZ, add, ticking);
    }
}
