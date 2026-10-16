package com.mawlee.cointcore.slots;

import com.mawlee.cointcore.luckperms.LuckPermsIntegration;
import com.mawlee.cointcore.luckperms.LuckPermsUsers;
import com.mawlee.cointcore.permission.CointPermissionNodes;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class ReservedSlotsPermissions {
    private static final long LOAD_TIMEOUT_MS = 2500L;

    private ReservedSlotsPermissions() {
    }

    public static boolean hasReserved(UUID playerId) {
        return loadAndCheck(playerId, CointPermissionNodes.luckPermsKey(CointPermissionNodes.RESERVED_SLOT));
    }

    public static boolean hasBypass(UUID playerId) {
        return loadAndCheck(playerId, CointPermissionNodes.luckPermsKey(CointPermissionNodes.RESERVED_SLOT_BYPASS));
    }

    private static boolean loadAndCheck(UUID playerId, String node) {
        if (!LuckPermsIntegration.isAvailable() || playerId == null || node == null || node.isBlank()) {
            return false;
        }
        try {
            Object user = LuckPermsUsers.loadUser(playerId).get(LOAD_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (user == null) {
                return false;
            }
            return LuckPermsIntegration.permissionTristate(playerId, node).orElse(false);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        } catch (TimeoutException | RuntimeException exception) {
            return false;
        } catch (Exception exception) {
            return false;
        }
    }
}
