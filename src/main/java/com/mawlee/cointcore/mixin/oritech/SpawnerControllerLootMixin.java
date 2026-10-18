package com.mawlee.cointcore.mixin.oritech;

import com.mawlee.cointcore.spawner.MachineSpawnLoot;
import com.mawlee.cointcore.spawner.VirtualSpawnContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rearth.oritech.block.entity.arcane.SpawnerControllerBlockEntity;

@Mixin(value = SpawnerControllerBlockEntity.class, remap = false)
public abstract class SpawnerControllerLootMixin {

    @Inject(method = "spawnMob", at = @At("HEAD"), require = 0)
    private void cointcore$enterVirtual(CallbackInfo ci) {
        VirtualSpawnContext.enter();
    }

    @Inject(method = "spawnMob", at = @At("RETURN"), require = 0)
    private void cointcore$exitVirtual(CallbackInfo ci) {
        VirtualSpawnContext.exit();
    }

    @Redirect(
            method = "spawnMob",
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
