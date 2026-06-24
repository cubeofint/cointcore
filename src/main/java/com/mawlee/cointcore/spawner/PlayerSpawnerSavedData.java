package com.mawlee.cointcore.spawner;

import com.mawlee.cointcore.CointCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class PlayerSpawnerSavedData extends SavedData {
    private static final String DATA_ID = CointCore.MOD_ID + "_player_spawners";
    private static final String KEYS_KEY = "keys";

    private final Set<String> keys = new HashSet<>();
    private final Map<String, Set<Long>> spawnersByChunk = new HashMap<>();

    private PlayerSpawnerSavedData() {
    }

    public static PlayerSpawnerSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(PlayerSpawnerSavedData::new, PlayerSpawnerSavedData::load), DATA_ID);
    }

    public static String key(ResourceKey<Level> dimension, BlockPos pos) {
        return dimension.location() + ":" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    public void mark(ServerLevel level, BlockPos pos) {
        if (keys.add(key(level.dimension(), pos))) {
            indexSpawner(level.dimension(), pos);
            setDirty();
        }
    }

    public void unmark(ServerLevel level, BlockPos pos) {
        if (keys.remove(key(level.dimension(), pos))) {
            deindexSpawner(level.dimension(), pos);
            setDirty();
        }
    }

    public boolean isMarked(ResourceKey<Level> dimension, BlockPos pos) {
        return keys.contains(key(dimension, pos));
    }

    public void forEachNear(ServerLevel level, BlockPos center, double maxDistance, Consumer<BlockPos> consumer) {
        double maxDistanceSq = maxDistance * maxDistance;
        ResourceKey<Level> dimension = level.dimension();
        int chunkRadius = (int) Math.ceil(maxDistance / 16.0D);
        ChunkPos centerChunk = new ChunkPos(center);
        int centerY = center.getY();

        for (int chunkX = centerChunk.x - chunkRadius; chunkX <= centerChunk.x + chunkRadius; chunkX++) {
            for (int chunkZ = centerChunk.z - chunkRadius; chunkZ <= centerChunk.z + chunkRadius; chunkZ++) {
                Set<Long> packedPositions = spawnersByChunk.get(chunkKey(dimension, chunkX, chunkZ));
                if (packedPositions == null) {
                    continue;
                }

                for (long packed : List.copyOf(packedPositions)) {
                    BlockPos pos = BlockPos.of(packed);
                    if (Math.abs(pos.getY() - centerY) > maxDistance) {
                        continue;
                    }

                    double dx = center.getX() + 0.5D - (pos.getX() + 0.5D);
                    double dy = center.getY() + 0.5D - (pos.getY() + 0.5D);
                    double dz = center.getZ() + 0.5D - (pos.getZ() + 0.5D);
                    if (dx * dx + dy * dy + dz * dz <= maxDistanceSq) {
                        consumer.accept(pos);
                    }
                }
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (String key : keys) {
            list.add(net.minecraft.nbt.StringTag.valueOf(key));
        }
        tag.put(KEYS_KEY, list);
        return tag;
    }

    private static PlayerSpawnerSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        PlayerSpawnerSavedData data = new PlayerSpawnerSavedData();
        ListTag list = tag.getList(KEYS_KEY, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            String key = list.getString(i);
            if (data.keys.add(key)) {
                parseKey(key).ifPresent(parsed -> data.indexSpawner(parsed.dimension(), parsed.pos()));
            }
        }
        return data;
    }

    private void indexSpawner(ResourceKey<Level> dimension, BlockPos pos) {
        spawnersByChunk
                .computeIfAbsent(chunkKey(dimension, new ChunkPos(pos)), ignored -> new HashSet<>())
                .add(pos.asLong());
    }

    private void deindexSpawner(ResourceKey<Level> dimension, BlockPos pos) {
        Set<Long> packedPositions = spawnersByChunk.get(chunkKey(dimension, new ChunkPos(pos)));
        if (packedPositions != null) {
            packedPositions.remove(pos.asLong());
            if (packedPositions.isEmpty()) {
                spawnersByChunk.remove(chunkKey(dimension, new ChunkPos(pos)));
            }
        }
    }

    private static String chunkKey(ResourceKey<Level> dimension, ChunkPos chunkPos) {
        return chunkKey(dimension, chunkPos.x, chunkPos.z);
    }

    private static String chunkKey(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        return dimension.location() + "#" + chunkX + "#" + chunkZ;
    }

    private static java.util.Optional<ParsedKey> parseKey(String key) {
        int lastColon = key.lastIndexOf(':');
        if (lastColon <= 0 || lastColon >= key.length() - 1) {
            return java.util.Optional.empty();
        }

        String dimensionId = key.substring(0, lastColon);
        String[] coordinates = key.substring(lastColon + 1).split(",");
        if (coordinates.length != 3) {
            return java.util.Optional.empty();
        }

        try {
            int x = Integer.parseInt(coordinates[0]);
            int y = Integer.parseInt(coordinates[1]);
            int z = Integer.parseInt(coordinates[2]);
            ResourceKey<Level> dimension = ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    net.minecraft.resources.ResourceLocation.parse(dimensionId)
            );
            return java.util.Optional.of(new ParsedKey(dimension, new BlockPos(x, y, z)));
        } catch (RuntimeException ignored) {
            return java.util.Optional.empty();
        }
    }

    private record ParsedKey(ResourceKey<Level> dimension, BlockPos pos) {
    }
}
