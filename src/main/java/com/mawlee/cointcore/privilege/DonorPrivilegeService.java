package com.mawlee.cointcore.privilege;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.luckperms.LuckPermsIntegration;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DonorPrivilegeService {
    private static final Map<UUID, DonorGrantState> STATES = new ConcurrentHashMap<>();

    private DonorPrivilegeService() {
    }

    public static void init(MinecraftServer server) {
        if (!LuckPermsIntegration.isAvailable()) {
            return;
        }

        LuckPermsIntegration.registerUserDataRecalculateListener(playerId -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                server.execute(() -> onPermissionsRecalculated(player));
            }
        });
    }

    public static void onPlayerJoin(ServerPlayer player) {
        checkPlayer(player);
    }

    public static void onPlayerLeave(ServerPlayer player) {
        STATES.remove(player.getUUID());
    }

    public static void onPermissionsRecalculated(ServerPlayer player) {
        checkPlayer(player);
    }

    private static void checkPlayer(ServerPlayer player) {
        if (!LuckPermsIntegration.isAvailable()) {
            return;
        }

        DonorGrantState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new DonorGrantState());
        checkFly(player, state);
        checkGod(player, state);
    }

    private static void checkFly(ServerPlayer player, DonorGrantState state) {
        String permission = permissionKey(CointPermissionNodes.FLY.getNodeName());
        boolean hasPermission = PermissionService.has(player, CointPermissionNodes.FLY);
        boolean hasTemporaryGrant = LuckPermsIntegration.hasActiveTemporaryGrant(player.getUUID(), permission);

        if (shouldRevokeDonorFly(player, state, hasPermission, permission)) {
            revokeVanillaFly(player);
        }

        state.hadTemporaryFly = hasTemporaryGrant;
    }

    private static void checkGod(ServerPlayer player, DonorGrantState state) {
        String permission = permissionKey(CointPermissionNodes.GOD.getNodeName());
        boolean hasPermission = PermissionService.has(player, CointPermissionNodes.GOD);
        boolean hasTemporaryGrant = LuckPermsIntegration.hasActiveTemporaryGrant(player.getUUID(), permission);

        if (shouldRevokeDonorGod(player, state, hasPermission, permission)) {
            revokeVanillaGod(player);
        }

        state.hadTemporaryGod = hasTemporaryGrant;
    }

    private static boolean shouldRevokeDonorFly(
            ServerPlayer player,
            DonorGrantState state,
            boolean hasPermission,
            String permission
    ) {
        if (hasPermission || isProtectedGameMode(player) || FlightSourceService.shouldPreserveFlight(player)) {
            return false;
        }

        var abilities = player.getAbilities();
        if (!abilities.mayfly && !abilities.flying) {
            return false;
        }

        return state.hadTemporaryFly || LuckPermsIntegration.hadTemporaryGrantExpire(player.getUUID(), permission);
    }

    private static boolean shouldRevokeDonorGod(
            ServerPlayer player,
            DonorGrantState state,
            boolean hasPermission,
            String permission
    ) {
        if (hasPermission || isProtectedGameMode(player)) {
            return false;
        }

        if (!player.getAbilities().invulnerable) {
            return false;
        }

        return state.hadTemporaryGod || LuckPermsIntegration.hadTemporaryGrantExpire(player.getUUID(), permission);
    }

    private static void revokeVanillaFly(ServerPlayer player) {
        if (FlightSourceService.shouldPreserveFlight(player)) {
            return;
        }

        var abilities = player.getAbilities();
        abilities.mayfly = false;
        if (!FlightSourceService.shouldPreserveGliding(player)) {
            abilities.flying = false;
        }
        player.onUpdateAbilities();
        player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.FLY_DISABLED));
    }

    private static void revokeVanillaGod(ServerPlayer player) {
        player.getAbilities().invulnerable = false;
        player.onUpdateAbilities();
        player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.GOD_DISABLED));
    }

    private static boolean isProtectedGameMode(ServerPlayer player) {
        GameType gameType = player.gameMode.getGameModeForPlayer();
        return gameType == GameType.CREATIVE || gameType == GameType.SPECTATOR;
    }

    private static String permissionKey(String nodeName) {
        return CointCore.MOD_ID + "." + nodeName;
    }

    private static final class DonorGrantState {
        private boolean hadTemporaryFly;
        private boolean hadTemporaryGod;
    }
}
