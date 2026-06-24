package com.mawlee.cointcore.spawner;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PlayerSpawnerService {
    private static final double MAX_SPAWN_DISTANCE = VanillaSpawnerLimits.SPAWN_RANGE + 1.5D;
    private static final double MAX_SPAWN_DISTANCE_SQ = MAX_SPAWN_DISTANCE * MAX_SPAWN_DISTANCE;

    private PlayerSpawnerService() {
    }

    public static SpawnerResolution resolve(ServerLevel level, Mob mob) {
        List<SpawnerCandidate> candidates = collectCandidates(level, mob);
        if (candidates.isEmpty()) {
            return null;
        }

        candidates.sort(Comparator.comparingDouble(candidate -> candidate.distanceSq));
        BlockPos interceptSpawner = candidates.getFirst().pos;
        BlockPos lootSpawner = selectLootSpawner(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()), candidates);
        return new SpawnerResolution(interceptSpawner, lootSpawner);
    }

    public static boolean isPlayerPlaced(ServerLevel level, BlockPos pos) {
        if (PlayerSpawnerSavedData.get(level.getServer()).isMarked(level.dimension(), pos)) {
            return true;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return false;
        }

        return isModifiedSpawner(blockEntity, blockEntity.saveWithoutMetadata(level.registryAccess()));
    }

    private static List<SpawnerCandidate> collectCandidates(ServerLevel level, Mob mob) {
        List<SpawnerCandidate> candidates = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        HolderLookup.Provider registryAccess = level.registryAccess();
        PlayerSpawnerSavedData savedData = PlayerSpawnerSavedData.get(level.getServer());

        savedData.forEachNear(level, mob.blockPosition(), MAX_SPAWN_DISTANCE, pos -> {
            if (!seen.add(pos)) {
                return;
            }

            addCandidate(level, mob, pos, true, registryAccess, candidates);
        });

        collectModifiedSpawnerCandidates(level, mob, seen, registryAccess, savedData, candidates);
        return candidates;
    }

    private static void collectModifiedSpawnerCandidates(
            ServerLevel level,
            Mob mob,
            Set<BlockPos> seen,
            HolderLookup.Provider registryAccess,
            PlayerSpawnerSavedData savedData,
            List<SpawnerCandidate> candidates
    ) {
        int chunkRadius = (int) Math.ceil(MAX_SPAWN_DISTANCE / 16.0D);
        BlockPos mobPos = mob.blockPosition();
        int centerChunkX = mobPos.getX() >> 4;
        int centerChunkZ = mobPos.getZ() >> 4;

        for (int chunkX = centerChunkX - chunkRadius; chunkX <= centerChunkX + chunkRadius; chunkX++) {
            for (int chunkZ = centerChunkZ - chunkRadius; chunkZ <= centerChunkZ + chunkRadius; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }

                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof SpawnerBlockEntity)) {
                        continue;
                    }

                    BlockPos pos = blockEntity.getBlockPos();
                    if (!seen.add(pos) || savedData.isMarked(level.dimension(), pos)) {
                        continue;
                    }

                    addCandidate(level, mob, pos, false, registryAccess, candidates, blockEntity);
                }
            }
        }
    }

    private static void addCandidate(
            ServerLevel level,
            Mob mob,
            BlockPos pos,
            boolean marked,
            HolderLookup.Provider registryAccess,
            List<SpawnerCandidate> candidates
    ) {
        addCandidate(level, mob, pos, marked, registryAccess, candidates, level.getBlockEntity(pos));
    }

    private static void addCandidate(
            ServerLevel level,
            Mob mob,
            BlockPos pos,
            boolean marked,
            HolderLookup.Provider registryAccess,
            List<SpawnerCandidate> candidates,
            BlockEntity blockEntity
    ) {
        if (!level.getBlockState(pos).is(Blocks.SPAWNER)) {
            if (marked) {
                PlayerSpawnerSavedData.get(level.getServer()).unmark(level, pos);
            }
            return;
        }

        double dx = mob.getX() - (pos.getX() + 0.5D);
        double dy = mob.getY() - (pos.getY() + 0.5D);
        double dz = mob.getZ() - (pos.getZ() + 0.5D);
        double distanceSq = dx * dx + dy * dy + dz * dz;
        if (distanceSq > MAX_SPAWN_DISTANCE_SQ) {
            return;
        }

        if (blockEntity == null) {
            return;
        }

        CompoundTag tag = blockEntity.saveWithoutMetadata(registryAccess);
        if (!marked && !isModifiedSpawner(blockEntity, tag)) {
            return;
        }

        candidates.add(new SpawnerCandidate(pos, distanceSq, readConfiguredEntityId(tag)));
    }

    private static BlockPos selectLootSpawner(ResourceLocation mobId, List<SpawnerCandidate> sortedCandidates) {
        List<SpawnerCandidate> matching = new ArrayList<>();
        for (SpawnerCandidate candidate : sortedCandidates) {
            if (candidate.entityId == null || candidate.entityId.equals(mobId)) {
                matching.add(candidate);
            }
        }

        if (matching.isEmpty()) {
            return null;
        }

        if (matching.size() > 1 && matching.get(1).distanceSq <= matching.getFirst().distanceSq + 0.25D) {
            return null;
        }

        return matching.getFirst().pos;
    }

    private static boolean isModifiedSpawner(BlockEntity blockEntity, CompoundTag tag) {
        if (tag.getBoolean("modified")) {
            return true;
        }

        return hasApothicBeenModified(blockEntity);
    }

    private static ResourceLocation readConfiguredEntityId(CompoundTag tag) {
        CompoundTag spawnData = tag.contains("SpawnData") ? tag.getCompound("SpawnData") : tag.getCompound("spawn_data");
        if (spawnData.isEmpty()) {
            return null;
        }

        CompoundTag entityTag = spawnData.contains("entity")
                ? spawnData.getCompound("entity")
                : spawnData.getCompound("entityToSpawn");
        if (entityTag.isEmpty() || !entityTag.contains("id")) {
            return null;
        }

        return ResourceLocation.tryParse(entityTag.getString("id"));
    }

    private static boolean hasApothicBeenModified(BlockEntity blockEntity) {
        try {
            var method = blockEntity.getClass().getMethod("hasBeenModified");
            if (method.getReturnType() == boolean.class) {
                return (Boolean) method.invoke(blockEntity);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return false;
    }

    private record SpawnerCandidate(BlockPos pos, double distanceSq, ResourceLocation entityId) {
    }
}
