package com.mawlee.cointcore.chunklimit;

import com.mawlee.cointcore.config.ChunkLimitConfig;
import com.mawlee.cointcore.config.ChunkLimitConfig.LimitScope;
import com.mawlee.cointcore.ftb.FtbIntegration;
import com.mawlee.cointcore.lang.CointCoreMessages;
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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Chunk + team mob/entity caps: block all spawn sources, cull excess without drops,
 * return spawn items to players when their spawn is denied.
 */
public final class MobLimitService {
    private MobLimitService() {
    }

    public static boolean isMob(Entity entity) {
        return entity instanceof Mob;
    }

    public static boolean shouldEnforce(Entity entity) {
        if (!ChunkLimitConfig.isEnabled() || entity == null || entity instanceof ServerPlayer) {
            return false;
        }
        boolean mob = isMob(entity);
        return ChunkLimitConfig.isEntityLimited(entity.getType(), mob);
    }

    /**
     * True if adding/keeping this entity would exceed chunk or team limits.
     * Assumes the entity is already present in the level for join/cull checks ({@code >}).
     */
    public static boolean wouldExceed(ServerLevel level, ChunkPos chunkPos, Entity entity) {
        return wouldExceedInternal(level, chunkPos, entity, true);
    }

    /**
     * Join-time check that works whether or not {@code entity} is already visible to
     * {@link ServerLevel#getEntities}. Counts others in the chunk, then applies {@code + 1}.
     * Needed for DE Stabilized Spawner: MobSoul with NBT skips {@code finalizeMobSpawn}.
     */
    public static boolean wouldExceedJoining(ServerLevel level, ChunkPos chunkPos, Entity entity) {
        return wouldExceedInternal(level, chunkPos, entity, false);
    }

    private static boolean wouldExceedInternal(
            ServerLevel level,
            ChunkPos chunkPos,
            Entity entity,
            boolean entityAlreadyCounted
    ) {
        if (!shouldEnforce(entity)) {
            return false;
        }
        boolean mob = isMob(entity);
        for (ChunkLimitKey key : ChunkLimitConfig.resolveEntityKeys(LimitScope.CHUNK, entity.getType(), mob)) {
            int count = entityAlreadyCounted
                    ? countForKey(level, chunkPos, key)
                    : countForKeyExcluding(level, chunkPos, key, entity);
            if (entityAlreadyCounted) {
                if (count > key.limit()) {
                    return true;
                }
            } else if (count + 1 > key.limit()) {
                return true;
            }
        }
        UUID teamId = FtbIntegration.getTeamIdAt(level, chunkPos).orElse(null);
        if (teamId == null) {
            return false;
        }
        for (ChunkLimitKey key : ChunkLimitConfig.resolveEntityKeys(LimitScope.TEAM, entity.getType(), mob)) {
            int teamCount = TeamMobLimitIndex.count(teamId, key);
            if (entityAlreadyCounted) {
                if (teamCount > key.limit()) {
                    return true;
                }
            } else if (teamCount + 1 > key.limit()) {
                return true;
            }
        }
        return false;
    }

    /** Predict exceed before the entity is in the world (count + 1). */
    public static boolean wouldExceedIfAdded(ServerLevel level, ChunkPos chunkPos, EntityType<?> type, boolean isMob) {
        if (!ChunkLimitConfig.isEnabled() || !ChunkLimitConfig.isEntityLimited(type, isMob)) {
            return false;
        }
        for (ChunkLimitKey key : ChunkLimitConfig.resolveEntityKeys(LimitScope.CHUNK, type, isMob)) {
            if (countForKey(level, chunkPos, key) + 1 > key.limit()) {
                return true;
            }
        }
        UUID teamId = FtbIntegration.getTeamIdAt(level, chunkPos).orElse(null);
        if (teamId == null) {
            return false;
        }
        for (ChunkLimitKey key : ChunkLimitConfig.resolveEntityKeys(LimitScope.TEAM, type, isMob)) {
            if (TeamMobLimitIndex.count(teamId, key) + 1 > key.limit()) {
                return true;
            }
        }
        return false;
    }

    public static int countForKey(ServerLevel level, ChunkPos chunkPos, ChunkLimitKey key) {
        return countForKeyExcluding(level, chunkPos, key, null);
    }

    public static int countForKeyExcluding(ServerLevel level, ChunkPos chunkPos, ChunkLimitKey key, Entity exclude) {
        AABB bounds = chunkBounds(level, chunkPos);
        String id = key.id();
        if (ChunkLimitConfig.ENTITY_CAP_LIMIT_ID.equals(id)) {
            return level.getEntities((Entity) null, bounds, e ->
                    e instanceof Mob && !e.isRemoved() && e != exclude).size();
        }
        if (id.startsWith("emod:")) {
            String ns = id.substring("emod:".length());
            return level.getEntities((Entity) null, bounds, e -> {
                if (!(e instanceof Mob) || e.isRemoved() || e == exclude) {
                    return false;
                }
                ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
                return typeId != null && ns.equals(typeId.getNamespace());
            }).size();
        }
        if (id.startsWith("entity:")) {
            ResourceLocation typeId = ResourceLocation.tryParse(id.substring("entity:".length()));
            if (typeId == null) {
                return 0;
            }
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(typeId);
            if (type == null) {
                return 0;
            }
            return level.getEntities((Entity) null, bounds, e ->
                    e.getType() == type && !e.isRemoved() && e != exclude).size();
        }
        return 0;
    }

    public static Map<String, Integer> snapshotChunkCounts(ServerLevel level, ChunkPos chunkPos) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (!ChunkLimitConfig.hasTeamEntityLimits() && !ChunkLimitConfig.hasChunkEntityLimits()) {
            return counts;
        }
        // Build counts for every configured team key so TeamMobLimitIndex stays accurate.
        addConfiguredCounts(counts, level, chunkPos, LimitScope.TEAM);
        addConfiguredCounts(counts, level, chunkPos, LimitScope.CHUNK);
        return counts;
    }

    private static void addConfiguredCounts(
            Map<String, Integer> counts,
            ServerLevel level,
            ChunkPos chunkPos,
            LimitScope scope
    ) {
        Integer cap = ChunkLimitConfig.getEntityCap(scope);
        if (cap != null) {
            ChunkLimitKey key = new ChunkLimitKey(ChunkLimitConfig.ENTITY_CAP_LIMIT_ID, cap);
            counts.putIfAbsent(key.id(), countForKey(level, chunkPos, key));
        }
        Map<ResourceLocation, Integer> exact = scope == LimitScope.TEAM
                ? ChunkLimitConfig.getTeamEntityLimits()
                : ChunkLimitConfig.getEntityLimits();
        for (Map.Entry<ResourceLocation, Integer> entry : exact.entrySet()) {
            ChunkLimitKey key = new ChunkLimitKey("entity:" + entry.getKey(), entry.getValue());
            counts.putIfAbsent(key.id(), countForKey(level, chunkPos, key));
        }
        var mods = scope == LimitScope.TEAM
                ? ChunkLimitConfig.getTeamEntityModBindings()
                : ChunkLimitConfig.getEntityModBindings();
        for (var binding : mods) {
            counts.putIfAbsent(binding.key().id(), countForKey(level, chunkPos, binding.key()));
        }
    }

    public static void syncTeamIndex(ServerLevel level, ChunkPos chunkPos) {
        if (!ChunkLimitConfig.hasTeamEntityLimits()) {
            TeamMobLimitIndex.clearChunk(level, chunkPos);
            return;
        }
        TeamMobLimitIndex.syncChunk(level, chunkPos, snapshotChunkCounts(level, chunkPos));
    }

    /**
     * Discard excess mobs in this chunk without drops until within all chunk/team limits.
     */
    public static int cullOverLimit(ServerLevel level, ChunkPos chunkPos) {
        if (!ChunkLimitConfig.isEnabled() || !ChunkLimitConfig.hasAnyEntityLimits()) {
            return 0;
        }
        int removed = 0;
        for (int guard = 0; guard < 512; guard++) {
            List<Mob> mobs = listMobs(level, chunkPos);
            Mob victim = pickCullVictim(level, chunkPos, mobs);
            if (victim == null) {
                break;
            }
            discardQuietly(victim);
            removed++;
            syncTeamIndex(level, chunkPos);
        }
        return removed;
    }

    private static Mob pickCullVictim(ServerLevel level, ChunkPos chunkPos, List<Mob> mobs) {
        if (mobs.isEmpty()) {
            return null;
        }
        // Prefer removing from the most over-limit exact key, then mod, then cap.
        for (Mob mob : mobs) {
            if (wouldExceed(level, chunkPos, mob)) {
                return mob;
            }
        }
        return null;
    }

    public static List<Mob> listMobs(ServerLevel level, ChunkPos chunkPos) {
        AABB bounds = chunkBounds(level, chunkPos);
        List<Mob> mobs = new ArrayList<>();
        for (Entity entity : level.getEntities((Entity) null, bounds, e -> e instanceof Mob && !e.isRemoved())) {
            mobs.add((Mob) entity);
        }
        mobs.sort(Comparator.comparingInt(Entity::getId).reversed());
        return mobs;
    }

    public static void discardQuietly(Entity entity) {
        if (entity == null || entity.isRemoved()) {
            return;
        }
        entity.setRemoved(Entity.RemovalReason.DISCARDED);
        entity.discard();
    }

    public static void denySpawn(ServerPlayer player, Entity entity, ChunkPos chunkPos) {
        denySpawnAttempt(player, entity.getType(), chunkPos, isMob(entity));
        returnEntityToPlayer(player, entity);
    }

    /** Message-only deny (egg/bucket use cancelled before an entity exists). */
    public static void denySpawnAttempt(ServerPlayer player, EntityType<?> type, ChunkPos chunkPos) {
        denySpawnAttempt(player, type, chunkPos, true);
    }

    public static void denySpawnAttempt(ServerPlayer player, EntityType<?> type, ChunkPos chunkPos, boolean mob) {
        ServerLevel level = player.serverLevel();
        ChunkLimitKey key = primaryExceededKey(level, chunkPos, type, mob);
        int current = key != null ? countForKey(level, chunkPos, key) : 0;
        int limit = key != null ? key.limit() : 0;
        String id = key != null ? key.id() : formatId(BuiltInRegistries.ENTITY_TYPE.getKey(type));
        player.sendSystemMessage(CointCoreMessages.forPlayer(
                player,
                CointCoreMessages.CHUNK_LIMIT_ENTITY_DENIED,
                id,
                current,
                limit,
                chunkPos.x,
                chunkPos.z
        ));
    }

    private static ChunkLimitKey primaryExceededKey(ServerLevel level, ChunkPos chunkPos, Entity entity) {
        return primaryExceededKey(level, chunkPos, entity.getType(), isMob(entity));
    }

    private static ChunkLimitKey primaryExceededKey(
            ServerLevel level,
            ChunkPos chunkPos,
            EntityType<?> type,
            boolean mob
    ) {
        for (ChunkLimitKey key : ChunkLimitConfig.resolveEntityKeys(LimitScope.CHUNK, type, mob)) {
            // Pre-join / egg use: compare against count (not yet added). Join path uses wouldExceed (>).
            if (countForKey(level, chunkPos, key) >= key.limit()) {
                return key;
            }
        }
        UUID teamId = FtbIntegration.getTeamIdAt(level, chunkPos).orElse(null);
        if (teamId != null) {
            for (ChunkLimitKey key : ChunkLimitConfig.resolveEntityKeys(LimitScope.TEAM, type, mob)) {
                if (TeamMobLimitIndex.count(teamId, key) >= key.limit()) {
                    return key;
                }
            }
        }
        List<ChunkLimitKey> keys = ChunkLimitConfig.resolveEntityKeys(LimitScope.CHUNK, type, mob);
        if (!keys.isEmpty()) {
            return keys.get(0);
        }
        keys = ChunkLimitConfig.resolveEntityKeys(LimitScope.TEAM, type, mob);
        return keys.isEmpty() ? null : keys.get(0);
    }

    public static void returnEntityToPlayer(ServerPlayer player, Entity entity) {
        if (entity instanceof ItemEntity itemEntity) {
            ChunkLimitService.returnStackToPlayer(player, itemEntity.getItem());
            return;
        }
        if (entity instanceof Mob mob) {
            ItemStack spawnEgg = createSpawnEggStack(mob.getType());
            if (!spawnEgg.isEmpty()) {
                ChunkLimitService.returnStackToPlayer(player, spawnEgg);
                return;
            }
            ItemStack bucket = createBucketStack(mob);
            if (!bucket.isEmpty()) {
                ChunkLimitService.returnStackToPlayer(player, bucket);
            }
        }
    }

    public static ServerPlayer findResponsiblePlayer(Entity entity) {
        return ChunkLimitService.findResponsiblePlayer(entity);
    }

    public static boolean isPlayerInitiatedEntity(Entity entity) {
        return ChunkLimitService.isPlayerInitiatedEntity(entity);
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

    private static String formatId(ResourceLocation id) {
        return id != null ? id.toString() : "unknown";
    }

    public static List<ChunkLimitService.ChunkLimitStatusEntry> collectMobStatus(ServerLevel level, ChunkPos chunkPos) {
        List<ChunkLimitService.ChunkLimitStatusEntry> entries = new ArrayList<>();
        Map<String, Integer> seen = new HashMap<>();
        addStatusEntries(entries, seen, level, chunkPos, LimitScope.CHUNK, false);
        return entries;
    }

    public static List<ChunkLimitService.ChunkLimitStatusEntry> collectTeamMobStatus(
            ServerLevel level,
            ChunkPos chunkPos
    ) {
        List<ChunkLimitService.ChunkLimitStatusEntry> entries = new ArrayList<>();
        UUID teamId = FtbIntegration.getTeamIdAt(level, chunkPos).orElse(null);
        if (teamId == null) {
            return entries;
        }
        Map<String, Integer> seen = new HashMap<>();
        Integer cap = ChunkLimitConfig.getEntityCap(LimitScope.TEAM);
        if (cap != null) {
            ChunkLimitKey key = new ChunkLimitKey(ChunkLimitConfig.ENTITY_CAP_LIMIT_ID, cap);
            entries.add(new ChunkLimitService.ChunkLimitStatusEntry(
                    key.id(), false, TeamMobLimitIndex.count(teamId, key), key.limit(), 0
            ));
            seen.put(key.id(), 1);
        }
        for (Map.Entry<ResourceLocation, Integer> entry : ChunkLimitConfig.getTeamEntityLimits().entrySet()) {
            ChunkLimitKey key = new ChunkLimitKey("entity:" + entry.getKey(), entry.getValue());
            if (seen.put(key.id(), 1) != null) {
                continue;
            }
            entries.add(new ChunkLimitService.ChunkLimitStatusEntry(
                    key.id(), false, TeamMobLimitIndex.count(teamId, key), key.limit(), 0
            ));
        }
        for (var binding : ChunkLimitConfig.getTeamEntityModBindings()) {
            if (seen.put(binding.key().id(), 1) != null) {
                continue;
            }
            entries.add(new ChunkLimitService.ChunkLimitStatusEntry(
                    binding.key().id(), false, TeamMobLimitIndex.count(teamId, binding.key()), binding.key().limit(), 0
            ));
        }
        return entries;
    }

    private static void addStatusEntries(
            List<ChunkLimitService.ChunkLimitStatusEntry> entries,
            Map<String, Integer> seen,
            ServerLevel level,
            ChunkPos chunkPos,
            LimitScope scope,
            boolean unused
    ) {
        Integer cap = ChunkLimitConfig.getEntityCap(scope);
        if (cap != null) {
            ChunkLimitKey key = new ChunkLimitKey(ChunkLimitConfig.ENTITY_CAP_LIMIT_ID, cap);
            if (seen.put(key.id(), 1) == null) {
                int current = countForKey(level, chunkPos, key);
                entries.add(new ChunkLimitService.ChunkLimitStatusEntry(
                        key.id(), false, current, key.limit(), Math.max(0, current - key.limit())
                ));
            }
        }
        Map<ResourceLocation, Integer> exact = scope == LimitScope.TEAM
                ? ChunkLimitConfig.getTeamEntityLimits()
                : ChunkLimitConfig.getEntityLimits();
        for (Map.Entry<ResourceLocation, Integer> entry : exact.entrySet()) {
            ChunkLimitKey key = new ChunkLimitKey("entity:" + entry.getKey(), entry.getValue());
            if (seen.put(key.id(), 1) != null) {
                continue;
            }
            int current = countForKey(level, chunkPos, key);
            entries.add(new ChunkLimitService.ChunkLimitStatusEntry(
                    key.id(), false, current, key.limit(), Math.max(0, current - key.limit())
            ));
        }
        var mods = scope == LimitScope.TEAM
                ? ChunkLimitConfig.getTeamEntityModBindings()
                : ChunkLimitConfig.getEntityModBindings();
        for (var binding : mods) {
            if (seen.put(binding.key().id(), 1) != null) {
                continue;
            }
            int current = countForKey(level, chunkPos, binding.key());
            entries.add(new ChunkLimitService.ChunkLimitStatusEntry(
                    binding.key().id(), false, current, binding.key().limit(), Math.max(0, current - binding.key().limit())
            ));
        }
    }
}
