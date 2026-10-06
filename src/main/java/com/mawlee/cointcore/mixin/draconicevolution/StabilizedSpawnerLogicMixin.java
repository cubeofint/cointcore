package com.mawlee.cointcore.mixin.draconicevolution;

import com.brandon3055.draconicevolution.blocks.tileentity.StabilizedSpawnerLogic;
import com.brandon3055.draconicevolution.blocks.tileentity.TileStabilizedSpawner;
import com.mawlee.cointcore.spawner.MachineSpawnLoot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Stabilized Spawner builds the mob from a soul, then the death loot goes into an adjacent inventory.
 */
@Mixin(value = StabilizedSpawnerLogic.class, remap = false)
public abstract class StabilizedSpawnerLogicMixin {

    @Shadow
    private TileStabilizedSpawner tile;

    @Redirect(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;tryAddFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)Z"
            ),
            remap = true
    )
    private boolean cointcore$lootInsteadOfMob(ServerLevel level, Entity entity) {
        return MachineSpawnLoot.capturePassengers(level, entity, tile.getBlockPos());
    }
}
