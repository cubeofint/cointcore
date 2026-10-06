package com.mawlee.cointcore.spawner;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Farm spawner = player-placed, Apothic-modified, or Apothic with any custom stats.
 * Untouched dungeon spawners are not farms and must spawn mobs normally.
 */
public final class PlayerSpawnerService {
    private static final Map<Class<?>, MethodHandle> APOTHIC_MODIFIED_GETTERS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, MethodHandle> APOTHIC_STATS_GETTERS = new ConcurrentHashMap<>();
    private static final MethodHandle NO_HANDLE = MethodHandles.constant(boolean.class, false);

    private PlayerSpawnerService() {
    }

    public static boolean isFarmSpawner(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).is(Blocks.SPAWNER)) {
            return false;
        }

        if (PlayerSpawnerSavedData.get(level.getServer()).isMarked(level.dimension(), pos)) {
            return true;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity != null && isApothicFarm(blockEntity, level);
    }

    public static boolean isFarmSpawner(ServerLevel level, BlockEntity blockEntity) {
        if (!(blockEntity instanceof SpawnerBlockEntity)) {
            return false;
        }

        BlockPos pos = blockEntity.getBlockPos();
        if (PlayerSpawnerSavedData.get(level.getServer()).isMarked(level.dimension(), pos)) {
            return true;
        }

        return isApothicFarm(blockEntity, level);
    }

    /**
     * Lazily indexes an in-place Apothic-modified dungeon spawner so place-data stays in sync.
     */
    public static void ensureIndexedIfFarm(ServerLevel level, BlockEntity blockEntity) {
        if (!(blockEntity instanceof SpawnerBlockEntity)) {
            return;
        }

        BlockPos pos = blockEntity.getBlockPos();
        PlayerSpawnerSavedData data = PlayerSpawnerSavedData.get(level.getServer());
        if (data.isMarked(level.dimension(), pos)) {
            return;
        }

        if (isApothicFarm(blockEntity, level)) {
            data.mark(level, pos);
        }
    }

    private static boolean isApothicFarm(BlockEntity blockEntity, ServerLevel level) {
        if (isApothicModifiedFlag(blockEntity, level)) {
            return true;
        }
        return hasApothicCustomStats(blockEntity, level);
    }

    private static boolean isApothicModifiedFlag(BlockEntity blockEntity, ServerLevel level) {
        MethodHandle handle = APOTHIC_MODIFIED_GETTERS.computeIfAbsent(
                blockEntity.getClass(),
                PlayerSpawnerService::lookupApothicModifiedGetter
        );
        if (handle != NO_HANDLE) {
            try {
                return (boolean) handle.invoke(blockEntity);
            } catch (Throwable ignored) {
                return readModifiedNbt(blockEntity, level);
            }
        }

        return readModifiedNbt(blockEntity, level);
    }

    private static boolean hasApothicCustomStats(BlockEntity blockEntity, ServerLevel level) {
        MethodHandle handle = APOTHIC_STATS_GETTERS.computeIfAbsent(
                blockEntity.getClass(),
                PlayerSpawnerService::lookupApothicStatsGetter
        );
        if (handle != NO_HANDLE) {
            try {
                Object stats = handle.invoke(blockEntity);
                return stats instanceof Map<?, ?> map && !map.isEmpty();
            } catch (Throwable ignored) {
                return readStatsNbt(blockEntity, level);
            }
        }
        return readStatsNbt(blockEntity, level);
    }

    private static MethodHandle lookupApothicModifiedGetter(Class<?> type) {
        try {
            return MethodHandles.publicLookup()
                    .findVirtual(type, "hasBeenModified", MethodType.methodType(boolean.class));
        } catch (NoSuchMethodException | IllegalAccessException ignored) {
            return NO_HANDLE;
        }
    }

    private static MethodHandle lookupApothicStatsGetter(Class<?> type) {
        try {
            return MethodHandles.publicLookup()
                    .findVirtual(type, "getStatsMap", MethodType.methodType(Map.class));
        } catch (NoSuchMethodException | IllegalAccessException ignored) {
            return NO_HANDLE;
        }
    }

    private static boolean readModifiedNbt(BlockEntity blockEntity, ServerLevel level) {
        CompoundTag tag = blockEntity.saveWithoutMetadata(level.registryAccess());
        return tag.getBoolean("modified");
    }

    private static boolean readStatsNbt(BlockEntity blockEntity, ServerLevel level) {
        CompoundTag tag = blockEntity.saveWithoutMetadata(level.registryAccess());
        return tag.contains("stats") && !tag.getCompound("stats").isEmpty();
    }
}
