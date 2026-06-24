package com.mawlee.cointcore.join;

import com.mawlee.cointcore.config.JoinMessagesConfig;
import com.mawlee.cointcore.lang.LegacyTextParser;
import net.minecraft.server.level.ServerPlayer;

public final class JoinMessageService {
    private JoinMessageService() {
    }

    public static void sendJoinMessages(ServerPlayer player) {
        for (String line : JoinMessagesConfig.getLines()) {
            player.sendSystemMessage(LegacyTextParser.parse(line));
        }
    }
}
