package com.mawlee.cointcore.mixin.naturesaura;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mawlee.cointcore.config.NaturesAuraPerfConfig;
import com.mojang.logging.LogUtils;
import de.ellpeck.naturesaura.api.NaturesAuraAPI;
import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import de.ellpeck.naturesaura.api.misc.ILevelData;
import de.ellpeck.naturesaura.chunk.AuraChunk;
import de.ellpeck.naturesaura.events.CommonEvents;
import de.ellpeck.naturesaura.misc.LevelData;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Nature's Aura walks every ticking chunk via reflection each second.
 * Only chunks registered in {@link LevelData#auraChunksWithSpots} can run drain effects,
 * so tick those instead — same gameplay, far less work on large worlds.
 * <p>
 * Map entries can outlive unload/reload; always resolve the live attachment and skip
 * detached {@link LevelChunk}s (null {@code getLevel()}) that otherwise NPE in {@link AuraChunk#update()}.
 */
@Mixin(value = CommonEvents.class, remap = false)
public abstract class CommonEventsLevelTickMixin {
    @Unique
    private static final Logger COINTCORE$LOGGER = LogUtils.getLogger();

    @Unique
    private static long cointcore$lastAuraTickErrorLogGameTime = Long.MIN_VALUE;

    @Inject(method = "onLevelTick", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$tickAuraChunksWithSpotsOnly(LevelTickEvent.Post event, CallbackInfo ci) {
        if (!NaturesAuraPerfConfig.isOptimizeAuraChunkTick()) {
            return;
        }

        ci.cancel();

        Level level = event.getLevel();
        if (level.isClientSide || level.getGameTime() % 20L != 0L) {
            return;
        }

        level.getProfiler().push("naturesaura:onLevelTick");
        try {
            ILevelData raw = ILevelData.getLevelData(level);
            if (!(raw instanceof LevelData data)) {
                return;
            }

            Long2ObjectMap<AuraChunk> tracked = data.auraChunksWithSpots;
            if (tracked == null) {
                return;
            }

            // Snapshot keys under the same lock C2ME workers use in addOrRemoveAsActive.
            long[] keys;
            synchronized (tracked) {
                if (tracked.isEmpty()) {
                    return;
                }
                keys = tracked.keySet().toLongArray();
            }
            for (long key : keys) {
                int chunkX = ChunkPos.getX(key);
                int chunkZ = ChunkPos.getZ(key);
                LevelChunk loaded = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (loaded == null) {
                    synchronized (tracked) {
                        cointcore$pruneStaleTrackedAuraChunk(tracked, key);
                    }
                    continue;
                }

                IAuraChunk attachment = loaded.getData(NaturesAuraAPI.AURA_CHUNK_ATTACHMENT);
                if (!(attachment instanceof AuraChunk liveAura)) {
                    synchronized (tracked) {
                        tracked.remove(key);
                    }
                    continue;
                }

                LevelChunk liveChunk = ((AuraChunkAccessor) (Object) liveAura).cointcore$getChunk();
                if (liveChunk == null || liveChunk.getLevel() != level) {
                    synchronized (tracked) {
                        tracked.remove(key);
                    }
                    continue;
                }

                try {
                    liveAura.update();
                } catch (RuntimeException exception) {
                    cointcore$logAuraTickFailure(level, chunkX, chunkZ, exception);
                }
            }
        } catch (RuntimeException exception) {
            cointcore$logAuraTickFailure(level, Integer.MIN_VALUE, Integer.MIN_VALUE, exception);
        } finally {
            level.getProfiler().pop();
        }
    }

    @WrapOperation(
            method = "onChunkUnload",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;remove(J)Ljava/lang/Object;"
            ),
            remap = false
    )
    private Object cointcore$syncAuraSpotMapUnload(
            Long2ObjectMap<AuraChunk> map,
            long key,
            Operation<Object> original
    ) {
        synchronized (map) {
            return original.call(map, key);
        }
    }

    @Unique
    private static void cointcore$pruneStaleTrackedAuraChunk(Long2ObjectMap<AuraChunk> tracked, long key) {
        AuraChunk trackedAura = tracked.get(key);
        if (trackedAura == null) {
            return;
        }
        LevelChunk chunk = ((AuraChunkAccessor) (Object) trackedAura).cointcore$getChunk();
        if (chunk == null || chunk.getLevel() == null) {
            tracked.remove(key);
        }
    }

    @Unique
    private static void cointcore$logAuraTickFailure(Level level, int chunkX, int chunkZ, RuntimeException exception) {
        long now = level.getGameTime();
        // Avoid flooding the console when the same failure repeats every second.
        if (now - cointcore$lastAuraTickErrorLogGameTime < 200L) {
            return;
        }
        cointcore$lastAuraTickErrorLogGameTime = now;
        if (chunkX == Integer.MIN_VALUE) {
            COINTCORE$LOGGER.error("CointCore NaturesAura aura-chunk tick failed", exception);
        } else {
            COINTCORE$LOGGER.error(
                    "CointCore NaturesAura aura-chunk tick failed at [{}, {}]",
                    chunkX,
                    chunkZ,
                    exception
            );
        }
    }
}
