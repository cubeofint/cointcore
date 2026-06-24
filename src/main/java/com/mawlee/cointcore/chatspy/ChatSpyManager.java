package com.mawlee.cointcore.chatspy;

import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatSpyManager {
    private static final Set<UUID> ENABLED = ConcurrentHashMap.newKeySet();

    private ChatSpyManager() {
    }

    public static Set<UUID> enabledPlayers() {
        return Collections.unmodifiableSet(ENABLED);
    }

    public static boolean isEnabled(UUID playerId) {
        return playerId != null && ENABLED.contains(playerId);
    }

    public static void loadFromSavedData(MinecraftServer server) {
        ENABLED.clear();
        ENABLED.addAll(ChatSpySavedData.get(server).enabledPlayers());
    }

    public static void setEnabled(MinecraftServer server, UUID playerId, boolean enabled) {
        if (enabled) {
            ENABLED.add(playerId);
        } else {
            ENABLED.remove(playerId);
        }

        ChatSpySavedData.get(server).setEnabled(playerId, enabled);
    }

    public static void clearRuntimeState() {
        ENABLED.clear();
    }
}
