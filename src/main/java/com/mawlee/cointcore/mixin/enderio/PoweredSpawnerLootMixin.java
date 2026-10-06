package com.mawlee.cointcore.mixin.enderio;

import com.mawlee.cointcore.spawner.MachineSpawnLoot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.enderio.enderio.content.machines.powered_spawner.MobSpawnTask", remap = false)
public abstract class PoweredSpawnerLootMixin {

    @Inject(method = "trySpawnEntity", at = @At("HEAD"))
    private void cointcore$pushOrigin(BlockPos pos, ServerLevel level, CallbackInfo ci) {
        MachineSpawnLoot.pushOrigin(pos);
    }

    @Inject(method = "trySpawnEntity", at = @At("RETURN"))
    private void cointcore$popOrigin(BlockPos pos, ServerLevel level, CallbackInfo ci) {
        MachineSpawnLoot.popOrigin();
    }

    @Redirect(
            method = "trySpawnEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;tryAddFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)Z"
            ),
            remap = true
    )
    private boolean cointcore$lootInsteadOfMob(ServerLevel level, Entity entity) {
        return MachineSpawnLoot.capturePassengers(level, entity);
    }
}
