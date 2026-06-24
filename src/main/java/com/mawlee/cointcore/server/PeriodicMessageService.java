package com.mawlee.cointcore.server;

import com.mawlee.cointcore.config.ServerAutomationConfig;
import com.mawlee.cointcore.lang.LegacyTextParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class PeriodicMessageService {
    private static long lastBroadcastMs;
    private static int messageIndex;

    private PeriodicMessageService() {
    }

    public static void tick(MinecraftServer server) {
        ServerAutomationConfig.PeriodicMessageSettings settings = ServerAutomationConfig.getPeriodicMessages();
        if (!settings.enabled() || settings.messages().isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        long intervalMs = settings.intervalSeconds() * 1000L;
        if (now - lastBroadcastMs < intervalMs) {
            return;
        }

        lastBroadcastMs = now;
        String message = settings.messages().get(messageIndex);
        messageIndex = (messageIndex + 1) % settings.messages().size();

        Component component = LegacyTextParser.parse(message);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(component);
        }
    }

    public static void resetRuntimeState() {
        lastBroadcastMs = 0L;
        messageIndex = 0;
    }
}
