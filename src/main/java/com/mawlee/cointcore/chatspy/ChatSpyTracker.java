package com.mawlee.cointcore.chatspy;

import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ChatSpyTracker {
    private static final Map<Long, PendingPlayerChat> PLAYER_CHAT = new HashMap<>();
    private static final Map<Integer, PendingSystemChat> SYSTEM_CHAT = new HashMap<>();

    private ChatSpyTracker() {
    }

    public static void recordPlayerChat(ServerPlayer recipient, PlayerChatMessage message, ChatType.Bound bound) {
        if (ChatSpyService.isSuppressingTracking() || message.link() == null) {
            return;
        }

        long index = message.link().index();
        PendingPlayerChat pending = PLAYER_CHAT.computeIfAbsent(index, ignored -> new PendingPlayerChat(message, bound));
        pending.recipients.add(recipient.getUUID());
        pending.senderId = message.link().sender();
    }

    public static void recordSystemChat(ServerPlayer recipient, Component message) {
        if (ChatSpyService.isSuppressingTracking() || message == null) {
            return;
        }

        String text = message.getString();
        if (text.isEmpty()) {
            return;
        }

        int key = text.hashCode();
        PendingSystemChat pending = SYSTEM_CHAT.computeIfAbsent(key, ignored -> new PendingSystemChat(message));
        pending.recipients.add(recipient.getUUID());
    }

    public static void flush(MinecraftServer server) {
        if (PLAYER_CHAT.isEmpty() && SYSTEM_CHAT.isEmpty()) {
            return;
        }

        int eligibleRecipients = countEligibleRecipients(server);

        for (PendingPlayerChat pending : PLAYER_CHAT.values()) {
            if (eligibleRecipients <= 0 || pending.recipients.size() >= eligibleRecipients) {
                continue;
            }

            ChatSpyService.notifyLocalPlayerChat(server, pending.senderId, pending.message, pending.bound, pending.recipients);
        }

        for (PendingSystemChat pending : SYSTEM_CHAT.values()) {
            if (eligibleRecipients <= 0 || pending.recipients.size() >= eligibleRecipients) {
                continue;
            }

            ChatSpyService.notifyLocalSystemChat(server, pending.message, pending.recipients);
        }

        PLAYER_CHAT.clear();
        SYSTEM_CHAT.clear();
    }

    public static void clearRuntimeState() {
        PLAYER_CHAT.clear();
        SYSTEM_CHAT.clear();
    }

    private static int countEligibleRecipients(MinecraftServer server) {
        int count = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
                count++;
            }
        }
        return count;
    }

    private static final class PendingPlayerChat {
        private final PlayerChatMessage message;
        private final ChatType.Bound bound;
        private final Set<UUID> recipients = new HashSet<>();
        private UUID senderId;

        private PendingPlayerChat(PlayerChatMessage message, ChatType.Bound bound) {
            this.message = message;
            this.bound = bound;
        }
    }

    private static final class PendingSystemChat {
        private final Component message;
        private final Set<UUID> recipients = new HashSet<>();

        private PendingSystemChat(Component message) {
            this.message = message;
        }
    }
}
