package com.mawlee.cointcore.mixin.industrialforegoing;

import com.buuz135.industrial.block.agriculturehusbandry.tile.MobDuplicatorTile;
import com.mawlee.cointcore.spawner.MachineSpawnLoot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Mob Duplicator pays its recipe, then the mob's death loot goes into an adjacent inventory.
 */
@Mixin(value = MobDuplicatorTile.class, remap = false)
public abstract class MobDuplicatorLootMixin {

    @Redirect(
            method = "work",
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
