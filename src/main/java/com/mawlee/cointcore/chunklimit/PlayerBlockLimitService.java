package com.mawlee.cointcore.chunklimit;

import com.mawlee.cointcore.config.ChunkLimitConfig;
import com.mawlee.cointcore.config.ChunkLimitConfig.LimitScope;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Per-player block limits: blocks count against the player who placed them, in every dimension.
 * Machine placements (FakePlayer) have no owner and only respect chunk / team limits.
 */
public final class PlayerBlockLimitService {
    private static MinecraftServer cachedServer;
    private static BlockOwnerSavedData cachedData;

    private PlayerBlockLimitService() {
    }

    public static boolean appliesTo(ServerPlayer player) {
        return player != null && !(player instanceof FakePlayer);
    }

    /** True when placing one more {@code block} would put {@code player} over a personal limit. */
    public static boolean wouldExceed(ServerPlayer player, Block block) {
        ChunkLimitKey key = ChunkLimitConfig.resolveBlockKey(LimitScope.PLAYER, block);
        if (key == null || !appliesTo(player)) {
            return false;
        }
        return count(player.getServer(), player.getUUID(), key) + 1 > key.limit();
    }

    public static int count(MinecraftServer server, UUID owner, ChunkLimitKey key) {
        return registry(server).count(owner, key.id());
    }

    public static void recordPlacement(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        if (!appliesTo(player) || ChunkLimitConfig.resolveBlockKey(LimitScope.PLAYER, state.getBlock()) == null) {
            return;
        }
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        long packed = pos.asLong();
        registry(level.getServer()).put(dimensionId(level), ChunkPos.asLong(pos), packed, player.getUUID(), blockId.toString());
        data(level.getServer()).setDirty();
    }

    /** Drops ownership whenever the block at {@code pos} turns into a different block. */
    public static void onBlockChange(ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState) {
        if (oldState.getBlock() == newState.getBlock()) {
            return;
        }
        BlockOwnerRegistry registry = registry(level.getServer());
        if (registry.isEmpty()) {
            return;
        }
        if (registry.remove(dimensionId(level), ChunkPos.asLong(pos), pos.asLong()) != null) {
            data(level.getServer()).setDirty();
        }
    }

    /** Removes owners whose recorded block no longer matches the world (pistons, wipes, edits while unloaded). */
    public static void validateChunk(ServerLevel level, LevelChunk chunk) {
        BlockOwnerRegistry registry = registry(level.getServer());
        if (registry.isEmpty()) {
            return;
        }
        String dimension = dimensionId(level);
        long chunkKey = chunk.getPos().toLong();
        boolean changed = false;
        for (long packed : registry.positionsInChunk(dimension, chunkKey)) {
            BlockOwnerRegistry.Entry entry = registry.get(dimension, packed);
            BlockState state = chunk.getBlockState(BlockPos.of(packed));
            ResourceLocation actual = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (entry == null || !actual.toString().equals(entry.blockId())) {
                registry.remove(dimension, chunkKey, packed);
                changed = true;
            }
        }
        if (changed) {
            data(level.getServer()).setDirty();
        }
    }

    public static List<ChunkLimitService.ChunkLimitStatusEntry> collectStatus(MinecraftServer server, UUID owner) {
        List<ChunkLimitService.ChunkLimitStatusEntry> entries = new ArrayList<>();
        for (ChunkLimitKey key : configuredKeys()) {
            entries.add(new ChunkLimitService.ChunkLimitStatusEntry(
                    key.id(), true, count(server, owner, key), key.limit(), 0
            ));
        }
        return entries;
    }

    private static List<ChunkLimitKey> configuredKeys() {
        List<ChunkLimitKey> keys = new ArrayList<>();
        for (var entry : ChunkLimitConfig.getBlockLimits(LimitScope.PLAYER).entrySet()) {
            keys.add(new ChunkLimitKey("block:" + entry.getKey(), entry.getValue()));
        }
        for (ChunkLimitConfig.GroupLimit group : ChunkLimitConfig.getGroups(LimitScope.PLAYER).values()) {
            keys.add(new ChunkLimitKey("group:" + group.name(), group.limit()));
        }
        for (ChunkLimitConfig.TagLimitBinding binding : ChunkLimitConfig.getTagBindings(LimitScope.PLAYER)) {
            keys.add(binding.key());
        }
        for (ChunkLimitConfig.ModLimitBinding binding : ChunkLimitConfig.getModBindings(LimitScope.PLAYER)) {
            keys.add(binding.key());
        }
        return keys;
    }

    private static BlockOwnerRegistry registry(MinecraftServer server) {
        BlockOwnerRegistry registry = data(server).registry();
        registry.ensureResolver(ChunkLimitConfig.rulesVersion(), PlayerBlockLimitService::resolveKeyId);
        return registry;
    }

    private static BlockOwnerSavedData data(MinecraftServer server) {
        if (cachedServer != server || cachedData == null) {
            cachedData = BlockOwnerSavedData.get(server);
            cachedServer = server;
        }
        return cachedData;
    }

    private static String resolveKeyId(String blockId) {
        ResourceLocation id = ResourceLocation.tryParse(blockId);
        if (id == null) {
            return null;
        }
        return BuiltInRegistries.BLOCK.getOptional(id)
                .map(block -> ChunkLimitConfig.resolveBlockKey(LimitScope.PLAYER, block))
                .map(ChunkLimitKey::id)
                .orElse(null);
    }

    private static String dimensionId(ServerLevel level) {
        return level.dimension().location().toString();
    }
}
