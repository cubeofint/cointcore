package com.mawlee.cointcore.pvp;

import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.mute.TimeUtil;
import net.minecraft.server.level.ServerPlayer;

public final class PvpModeService {
    private PvpModeService() {
    }

    public static void toggle(ServerPlayer player) {
        PvpModeManager.ToggleResult result = PvpModeManager.tryToggle(player);

        switch (result.state()) {
            case ENABLED -> player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.PVP_ENABLED));
            case DISABLED -> player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.PVP_DISABLED));
            case PEACE_REQUIRED -> player.sendSystemMessage(CointCoreMessages.forPlayer(
                    player,
                    CointCoreMessages.PVP_PEACE_REQUIRED,
                    TimeUtil.formatDuration(result.remainingMs())
            ));
        }
    }
}
