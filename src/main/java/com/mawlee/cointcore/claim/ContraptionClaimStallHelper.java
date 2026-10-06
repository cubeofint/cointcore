package com.mawlee.cointcore.claim;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches Create contraption claim-stall decisions for a few ticks to avoid nearest-player
 * + FTB claim lookups every entity tick.
 */
public final class ContraptionClaimStallHelper {
    private static final int CACHE_TICKS = 10;

    private static final ConcurrentHashMap<Integer, CacheEntry> CACHE = new ConcurrentHashMap<>();

    private ContraptionClaimStallHelper() {
    }

    public static void applyStall(AbstractContraptionEntity self, Optional<UUID> controllingPlayer) {
        if (!ClaimGuard.isAvailable()) {
            return;
        }
        if (!(self.level() instanceof ServerLevel level)) {
            return;
        }

        Contraption contraption = self.getContraption();
        if (contraption == null) {
            return;
        }

        long gameTime = level.getGameTime();
        BlockPos pos = self.blockPosition();
        long chunkKey = ChunkPos.asLong(pos);
        int entityId = self.getId();

        CacheEntry cached = CACHE.get(entityId);
        if (cached != null && cached.expireTick > gameTime && cached.chunkKey == chunkKey) {
            if (cached.shouldStall) {
                contraption.stalled = true;
            }
            return;
        }

        Entity actor = ClaimGuard.resolveActor(level, pos, controllingPlayer);
        boolean shouldStall = actor != null && !ClaimGuard.canEdit(actor, level, pos);
        CACHE.put(entityId, new CacheEntry(gameTime + CACHE_TICKS, chunkKey, shouldStall));
        if (shouldStall) {
            contraption.stalled = true;
        }
    }

    public static void reset() {
        CACHE.clear();
    }

    private record CacheEntry(long expireTick, long chunkKey, boolean shouldStall) {
    }
}
