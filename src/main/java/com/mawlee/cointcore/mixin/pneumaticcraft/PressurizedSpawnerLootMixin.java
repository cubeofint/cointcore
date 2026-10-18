package com.mawlee.cointcore.mixin.pneumaticcraft;

import com.mawlee.cointcore.spawner.MachineSpawnLoot;
import com.mawlee.cointcore.spawner.VirtualSpawnContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
        targets = "me.desht.pneumaticcraft.common.block.entity.spawning.PressurizedSpawnerBlockEntity",
        remap = false
)
public abstract class PressurizedSpawnerLootMixin {

    @Inject(method = "trySpawnSomething", at = @At("HEAD"))
    private void cointcore$enterVirtual(CallbackInfo ci) {
        VirtualSpawnContext.enter();
    }

    @Inject(method = "trySpawnSomething", at = @At("RETURN"))
    private void cointcore$exitVirtual(CallbackInfo ci) {
        VirtualSpawnContext.exit();
    }

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
