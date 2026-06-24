package com.mawlee.cointcore.spawner;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ApothicSpawnerIntegration {
    private static final ResourceLocation IGNORE_PLAYERS_STAT = ResourceLocation.fromNamespaceAndPath(
            "apothic_spawners",
            "ignore_players"
    );
    private static final String APOTHIC_SPAWNER_TILE = "dev.shadowsoffire.apothic_spawners.block.ApothSpawnerTile";

    private ApothicSpawnerIntegration() {
    }

    public static boolean ignoresPlayers(ServerLevel level, BlockPos spawnerPos) {
        BlockEntity blockEntity = level.getBlockEntity(spawnerPos);
        if (blockEntity == null) {
            return false;
        }

        Boolean reflected = readIgnorePlayersFromApothicApi(blockEntity);
        if (reflected != null) {
            return reflected;
        }

        return readIgnorePlayersFromNbt(level, blockEntity);
    }

    private static Boolean readIgnorePlayersFromApothicApi(BlockEntity blockEntity) {
        if (!APOTHIC_SPAWNER_TILE.equals(blockEntity.getClass().getName())) {
            return null;
        }

        try {
            Class<?> statsClass = Class.forName("dev.shadowsoffire.apothic_spawners.stats.SpawnerStats");
            Object ignorePlayersStat = statsClass.getField("IGNORE_PLAYERS").get(null);
            var getValue = ignorePlayersStat.getClass().getMethod("getValue", blockEntity.getClass());
            Object value = getValue.invoke(ignorePlayersStat, blockEntity);
            if (value instanceof Boolean bool) {
                return bool;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return null;
    }

    private static boolean readIgnorePlayersFromNbt(ServerLevel level, BlockEntity blockEntity) {
        CompoundTag tag = blockEntity.saveWithoutMetadata(level.registryAccess());
        CompoundTag stats = tag.getCompound("stats");
        String key = IGNORE_PLAYERS_STAT.toString();
        if (!stats.contains(key)) {
            return false;
        }

        return stats.getBoolean(key);
    }
}
