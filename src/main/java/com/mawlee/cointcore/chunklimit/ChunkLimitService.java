package com.mawlee.cointcore.chunklimit;

import com.mawlee.cointcore.config.ChunkLimitConfig;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ChunkLimitService {
    private ChunkLimitService() {
    }

    public static boolean canBypass(ServerPlayer player) {
        return PermissionService.has(player, CointPermissionNodes.CHUNK_LIMIT_BYPASS);
    }

    public static boolean wouldExceedBlockLimit(ServerLevel level, ChunkPos chunkPos, Block block) {
        if (!ChunkLimitConfig.isEnabled() || !ChunkLimitConfig.hasBlockLimit(block)) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null) {
            return false;
        }
        Integer limit = ChunkLimitConfig.getBlockLimit(id);
        if (limit == null) {
            return false;
        }
        return countBlocksInChunk(level, chunkPos, block) >= limit;
    }

    public static boolean wouldExceedEntityLimit(ServerLevel level, ChunkPos chunkPos, EntityType<?> type) {
        if (!ChunkLimitConfig.isEnabled() || !ChunkLimitConfig.hasEntityLimit(type)) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id == null) {
            return false;
        }
        Integer limit = ChunkLimitConfig.getEntityLimit(id);
        if (limit == null) {
            return false;
        }
        return countEntitiesInChunk(level, chunkPos, type) >= limit;
    }

    public static int countBlocksInChunk(ServerLevel level, ChunkPos chunkPos, Block block) {
        if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
            return 0;
        }

        LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
        int count = 0;
        for (LevelChunkSection section : chunk.getSections()) {
            if (section == null || section.hasOnlyAir()) {
                continue;
            }
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        if (section.getBlockState(x, y, z).is(block)) {
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    public static int countEntitiesInChunk(ServerLevel level, ChunkPos chunkPos, EntityType<?> type) {
        AABB bounds = chunkBounds(level, chunkPos);
        List<Entity> entities = level.getEntities((Entity) null, bounds, entity -> entity.getType() == type && !entity.isRemoved());
        return entities.size();
    }

    public static void denyBlockPlacement(ServerPlayer player, BlockState placedState, ChunkPos chunkPos) {
        Block block = placedState.getBlock();
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        Integer limit = id != null ? ChunkLimitConfig.getBlockLimit(id) : null;
        int current = countBlocksInChunk(player.serverLevel(), chunkPos, block);

        ItemStack returnStack = block.getCloneItemStack(player.serverLevel(), player.blockPosition(), placedState);
        returnStackToPlayer(player, returnStack);

        sendLimitMessage(
                player,
                CointCoreMessages.CHUNK_LIMIT_BLOCK_DENIED,
                formatId(id),
                current,
                limit != null ? limit : 0,
                chunkPos.x,
                chunkPos.z
        );
    }

    public static void denyItemToss(ServerPlayer player, EntityType<?> type, ChunkPos chunkPos) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        Integer limit = id != null ? ChunkLimitConfig.getEntityLimit(id) : null;
        int current = countEntitiesInChunk(player.serverLevel(), chunkPos, type);
        sendLimitMessage(
                player,
                CointCoreMessages.CHUNK_LIMIT_ENTITY_DENIED,
                formatId(id),
                current,
                limit != null ? limit : 0,
                chunkPos.x,
                chunkPos.z
        );
    }

    public static void denyEntitySpawn(ServerPlayer player, Entity entity, ChunkPos chunkPos) {
        EntityType<?> type = entity.getType();
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        Integer limit = id != null ? ChunkLimitConfig.getEntityLimit(id) : null;
        int current = countEntitiesInChunk((ServerLevel) entity.level(), chunkPos, type);

        returnEntityToPlayer(player, entity);
        sendLimitMessage(
                player,
                CointCoreMessages.CHUNK_LIMIT_ENTITY_DENIED,
                formatId(id),
                current,
                limit != null ? limit : 0,
                chunkPos.x,
                chunkPos.z
        );
    }

    public static ServerPlayer findResponsiblePlayer(Entity entity) {
        if (entity instanceof ItemEntity itemEntity) {
            return findPlayerByUuid(entity, itemEntity.getTarget());
        }

        if (entity instanceof Mob mob) {
            if (mob instanceof OwnableEntity ownable) {
                ServerPlayer owner = findPlayerByUuid(entity, ownable.getOwnerUUID());
                if (owner != null) {
                    return owner;
                }
            }
            if (mob.getSpawnType() == MobSpawnType.SPAWN_EGG || mob.getSpawnType() == MobSpawnType.BUCKET) {
                return findNearestPlayer(entity, 8.0D);
            }
        }

        return null;
    }

    public static boolean isPlayerInitiatedEntity(Entity entity) {
        if (entity instanceof ItemEntity) {
            return true;
        }
        if (entity instanceof Mob mob) {
            MobSpawnType spawnType = mob.getSpawnType();
            return spawnType == MobSpawnType.SPAWN_EGG
                    || spawnType == MobSpawnType.BUCKET
                    || spawnType == MobSpawnType.MOB_SUMMONED;
        }
        return false;
    }

    public static List<ChunkLimitStatusEntry> collectChunkStatus(ServerLevel level, ChunkPos chunkPos) {
        List<ChunkLimitStatusEntry> entries = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Integer> entry : ChunkLimitConfig.getBlockLimits().entrySet()) {
            Block block = BuiltInRegistries.BLOCK.get(entry.getKey());
            if (block == null) {
                continue;
            }
            entries.add(new ChunkLimitStatusEntry(
                    entry.getKey(),
                    true,
                    countBlocksInChunk(level, chunkPos, block),
                    entry.getValue()
            ));
        }
        for (Map.Entry<ResourceLocation, Integer> entry : ChunkLimitConfig.getEntityLimits().entrySet()) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entry.getKey());
            if (type == null) {
                continue;
            }
            entries.add(new ChunkLimitStatusEntry(
                    entry.getKey(),
                    false,
                    countEntitiesInChunk(level, chunkPos, type),
                    entry.getValue()
            ));
        }
        return entries;
    }

    public static void returnStackToPlayer(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack copy = stack.copy();
        if (!player.getInventory().add(copy)) {
            player.drop(copy, false);
        }
    }

    private static void returnEntityToPlayer(ServerPlayer player, Entity entity) {
        if (entity instanceof ItemEntity itemEntity) {
            returnStackToPlayer(player, itemEntity.getItem());
            return;
        }

        if (entity instanceof Mob mob) {
            ItemStack spawnEgg = createSpawnEggStack(mob.getType());
            if (!spawnEgg.isEmpty()) {
                returnStackToPlayer(player, spawnEgg);
                return;
            }
            ItemStack bucket = createBucketStack(mob);
            if (!bucket.isEmpty()) {
                returnStackToPlayer(player, bucket);
            }
        }
    }

    private static ItemStack createSpawnEggStack(EntityType<?> type) {
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof SpawnEggItem spawnEggItem)) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            if (type.equals(spawnEggItem.getType(stack))) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack createBucketStack(Mob mob) {
        if (mob.getSpawnType() != MobSpawnType.BUCKET) {
            return ItemStack.EMPTY;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (id == null) {
            return ItemStack.EMPTY;
        }
        return switch (id.getPath()) {
            case "axolotl" -> new ItemStack(Items.AXOLOTL_BUCKET);
            case "cod" -> new ItemStack(Items.COD_BUCKET);
            case "salmon" -> new ItemStack(Items.SALMON_BUCKET);
            case "pufferfish" -> new ItemStack(Items.PUFFERFISH_BUCKET);
            case "tropical_fish" -> new ItemStack(Items.TROPICAL_FISH_BUCKET);
            case "tadpole" -> new ItemStack(Items.TADPOLE_BUCKET);
            default -> ItemStack.EMPTY;
        };
    }

    private static ServerPlayer findPlayerByUuid(Entity entity, UUID uuid) {
        if (uuid == null || !(entity.level() instanceof ServerLevel level)) {
            return null;
        }
        if (level.getPlayerByUUID(uuid) instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    private static ServerPlayer findNearestPlayer(Entity entity, double radius) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return null;
        }
        Vec3 pos = entity.position();
        ServerPlayer nearest = null;
        double nearestDistance = radius * radius;
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() != level) {
                continue;
            }
            double distance = player.distanceToSqr(pos);
            if (distance <= nearestDistance) {
                nearestDistance = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private static AABB chunkBounds(Level level, ChunkPos chunkPos) {
        return new AABB(
                chunkPos.getMinBlockX(),
                level.getMinBuildHeight(),
                chunkPos.getMinBlockZ(),
                chunkPos.getMaxBlockX() + 1.0D,
                level.getMaxBuildHeight(),
                chunkPos.getMaxBlockZ() + 1.0D
        );
    }

    private static void sendLimitMessage(
            ServerPlayer player,
            String key,
            String targetId,
            int current,
            int limit,
            int chunkX,
            int chunkZ
    ) {
        player.sendSystemMessage(CointCoreMessages.forPlayer(
                player,
                key,
                targetId,
                current,
                limit,
                chunkX,
                chunkZ
        ));
    }

    private static String formatId(ResourceLocation id) {
        return id != null ? id.toString() : "unknown";
    }

    public record ChunkLimitStatusEntry(
            ResourceLocation id,
            boolean block,
            int current,
            int limit
    ) {
    }
}
