package com.mawlee.cointcore.privilege;

import com.mawlee.cointcore.nightvision.NightVisionService;
import net.minecraft.server.level.ServerPlayer;

public final class PrivilegeEnforcement {
    private PrivilegeEnforcement() {
    }

    public static void enforceNightVision(ServerPlayer player) {
        NightVisionService.revokeIfUnauthorized(player);
    }
}
