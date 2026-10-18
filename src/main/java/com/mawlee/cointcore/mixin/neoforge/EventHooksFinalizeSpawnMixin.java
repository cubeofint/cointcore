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
 * Virtual machine spawners skip {@code EventHooks.finalizeMobSpawn} (Apotheosis / Aether).
 * NeoForge 21.1 ships a 5-arg {@code finalizeMobSpawn} (no boolean) plus
 * {@code finalizeMobSpawnSpawner}; a 6-arg boolean overload does not exist and
 * must not be targeted. {@code require = 0} so a missing target never crashes startup.
 */
@Mixin(value = EventHooks.class, remap = false)
public abstract class EventHooksFinalizeSpawnMixin {

    /**
     * NeoForge 21.1.x: {@code finalizeMobSpawn(Mob, ServerLevelAccessor, DifficultyInstance, MobSpawnType, SpawnGroupData)}.
     */
    @Inject(
            method = "finalizeMobSpawn(Lnet/minecraft/world/entity/Mob;Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/DifficultyInstance;Lnet/minecraft/world/entity/MobSpawnType;Lnet/minecraft/world/entity/SpawnGroupData;)Lnet/minecraft/world/entity/SpawnGroupData;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private static void cointcore$skipFinalizeForVirtualLoot(
            Mob mob,
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            MobSpawnType spawnType,
            SpawnGroupData spawnData,
            CallbackInfoReturnable<SpawnGroupData> cir
    ) {
        if (VirtualSpawnContext.skipFinalize()) {
            cir.setReturnValue(spawnData);
        }
    }
}
