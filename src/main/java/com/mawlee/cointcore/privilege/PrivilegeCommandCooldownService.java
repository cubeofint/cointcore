package com.mawlee.cointcore.privilege;

import com.mawlee.cointcore.config.PrivilegeCommandsConfig;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import net.minecraft.server.level.ServerPlayer;

public final class PrivilegeCommandCooldownService {
    private PrivilegeCommandCooldownService() {
    }

    public static int remainingSeconds(ServerPlayer player, PrivilegeCommandId commandId) {
        if (PermissionService.has(player, CointPermissionNodes.COOLDOWN_BYPASS)) {
            return 0;
        }
        long readyAt = PrivilegeCommandCooldownSavedData.get(player.server).readyAt(player.getUUID(), commandId);
        return PrivilegeCommandRules.remainingSeconds(readyAt, System.currentTimeMillis());
    }

    public static void arm(ServerPlayer player, PrivilegeCommandId commandId) {
        if (PermissionService.has(player, CointPermissionNodes.COOLDOWN_BYPASS)) {
            return;
        }
        int seconds = PrivilegeCommandsConfig.cooldownSeconds(commandId);
        if (seconds <= 0) {
            return;
        }
        long readyAt = PrivilegeCommandRules.readyAtEpochMs(System.currentTimeMillis(), seconds);
        PrivilegeCommandCooldownSavedData.get(player.server).setReadyAt(player.getUUID(), commandId, readyAt);
    }
}
