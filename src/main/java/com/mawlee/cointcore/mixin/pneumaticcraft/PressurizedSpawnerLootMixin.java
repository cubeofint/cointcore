package com.mawlee.cointcore.mixin.pneumaticcraft;

import com.mawlee.cointcore.spawner.MachineSpawnLoot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
        targets = "me.desht.pneumaticcraft.common.block.entity.spawning.PressurizedSpawnerBlockEntity",
        remap = false
)
public abstract class PressurizedSpawnerLootMixin {

    @Redirect(
            method = "trySpawnSomething",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;tryAddFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)Z"
            ),
            remap = true
    )
    private boolean cointcore$lootInsteadOfMob(ServerLevel level, Entity entity) {
        return MachineSpawnLoot.capturePassengers(level, entity, ((BlockEntity) (Object) this).getBlockPos());
    }
}
