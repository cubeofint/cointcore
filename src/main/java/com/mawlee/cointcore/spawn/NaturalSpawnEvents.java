package com.mawlee.cointcore.spawn;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.config.NaturalSpawnConfig;
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
