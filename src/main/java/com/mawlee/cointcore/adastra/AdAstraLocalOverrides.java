package com.mawlee.cointcore.adastra;

import earth.terrarium.adastra.api.systems.GravityApi;
import earth.terrarium.adastra.api.systems.OxygenApi;
import earth.terrarium.adastra.api.systems.TemperatureApi;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Chunks where Ad Astra stored a value that differs from the dimension default.
 * Entity ticks consult this instead of rereading planet data for every mob.
 */
public final class AdAstraLocalOverrides {
    /** Matches {@code TemperatureApiImpl}: cold is below this, hot is above {@link #HOT_ABOVE}. */
    public static final short COLD_BELOW = -50;
    public static final short HOT_ABOVE = 70;

    private static final Map<ResourceKey<Level>, Float> DIMENSION_GRAVITY = new HashMap<>();
    private static final Map<ResourceKey<Level>, Boolean> DIMENSION_OXYGEN = new HashMap<>();
    private static final Map<ResourceKey<Level>, Short> DIMENSION_TEMPERATURE = new HashMap<>();
    private static final Map<ServerLevel, LongOpenHashSet> CUSTOM_GRAVITY = new HashMap<>();
    private static final Map<ServerLevel, LongOpenHashSet> PROBED_GRAVITY = new HashMap<>();
    private static final Map<ServerLevel, LongOpenHashSet> OXYGEN_FALSE = new HashMap<>();
    private static final Map<ServerLevel, LongOpenHashSet> EXTREME_TEMPERATURE = new HashMap<>();
    /** Chunks where saved oxygen or temperature differs from the dimension default. */
    private static final Map<ServerLevel, LongOpenHashSet> LOCAL_CLIMATE = new HashMap<>();

    private AdAstraLocalOverrides() {
    }

    public static float dimensionGravity(Level level) {
        return DIMENSION_GRAVITY.computeIfAbsent(level.dimension(), key -> GravityApi.API.getGravity(key));
    }

    public static boolean dimensionHasOxygen(Level level) {
        return DIMENSION_OXYGEN.computeIfAbsent(level.dimension(), key -> OxygenApi.API.hasOxygen(key));
    }

    public static short dimensionTemperature(Level level) {
        return DIMENSION_TEMPERATURE.computeIfAbsent(level.dimension(), key -> TemperatureApi.API.getTemperature(key));
    }

    public static boolean isLivable(short temperature) {
        return temperature >= COLD_BELOW && temperature <= HOT_ABOVE;
    }

    public static boolean isExtreme(short temperature) {
        return temperature < COLD_BELOW || temperature > HOT_ABOVE;
    }

    public static long chunkKey(int blockX, int blockZ) {
        return ChunkPos.asLong(blockX >> 4, blockZ >> 4);
    }

    public static long chunkKey(BlockPos pos) {
        return chunkKey(pos.getX(), pos.getZ());
    }

    public static boolean hasCustomGravity(ServerLevel level, long chunk) {
        LongOpenHashSet chunks = CUSTOM_GRAVITY.get(level);
        return chunks != null && chunks.contains(chunk);
    }

    public static boolean isGravityProbed(ServerLevel level, long chunk) {
        LongOpenHashSet chunks = PROBED_GRAVITY.get(level);
        return chunks != null && chunks.contains(chunk);
    }

    public static void markGravityProbed(ServerLevel level, long chunk) {
        PROBED_GRAVITY.computeIfAbsent(level, ignored -> new LongOpenHashSet()).add(chunk);
    }

    public static void markCustomGravity(ServerLevel level, BlockPos pos) {
        long chunk = chunkKey(pos);
        CUSTOM_GRAVITY.computeIfAbsent(level, ignored -> new LongOpenHashSet()).add(chunk);
        markGravityProbed(level, chunk);
    }

    public static boolean hasOxygenFalse(ServerLevel level, BlockPos pos) {
        LongOpenHashSet chunks = OXYGEN_FALSE.get(level);
        return chunks != null && chunks.contains(chunkKey(pos));
    }

    public static void markOxygenFalse(ServerLevel level, BlockPos pos) {
        OXYGEN_FALSE.computeIfAbsent(level, ignored -> new LongOpenHashSet()).add(chunkKey(pos));
    }

    public static boolean hasExtremeTemperature(ServerLevel level, BlockPos pos) {
        LongOpenHashSet chunks = EXTREME_TEMPERATURE.get(level);
        return chunks != null && chunks.contains(chunkKey(pos));
    }

    public static void markExtremeTemperature(ServerLevel level, BlockPos pos) {
        EXTREME_TEMPERATURE.computeIfAbsent(level, ignored -> new LongOpenHashSet()).add(chunkKey(pos));
    }

    public static void markLocalClimate(ServerLevel level, BlockPos pos) {
        LOCAL_CLIMATE.computeIfAbsent(level, ignored -> new LongOpenHashSet()).add(chunkKey(pos));
    }

    public static boolean hasLocalClimate(ServerLevel level, BlockPos pos) {
        LongOpenHashSet chunks = LOCAL_CLIMATE.get(level);
        return chunks != null && chunks.contains(chunkKey(pos));
    }

    /**
     * True when this block or an orthogonal neighbor in another chunk has a gravity override.
     */
    public static boolean areaHasCustomGravity(ServerLevel level, BlockPos pos) {
        return areaMarked(CUSTOM_GRAVITY.get(level), pos);
    }

    /**
     * True when this block or an orthogonal neighbor in another chunk has local climate data.
     */
    public static boolean areaHasLocalClimate(ServerLevel level, BlockPos pos) {
        return areaMarked(LOCAL_CLIMATE.get(level), pos);
    }

    private static boolean areaMarked(LongOpenHashSet chunks, BlockPos pos) {
        if (chunks == null || chunks.isEmpty()) {
            return false;
        }
        int x = pos.getX();
        int z = pos.getZ();
        if (chunks.contains(chunkKey(x, z))) {
            return true;
        }
        int localX = x & 15;
        int localZ = z & 15;
        if (localX == 0 && chunks.contains(chunkKey(x - 1, z))) {
            return true;
        }
        if (localX == 15 && chunks.contains(chunkKey(x + 1, z))) {
            return true;
        }
        if (localZ == 0 && chunks.contains(chunkKey(x, z - 1))) {
            return true;
        }
        return localZ == 15 && chunks.contains(chunkKey(x, z + 1));
    }
}
