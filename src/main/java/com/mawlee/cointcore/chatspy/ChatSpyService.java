package com.mawlee.cointcore.chatspy;

import com.mawlee.cointcore.chat.LocalChatService;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;

public final class ChatSpyService {
    private static final ThreadLocal<Boolean> SUPPRESS_TRACKING = ThreadLocal.withInitial(() -> false);

    private ChatSpyService() {
    }

    static boolean isSuppressingTracking() {
        return Boolean.TRUE.equals(SUPPRESS_TRACKING.get());
    }

    private static void sendSpyMessage(ServerPlayer spy, Component message) {
        SUPPRESS_TRACKING.set(true);
        try {
            spy.sendSystemMessage(message);
        } finally {
            SUPPRESS_TRACKING.set(false);
        }
    }

    public static boolean toggle(ServerPlayer player) {
        boolean enabled = !ChatSpyManager.isEnabled(player.getUUID());
        setEnabled(player, enabled, true);
        return enabled;
    }

    public static void setEnabled(ServerPlayer player, boolean enabled, boolean notify) {
        ChatSpyManager.setEnabled(player.server, player.getUUID(), enabled);
        if (!notify) {
            return;
        }

        player.sendSystemMessage(CointCoreMessages.forPlayer(
                player,
                enabled ? CointCoreMessages.CHAT_SPY_ENABLED : CointCoreMessages.CHAT_SPY_DISABLED
        ));
    }

    public static void onPlayerJoin(ServerPlayer player) {
        if (!PermissionService.has(player, CointPermissionNodes.CHAT_SPY)
                && ChatSpyManager.isEnabled(player.getUUID())) {
            setEnabled(player, false, false);
        }
    }

    public static void notifyRelayChat(ServerPlayer sender, String message, Set<UUID> recipients) {
        MinecraftServer server = sender.server;
        String senderName = sender.getGameProfile().getName();

        for (ServerPlayer spy : onlineSpies(server)) {
            if (shouldSkipLocalSpy(sender, spy, recipients)) {
                continue;
            }

            sendSpyMessage(spy, CointCoreMessages.forPlayer(spy, CointCoreMessages.CHAT_SPY_LOCAL, senderName, message));
        }
    }

    public static void notifyPrivateMessage(ServerPlayer sender, ServerPlayer target, String message) {
        MinecraftServer server = sender.server;
        for (ServerPlayer spy : onlineSpies(server)) {
            UUID spyId = spy.getUUID();
            if (spyId.equals(sender.getUUID()) || spyId.equals(target.getUUID())) {
                continue;
            }

            sendSpyMessage(spy, CointCoreMessages.forPlayer(
                    spy,
                    CointCoreMessages.CHAT_SPY_PM,
                    sender.getGameProfile().getName(),
                    target.getGameProfile().getName(),
                    message
            ));
        }
    }

    public static void notifyLocalPlayerChat(
            MinecraftServer server,
            UUID senderId,
            PlayerChatMessage message,
            ChatType.Bound bound,
            Set<UUID> recipients
    ) {
        String senderName = resolveName(server, senderId);
        String text = message.decoratedContent().getString();

        ServerPlayer sender = server.getPlayerList().getPlayer(senderId);
        for (ServerPlayer spy : onlineSpies(server)) {
            if (shouldSkipLocalSpy(sender, spy, recipients)) {
                continue;
            }

            sendSpyMessage(spy, CointCoreMessages.forPlayer(
                    spy,
                    CointCoreMessages.CHAT_SPY_LOCAL,
                    senderName,
                    text
            ));
        }
    }

    public static void notifyLocalSystemChat(MinecraftServer server, Component message, Set<UUID> recipients) {
        String text = message.getString();
        for (ServerPlayer spy : onlineSpies(server)) {
            if (recipients.contains(spy.getUUID())) {
                continue;
            }

            sendSpyMessage(spy, CointCoreMessages.forPlayer(spy, CointCoreMessages.CHAT_SPY_LOCAL_RAW, text));
        }
    }

    private static boolean shouldSkipLocalSpy(ServerPlayer sender, ServerPlayer spy, Set<UUID> recipients) {
        UUID spyId = spy.getUUID();
        if (sender != null && spyId.equals(sender.getUUID())) {
            return true;
        }

        if (recipients.contains(spyId)) {
            return true;
        }

        return sender != null && LocalChatService.isWithinLocalRange(sender, spy);
    }

    private static Iterable<ServerPlayer> onlineSpies(MinecraftServer server) {
        return () -> server.getPlayerList().getPlayers().stream()
                .filter(player -> ChatSpyManager.isEnabled(player.getUUID()))
                .filter(player -> PermissionService.has(player, CointPermissionNodes.CHAT_SPY))
                .iterator();
    }

    private static String resolveName(MinecraftServer server, UUID playerId) {
        ServerPlayer online = server.getPlayerList().getPlayer(playerId);
        if (online != null) {
            return online.getGameProfile().getName();
        }

        return server.getProfileCache()
                .get(playerId)
                .map(profile -> profile.getName())
                .orElse("?");
    }
}
