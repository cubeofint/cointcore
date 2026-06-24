package com.mawlee.cointcore.chat;

import com.mawlee.cointcore.config.AdminChatConfig;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.mute.MuteManager;
import com.mawlee.cointcore.mute.MuteService;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class AdminChatService {
    private AdminChatService() {
    }

    public static boolean send(ServerPlayer sender, String message) {
        if (!AdminChatConfig.isEnabled()) {
            sender.sendSystemMessage(CointCoreMessages.forPlayer(sender, CointCoreMessages.ADMIN_CHAT_DISABLED));
            return false;
        }

        if (MuteManager.isMuted(sender)) {
            MuteService.notifyIfMuted(sender);
            return false;
        }

        if (message.isBlank()) {
            return false;
        }

        deliver(sender, message.strip());
        return true;
    }

    private static void deliver(ServerPlayer sender, String message) {
        MinecraftServer server = sender.server;
        String senderName = sender.getGameProfile().getName();

        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            if (!canParticipate(online)) {
                continue;
            }

            online.sendSystemMessage(CointCoreMessages.forPlayer(
                    online,
                    CointCoreMessages.ADMIN_CHAT,
                    senderName,
                    message
            ));
        }
    }

    public static boolean canParticipate(ServerPlayer player) {
        return PermissionService.has(player, CointPermissionNodes.ADMIN_CHAT);
    }
}
