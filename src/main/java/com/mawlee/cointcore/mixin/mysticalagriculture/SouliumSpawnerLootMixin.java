package com.mawlee.cointcore.mixin.mysticalagriculture;

import com.blakebr0.mysticalagriculture.tileentity.SouliumSpawnerTileEntity;
import com.mawlee.cointcore.spawner.MachineSpawnLoot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Soulium spawner pays essence and energy, then the mob's death loot goes into
 * an adjacent inventory. The mob is not added to the world.
 */
@Mixin(value = SouliumSpawnerTileEntity.class, remap = false)
public abstract class SouliumSpawnerLootMixin {

    @Redirect(
            method = "attemptSpawn",
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
