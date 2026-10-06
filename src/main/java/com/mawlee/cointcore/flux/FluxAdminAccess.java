package com.mawlee.cointcore.flux;

import com.mawlee.cointcore.luckperms.LuckPermsIntegration;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

public final class FluxAdminAccess {
    private static final String MESSAGES_CLASS = "sonar.fluxnetworks.register.Messages";

    private FluxAdminAccess() {
    }

    public static boolean canModifyAny(ServerPlayer player) {
        return player != null && PermissionService.has(player, CointPermissionNodes.FLUX_ADMIN);
    }

    public static void init(MinecraftServer server) {
        if (!ModList.get().isLoaded("fluxnetworks")) {
            return;
        }

        LuckPermsIntegration.registerUserDataRecalculateListener(playerId -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                server.execute(() -> syncCapability(player));
            }
        });
    }

    public static void syncCapability(ServerPlayer player) {
        if (player == null || !ModList.get().isLoaded("fluxnetworks")) {
            return;
        }

        try {
            Class.forName(MESSAGES_CLASS)
                    .getMethod("syncCapability", ServerPlayer.class)
                    .invoke(null, player);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
