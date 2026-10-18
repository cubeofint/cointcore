package com.mawlee.cointcore.mekmm;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * Fast equivalent of Mekanism MoreMachine
 * {@code TileEntityLargeWindGenerator#checkSameBlockNearby()}.
 *
 * <p>The original walks every block in a 5×5 chunk square over full world height
 * ({@code getBlockState}). This walks {@link LevelChunk#getBlockEntities()} of
 * already-loaded chunks only.
 *
 * <p>Original semantics: compare {@code getBlockState().getBlock()} with this
 * generator's block. Mekanism bounding parts are a different block
 * ({@code BlockBounding}), so only another main large-wind-generator block
 * (and its {@code TileEntityLargeWindGenerator}) counts. Bounding tiles do not.
 */
public final class MekmmWindGeneratorNearbyScan {
    public static final int CHUNK_RADIUS = 2;
    public static final String TILE_CLASS_NAME =
            "com.jerry.meklg.common.tile.generator.TileEntityLargeWindGenerator";

    private static volatile Class<?> cachedTileClass;
    private static volatile boolean tileClassResolved;

    private MekmmWindGeneratorNearbyScan() {
    }

    public record Candidate(
            int x,
            int y,
            int z,
            boolean sameMainBlock,
            boolean windGeneratorTile
    ) {
    }

    /**
     * Pure search used by tests and the Minecraft wrapper.
     * {@code chunkPresent} must not load chunks. {@code entities} is only
     * invoked for chunks that passed {@code chunkPresent}.
     */
    public static boolean hasNeighbor(
            int selfX,
            int selfY,
            int selfZ,
            int radius,
            BiPredicate<Integer, Integer> chunkPresent,
            BiFunction<Integer, Integer, Iterable<Candidate>> entities
    ) {
        int selfChunkX = selfX >> 4;
        int selfChunkZ = selfZ >> 4;
        for (int cx = selfChunkX - radius; cx <= selfChunkX + radius; cx++) {
            for (int cz = selfChunkZ - radius; cz <= selfChunkZ + radius; cz++) {
                if (!chunkPresent.test(cx, cz)) {
                    continue;
                }
                Iterable<Candidate> candidates = entities.apply(cx, cz);
                if (candidates == null) {
                    continue;
                }
                for (Candidate candidate : candidates) {
                    if (candidate.x() == selfX && candidate.y() == selfY && candidate.z() == selfZ) {
                        continue;
                    }
                    if (candidate.sameMainBlock() || candidate.windGeneratorTile()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean hasSameBlockNearby(Level level, BlockPos self, Block selfBlock) {
        if (level == null || self == null) {
            return false;
        }
        ChunkSource chunkSource = level.getChunkSource();
        Class<?> tileClass = resolveTileClass();
        int selfX = self.getX();
        int selfY = self.getY();
        int selfZ = self.getZ();
        int selfChunkX = selfX >> 4;
        int selfChunkZ = selfZ >> 4;
        for (int cx = selfChunkX - CHUNK_RADIUS; cx <= selfChunkX + CHUNK_RADIUS; cx++) {
            for (int cz = selfChunkZ - CHUNK_RADIUS; cz <= selfChunkZ + CHUNK_RADIUS; cz++) {
                if (!chunkSource.hasChunk(cx, cz)) {
                    continue;
                }
                LevelChunk chunk = chunkSource.getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be.isRemoved()) {
                        continue;
                    }
                    BlockPos pos = be.getBlockPos();
                    if (pos.getX() == selfX && pos.getY() == selfY && pos.getZ() == selfZ) {
                        continue;
                    }
                    if (tileClass != null && tileClass.isInstance(be)) {
                        return true;
                    }
                    if (selfBlock != null && be.getBlockState().getBlock() == selfBlock) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    static Class<?> resolveTileClass() {
        if (tileClassResolved) {
            return cachedTileClass;
        }
        synchronized (MekmmWindGeneratorNearbyScan.class) {
            if (!tileClassResolved) {
                try {
                    cachedTileClass = Class.forName(TILE_CLASS_NAME);
                } catch (ClassNotFoundException ignored) {
                    cachedTileClass = null;
                }
                tileClassResolved = true;
            }
        }
        return cachedTileClass;
    }
}
