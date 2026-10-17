package com.mawlee.cointcore.mixin.minecraft;

import com.mawlee.cointcore.spawn.NaturalSpawnEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla runs the full spawn-position search in every chunk within ~8 chunks of a player and only then
 * fires {@code FinalizeSpawnEvent}, where {@link NaturalSpawnEvents} cancels anything outside the configured
 * radius / vertical window. Skipping those chunks and start positions up front removes the wasted search
 * (the cap never fills from them anyway).
 */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerPerfMixin {
    @Inject(method = "spawnForChunk", at = @At("HEAD"), cancellable = true)
    private static void cointcore$skipFilteredChunk(
            ServerLevel level,
            LevelChunk chunk,
            NaturalSpawner.SpawnState spawnState,
            boolean spawnFriendlies,
            boolean spawnMonsters,
            boolean spawnPersistent,
            CallbackInfo ci
    ) {
        if (NaturalSpawnEvents.shouldSkipChunk(level, chunk.getPos())) {
            ci.cancel();
        }
    }

    @Inject(
            method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void cointcore$skipFilteredHeight(
            MobCategory category,
            ServerLevel level,
            ChunkAccess chunk,
            BlockPos pos,
            NaturalSpawner.SpawnPredicate filter,
            NaturalSpawner.AfterSpawnCallback callback,
            CallbackInfo ci
    ) {
        if (NaturalSpawnEvents.shouldSkipPosition(level, pos)) {
            ci.cancel();
        }
    }
}
