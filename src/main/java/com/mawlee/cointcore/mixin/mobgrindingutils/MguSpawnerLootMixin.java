package com.mawlee.cointcore.mixin.mobgrindingutils;

import com.mawlee.cointcore.spawner.MachineSpawnLoot;
import com.mawlee.cointcore.spawner.VirtualSpawnContext;
import mob_grinding_utils.tile.TileEntityMGUSpawner;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TileEntityMGUSpawner.class, remap = false)
public abstract class MguSpawnerLootMixin {

    @Inject(method = "spawnMobInArea", at = @At("HEAD"), require = 0)
    private void cointcore$enterVirtual(CallbackInfoReturnable<Boolean> ci) {
        VirtualSpawnContext.enter();
    }

    @Inject(method = "spawnMobInArea", at = @At("RETURN"), require = 0)
    private void cointcore$exitVirtual(CallbackInfoReturnable<Boolean> ci) {
        VirtualSpawnContext.exit();
    }

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
