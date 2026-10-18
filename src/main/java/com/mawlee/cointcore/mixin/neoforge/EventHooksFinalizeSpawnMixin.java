package com.mawlee.cointcore.mixin.neoforge;

import com.mawlee.cointcore.spawner.VirtualSpawnContext;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Virtual machine spawners skip {@code finalizeMobSpawn} (Apotheosis / Aether).
 * Farm loot still rolls the death table plus a cheap affix chance.
 */
@Mixin(EventHooks.class)
public abstract class EventHooksFinalizeSpawnMixin {

    @Inject(
            method = "finalizeMobSpawn(Lnet/minecraft/world/entity/Mob;Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/DifficultyInstance;Lnet/minecraft/world/entity/MobSpawnType;Lnet/minecraft/world/entity/SpawnGroupData;Z)Lnet/minecraft/world/entity/SpawnGroupData;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void cointcore$skipFinalizeForVirtualLoot(
            Mob mob,
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            MobSpawnType spawnType,
            SpawnGroupData spawnData,
            boolean fireEvent,
            CallbackInfoReturnable<SpawnGroupData> cir
    ) {
        if (VirtualSpawnContext.skipFinalize()) {
            cir.setReturnValue(spawnData);
        }
    }
}
