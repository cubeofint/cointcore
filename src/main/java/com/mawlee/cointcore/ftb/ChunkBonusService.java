package com.mawlee.cointcore.ftb;

import com.mawlee.cointcore.config.ChunkBonusConfig;
import com.mawlee.cointcore.luckperms.LuckPermsIntegration;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.ChunkTeamDataImpl;
import dev.ftb.mods.ftblibrary.integration.permissions.PermissionHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class ChunkBonusService {
    private ChunkBonusService() {
    }

    public static void init(MinecraftServer server) {
        if (!isActive()) {
            return;
        }

        LuckPermsIntegration.registerUserDataRecalculateListener(playerId -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                server.execute(() -> refreshPlayerLimits(player));
            }
        });
    }

    public static void onPlayerJoin(ServerPlayer player) {
        refreshPlayerLimits(player);
    }

    public static void refreshAllOnlinePlayers(MinecraftServer server) {
        if (!isActive()) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            refreshPlayerLimits(player);
        }
    }

    public static void refreshPlayerLimits(ServerPlayer player) {
        if (!FtbIntegration.isAvailable()) {
            return;
        }

        var teamData = FTBChunksAPI.api().getManager().getOrCreateData(player);
        if (teamData instanceof ChunkTeamDataImpl impl) {
            impl.updateLimits();
        }
    }

    public static int getBonusClaimedChunks(ServerPlayer player) {
        return sumMetaValues(player, ChunkBonusConfig.getBonusClaimMetaKeys());
    }

    public static int getBonusForceLoadedChunks(ServerPlayer player) {
        return sumMetaValues(player, ChunkBonusConfig.getBonusForceLoadMetaKeys());
    }

    public static boolean isActive() {
        return ChunkBonusConfig.isEnabled()
                && LuckPermsIntegration.isAvailable()
                && FtbIntegration.isAvailable();
    }

    private static int sumMetaValues(ServerPlayer player, List<String> metaKeys) {
        if (!isActive() || player == null) {
            return 0;
        }

        var provider = PermissionHelper.INSTANCE.getProvider();
        int total = 0;
        for (String metaKey : metaKeys) {
            total += Math.max(0, provider.getIntegerPermission(player, metaKey, 0));
        }
        return total;
    }
}
