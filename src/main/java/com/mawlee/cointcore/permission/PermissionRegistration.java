package com.mawlee.cointcore.permission;

import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;

public final class PermissionRegistration {
    private PermissionRegistration() {
    }

    public static void registerNodes(PermissionGatherEvent.Nodes event) {
        event.addNodes(
                CointPermissionNodes.VANISH,
                CointPermissionNodes.VANISH_MOBS,
                CointPermissionNodes.SEE_VANISHED,
                CointPermissionNodes.RELOAD,
                CointPermissionNodes.NIGHT_VISION,
                CointPermissionNodes.FLY,
                CointPermissionNodes.GOD,
                CointPermissionNodes.MESSAGE,
                CointPermissionNodes.IGNORE,
                CointPermissionNodes.MUTE,
                CointPermissionNodes.UNMUTE,
                CointPermissionNodes.BAN,
                CointPermissionNodes.TPL,
                CointPermissionNodes.WARN,
                CointPermissionNodes.PUNISHMENTS,
                CointPermissionNodes.TURN_PVP,
                CointPermissionNodes.KEEP_INVENTORY,
                CointPermissionNodes.CLAIM_FLAGS,
                CointPermissionNodes.KIT_CREDITS,
                CointPermissionNodes.VOTE_DAY,
                CointPermissionNodes.VOTE_CLEAR_WEATHER,
                CointPermissionNodes.RESTART,
                CointPermissionNodes.CHAT_SPY,
                CointPermissionNodes.CHUNK_LIMIT,
                CointPermissionNodes.CHUNK_LIMIT_BYPASS,
                CointPermissionNodes.ADMIN_CHAT
        );
    }
}
