package com.mawlee.cointcore.chunklimit;

import com.mawlee.cointcore.config.ChunkLimitConfig;
import com.mawlee.cointcore.ftb.FtbIntegration;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ChunkLimitService {
    private ChunkLimitService() {
    }

    public static boolean canBypass(ServerPlayer player) {
        return PermissionService.has(player, CointPermissionNodes.CHUNK_LIMIT_BYPASS);
    }

    /**
     * Whether a limited block entity at {@code pos} may tick.
     * Always allows ticking: over-limit machines placed by admins (bypass) must keep working.
     * Enforcement is hard-deny on non-bypass player placement only.
     */
    public static boolean shouldTickLimitedBlock(ServerLevel level, net.minecraft.core.BlockPos pos, Block block) {
        return true;
    }

    /**
     * Whether {@code placer} putting this block into {@code chunkPos} breaks a chunk, team or personal limit.
     * {@code additionalBlocks} is 1 before placement and 0 once the block is already in the chunk/team index.
     * Personal counts never include the block being placed (ownership is recorded after the event).
     */
    public static boolean wouldExceedBlockLimit(
            ServerPlayer placer,
            ServerLevel level,
            ChunkPos chunkPos,
            Block block,
            int additionalBlocks
    ) {
        return blockExceedReason(placer, level, chunkPos, block, additionalBlocks) != ExceedReason.NONE;
    }

    public static ExceedReason blockExceedReason(
            ServerPlayer placer,
            ServerLevel level,
            ChunkPos chunkPos,
            Block block,
            int additionalBlocks
    ) {
        if (!ChunkLimitConfig.isEnabled() || additionalBlocks < 0) {
            return ExceedReason.NONE;
        }
        ChunkLimitKey chunkKey = ChunkLimitConfig.resolveBlockKey(block);
        if (chunkKey != null
                && ChunkLimitIndex.count(level, chunkPos, chunkKey) + additionalBlocks > chunkKey.limit()) {
            return ExceedReason.CHUNK;
        }
        ChunkLimitKey teamKey = ChunkLimitConfig.resolveTeamBlockKey(block);
        if (teamKey != null) {
            UUID teamId = FtbIntegration.getTeamIdAt(level, chunkPos).orElse(null);
            if (teamId != null && TeamLimitIndex.count(teamId, teamKey) + additionalBlocks > teamKey.limit()) {
                return ExceedReason.TEAM;
            }
        }
        if (PlayerBlockLimitService.wouldExceed(placer, block)) {
            return ExceedReason.PLAYER;
        }
        return ExceedReason.NONE;
    }

    public static boolean wouldExceedEntityLimit(ServerLevel level, ChunkPos chunkPos, EntityType<?> type) {
        return MobLimitService.wouldExceedIfAdded(level, chunkPos, type, true)
                || MobLimitService.wouldExceedIfAdded(level, chunkPos, type, false);
    }

    public static int countBlocksInChunk(ServerLevel level, ChunkPos chunkPos, Block block) {
        ChunkLimitKey key = ChunkLimitConfig.resolveBlockKey(block);
        if (key == null) {
            return 0;
        }
        return ChunkLimitIndex.count(level, chunkPos, key);
    }

    public static int countEntitiesInChunk(ServerLevel level, ChunkPos chunkPos, EntityType<?> type) {
        AABB bounds = chunkBounds(level, chunkPos);
        List<Entity> entities = level.getEntities((Entity) null, bounds, entity -> entity.getType() == type && !entity.isRemoved());
        return entities.size();
    }

    /** Post-placement deny: the block is in the world and will be rolled back by the cancelled event. */
    public static void denyBlockPlacement(ServerPlayer player, BlockState placedState, ChunkPos chunkPos) {
        // NeoForge onPlaceItemIntoWorld restores the used stack on cancel; only other callers lose the item.
        if (!ItemPlacementGuard.isPlacingFromItem()) {
            ItemStack returnStack = placedState.getBlock().getCloneItemStack(
                    player.serverLevel(),
                    player.blockPosition(),
                    placedState
            );
            returnStackToPlayer(player, returnStack);
        }
        notifyBlockPlacementDenied(player, placedState, chunkPos, 0);
    }

    /** Message only — use when the item was not consumed (preemptive cancel). */
    public static void notifyBlockPlacementDenied(
            ServerPlayer player,
            BlockState placedState,
            ChunkPos chunkPos,
            int additionalBlocks
    ) {
        if (player instanceof FakePlayer) {
            return;
        }
        Block block = placedState.getBlock();
        ExceedReason reason = blockExceedReason(player, player.serverLevel(), chunkPos, block, additionalBlocks);
        ChunkLimitKey chunkKey = ChunkLimitConfig.resolveBlockKey(block);
        ChunkLimitKey teamKey = ChunkLimitConfig.resolveTeamBlockKey(block);
        ChunkLimitKey playerKey = ChunkLimitConfig.resolveBlockKey(ChunkLimitConfig.LimitScope.PLAYER, block);

        String messageKey;
        String targetId;
        int current;
        int limit;
        if (reason == ExceedReason.PLAYER && playerKey != null) {
            messageKey = CointCoreMessages.CHUNK_LIMIT_PLAYER_BLOCK_DENIED;
            targetId = playerKey.id();
            current = PlayerBlockLimitService.count(player.getServer(), player.getUUID(), playerKey);
            limit = playerKey.limit();
        } else if (reason == ExceedReason.TEAM && teamKey != null) {
            UUID teamId = FtbIntegration.getTeamIdAt(player.serverLevel(), chunkPos).orElse(null);
            messageKey = CointCoreMessages.CHUNK_LIMIT_TEAM_BLOCK_DENIED;
            targetId = teamKey.id();
            current = teamId != null ? TeamLimitIndex.count(teamId, teamKey) : 0;
            limit = teamKey.limit();
        } else {
            messageKey = CointCoreMessages.CHUNK_LIMIT_BLOCK_DENIED;
            targetId = chunkKey != null ? chunkKey.id() : formatId(BuiltInRegistries.BLOCK.getKey(block));
            current = chunkKey != null ? ChunkLimitIndex.count(player.serverLevel(), chunkPos, chunkKey) : 0;
            limit = chunkKey != null ? chunkKey.limit() : 0;
        }

        sendLimitMessage(
                player,
                messageKey,
                targetId,
                current,
                limit,
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
        Set<String> seenKeys = new LinkedHashSet<>();

        Map<ResourceLocation, Integer> exact = ChunkLimitConfig.getBlockLimits();
        for (Map.Entry<ResourceLocation, Integer> entry : exact.entrySet()) {
            Block block = BuiltInRegistries.BLOCK.get(entry.getKey());
            if (block == null) {
                continue;
            }
            ChunkLimitKey key = ChunkLimitConfig.resolveBlockKey(block);
            if (key == null || !seenKeys.add(key.id())) {
                continue;
            }
            int current = ChunkLimitIndex.count(level, chunkPos, key);
            entries.add(new ChunkLimitStatusEntry(
                    key.id(),
                    true,
                    current,
                    key.limit(),
                    Math.max(0, current - key.limit())
            ));
        }

        for (ChunkLimitConfig.GroupLimit group : ChunkLimitConfig.getGroups().values()) {
            ChunkLimitKey key = new ChunkLimitKey("group:" + group.name(), group.limit());
            if (!seenKeys.add(key.id())) {
                continue;
            }
            int current = ChunkLimitIndex.count(level, chunkPos, key);
            entries.add(new ChunkLimitStatusEntry(
                    key.id(),
                    true,
                    current,
                    key.limit(),
                    Math.max(0, current - key.limit())
            ));
        }

        for (ChunkLimitConfig.TagLimitBinding binding : ChunkLimitConfig.getTagBindings()) {
            if (!seenKeys.add(binding.key().id())) {
                continue;
            }
            int current = ChunkLimitIndex.count(level, chunkPos, binding.key());
            entries.add(new ChunkLimitStatusEntry(
                    binding.key().id(),
                    true,
                    current,
                    binding.key().limit(),
                    Math.max(0, current - binding.key().limit())
            ));
        }

        for (ChunkLimitConfig.ModLimitBinding binding : ChunkLimitConfig.getModBindings()) {
            if (!seenKeys.add(binding.key().id())) {
                continue;
            }
            int current = ChunkLimitIndex.count(level, chunkPos, binding.key());
            entries.add(new ChunkLimitStatusEntry(
                    binding.key().id(),
                    true,
                    current,
                    binding.key().limit(),
                    Math.max(0, current - binding.key().limit())
            ));
        }

        entries.addAll(MobLimitService.collectMobStatus(level, chunkPos));
        return entries;
    }

    public static List<ChunkLimitStatusEntry> collectTeamStatus(ServerLevel level, ChunkPos chunkPos) {
        List<ChunkLimitStatusEntry> entries = new ArrayList<>();
        UUID teamId = FtbIntegration.getTeamIdAt(level, chunkPos).orElse(null);
        if (teamId == null || !ChunkLimitConfig.hasTeamBlockLimits()) {
            return entries;
        }

        Set<String> seenKeys = new LinkedHashSet<>();
        for (Map.Entry<ResourceLocation, Integer> entry : ChunkLimitConfig.getTeamBlockLimits().entrySet()) {
            Block block = BuiltInRegistries.BLOCK.get(entry.getKey());
            if (block == null) {
                continue;
            }
            ChunkLimitKey key = ChunkLimitConfig.resolveTeamBlockKey(block);
            if (key == null || !seenKeys.add(key.id())) {
                continue;
            }
            int current = TeamLimitIndex.count(teamId, key);
            entries.add(new ChunkLimitStatusEntry(key.id(), true, current, key.limit(), 0));
        }
        for (ChunkLimitConfig.GroupLimit group : ChunkLimitConfig.getTeamGroups().values()) {
            ChunkLimitKey key = new ChunkLimitKey("group:" + group.name(), group.limit());
            if (!seenKeys.add(key.id())) {
                continue;
            }
            int current = TeamLimitIndex.count(teamId, key);
            entries.add(new ChunkLimitStatusEntry(key.id(), true, current, key.limit(), 0));
        }
        for (ChunkLimitConfig.TagLimitBinding binding : ChunkLimitConfig.getTeamTagBindings()) {
            if (!seenKeys.add(binding.key().id())) {
                continue;
            }
            int current = TeamLimitIndex.count(teamId, binding.key());
            entries.add(new ChunkLimitStatusEntry(
                    binding.key().id(), true, current, binding.key().limit(), 0
            ));
        }
        for (ChunkLimitConfig.ModLimitBinding binding : ChunkLimitConfig.getTeamModBindings()) {
            if (!seenKeys.add(binding.key().id())) {
                continue;
            }
            int current = TeamLimitIndex.count(teamId, binding.key());
            entries.add(new ChunkLimitStatusEntry(
                    binding.key().id(), true, current, binding.key().limit(), 0
            ));
        }
        return entries;
    }

    public enum ExceedReason {
        NONE,
        CHUNK,
        TEAM,
        PLAYER
    }

    public static void returnStackToPlayer(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack copy = stack.copy();
        // Prefer main hand if empty / same item (typical FakePlayer placer slot), else inventory.
        ItemStack main = player.getMainHandItem();
        if (main.isEmpty()) {
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, copy);
            return;
        }
        if (ItemStack.isSameItemSameComponents(main, copy)
                && main.getCount() < main.getMaxStackSize()) {
            int room = main.getMaxStackSize() - main.getCount();
            int move = Math.min(room, copy.getCount());
            main.grow(move);
            copy.shrink(move);
            if (copy.isEmpty()) {
                return;
            }
        }
        if (!player.getInventory().add(copy) && !(player instanceof FakePlayer)) {
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
            String id,
            boolean block,
            int current,
            int limit,
            int inert
    ) {
    }
}
