package com.mawlee.cointcore.mixin.mobgrindingutils;

import com.mawlee.cointcore.spawner.MachineSpawnLoot;
import mob_grinding_utils.tile.TileEntityMGUSpawner;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TileEntityMGUSpawner.class, remap = false)
public abstract class MguSpawnerLootMixin {

    @Redirect(
            method = "spawnMobInArea",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"
            ),
            remap = true
    )
    private boolean cointcore$lootInsteadOfMob(Level level, Entity entity) {
        return MachineSpawnLoot.captureFresh(level, entity, ((BlockEntity) (Object) this).getBlockPos());
    }
}
