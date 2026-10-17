package com.mawlee.cointcore.spawn;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.config.NaturalSpawnConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/**
 * Restricts {@link MobSpawnType#NATURAL} spawns to a Chebyshev chunk radius and
 * vertical window around at least one online player in the same dimension.
 */
@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.DEDICATED_SERVER)
public final class NaturalSpawnEvents {
    private NaturalSpawnEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.isCanceled() || event.isSpawnCancelled() || event.getLevel().isClientSide()) {
            return;
        }
        if (!NaturalSpawnConfig.isEnabled() || event.getSpawnType() != MobSpawnType.NATURAL) {
            return;
        }
        if (!(event.getEntity() instanceof Mob mob) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (isAllowedNearAnyPlayer(level, mob)) {
            return;
        }
        event.setSpawnCancelled(true);
        event.setCanceled(true);
    }

    /**
     * Pre-filter for {@code NaturalSpawner.spawnForChunk}: true when the chunk is outside the horizontal
     * radius of every player (spawns there would be cancelled in {@link #onFinalizeSpawn} anyway), or when
     * {@code spawn_tick_interval} throttling skips this chunk this tick (staggered per chunk).
     */
    public static boolean shouldSkipChunk(ServerLevel level, ChunkPos chunkPos) {
        if (!NaturalSpawnConfig.isEnabled()) {
            return false;
        }
        int interval = NaturalSpawnConfig.getSpawnTickInterval();
        if (interval > 1 && Math.floorMod(level.getGameTime() + chunkPos.x * 31L + chunkPos.z, interval) != 0) {
            return true;
        }
        int maxChunkRadius = NaturalSpawnConfig.getMaxChunkRadius();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            ChunkPos playerChunk = player.chunkPosition();
            if (Math.max(Math.abs(chunkPos.x - playerChunk.x), Math.abs(chunkPos.z - playerChunk.z)) <= maxChunkRadius) {
                return false;
            }
        }
        return true;
    }

    /**
     * Pre-filter for {@code NaturalSpawner.spawnCategoryForPosition}: a natural pack keeps the start Y, so a
     * start position outside the vertical window of every nearby player would be cancelled in
     * {@link #onFinalizeSpawn} after the full pack search. One extra chunk of slack covers pack wander.
     */
    public static boolean shouldSkipPosition(ServerLevel level, BlockPos pos) {
        if (!NaturalSpawnConfig.isEnabled()) {
            return false;
        }
        int chunkRadius = NaturalSpawnConfig.getMaxChunkRadius() + 1;
        double maxVertical = NaturalSpawnConfig.getMaxVerticalBlocks();
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            ChunkPos playerChunk = player.chunkPosition();
            if (Math.max(Math.abs(chunkX - playerChunk.x), Math.abs(chunkZ - playerChunk.z)) > chunkRadius) {
                continue;
            }
            if (Math.abs(pos.getY() - player.getY()) <= maxVertical) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAllowedNearAnyPlayer(ServerLevel level, Mob mob) {
        int maxChunkRadius = NaturalSpawnConfig.getMaxChunkRadius();
        double maxVertical = NaturalSpawnConfig.getMaxVerticalBlocks();
        ChunkPos mobChunk = mob.chunkPosition();
        double mobY = mob.getY();

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            ChunkPos playerChunk = player.chunkPosition();
            int chunkDx = Math.abs(mobChunk.x - playerChunk.x);
            int chunkDz = Math.abs(mobChunk.z - playerChunk.z);
            if (Math.max(chunkDx, chunkDz) > maxChunkRadius) {
                continue;
            }
            if (Math.abs(mobY - player.getY()) > maxVertical) {
                continue;
            }
            return true;
        }
        return false;
    }
}
