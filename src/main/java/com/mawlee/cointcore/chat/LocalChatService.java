package com.mawlee.cointcore.chat;

import com.mawlee.cointcore.config.ChatChannelsConfig;
import net.minecraft.server.level.ServerPlayer;

public final class LocalChatService {
    private LocalChatService() {
    }

    public static boolean isWithinLocalRange(ServerPlayer sender, ServerPlayer recipient) {
        if (sender.getUUID().equals(recipient.getUUID())) {
            return true;
        }

        if (sender.level() != recipient.level()) {
            return false;
        }

        double radius = ChatChannelsConfig.getLocalRadius();
        return sender.distanceToSqr(recipient) <= radius * radius;
    }
}
