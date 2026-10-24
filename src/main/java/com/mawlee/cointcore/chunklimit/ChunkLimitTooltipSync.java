package com.mawlee.cointcore.chunklimit;

import com.mawlee.cointcore.config.ChunkLimitConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the item → placement-limit table from {@code chunk-limits.json} (blocks by id, tag, mod mask and group;
 * cable-bus parts by item id) and keeps clients in sync: on login and once a second when the rules version changed.
 */
public final class ChunkLimitTooltipSync {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static int sentVersion = Integer.MIN_VALUE;
    private static ChunkLimitTooltipPayload cached;

    private ChunkLimitTooltipSync() {
    }

    public static void onLogin(ServerPlayer player) {
        send(player, payload());
    }

    /** Called once a second; rebroadcasts after {@code /cointcore reload} or a {@code chunklimit} command. */
    public static void tick(MinecraftServer server) {
        if (ChunkLimitConfig.rulesVersion() == sentVersion) {
            return;
        }
        cached = null;
        ChunkLimitTooltipPayload payload = payload();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            send(player, payload);
        }
    }

    private static void send(ServerPlayer player, ChunkLimitTooltipPayload payload) {
        if (player.connection != null && player.connection.hasChannel(ChunkLimitTooltipPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    private static synchronized ChunkLimitTooltipPayload payload() {
        int version = ChunkLimitConfig.rulesVersion();
        if (cached == null || version != sentVersion) {
            cached = new ChunkLimitTooltipPayload(build());
            sentVersion = version;
            LOGGER.debug("Chunk limit tooltips rebuilt: {} items (rules v{})", cached.entries().size(), version);
        }
        return cached;
    }

    private static List<ChunkLimitTooltipPayload.Entry> build() {
        List<ChunkLimitTooltipPayload.Entry> entries = new ArrayList<>();
        if (!ChunkLimitConfig.isEnabled()) {
            return entries;
        }
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            ChunkLimitKey chunk;
            ChunkLimitKey team;
            ChunkLimitKey player;
            if (item instanceof BlockItem blockItem) {
                chunk = ChunkLimitConfig.resolveBlockKey(ChunkLimitConfig.LimitScope.CHUNK, blockItem.getBlock());
                team = ChunkLimitConfig.resolveBlockKey(ChunkLimitConfig.LimitScope.TEAM, blockItem.getBlock());
                player = ChunkLimitConfig.resolveBlockKey(ChunkLimitConfig.LimitScope.PLAYER, blockItem.getBlock());
            } else {
                chunk = ChunkLimitConfig.resolvePartKey(ChunkLimitConfig.LimitScope.CHUNK, id);
                team = ChunkLimitConfig.resolvePartKey(ChunkLimitConfig.LimitScope.TEAM, id);
                player = null;
            }
            if (chunk == null && team == null && player == null) {
                continue;
            }
            entries.add(new ChunkLimitTooltipPayload.Entry(id.toString(),
                    chunk == null ? -1 : chunk.limit(), ChunkLimitConfig.groupLabel(ChunkLimitConfig.LimitScope.CHUNK, chunk),
                    team == null ? -1 : team.limit(), ChunkLimitConfig.groupLabel(ChunkLimitConfig.LimitScope.TEAM, team),
                    player == null ? -1 : player.limit(), ChunkLimitConfig.groupLabel(ChunkLimitConfig.LimitScope.PLAYER, player)));
        }
        return entries;
    }
}
