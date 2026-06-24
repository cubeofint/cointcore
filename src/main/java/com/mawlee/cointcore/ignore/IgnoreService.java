package com.mawlee.cointcore.ignore;

import com.mawlee.cointcore.lang.CointCoreMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class IgnoreService {
    private IgnoreService() {
    }

    public static boolean toggle(ServerPlayer viewer, UUID targetId, String targetName) {
        if (viewer.getUUID().equals(targetId)) {
            viewer.sendSystemMessage(CointCoreMessages.forPlayer(viewer, CointCoreMessages.IGNORE_CANNOT_SELF));
            return false;
        }

        boolean ignored = IgnoreManager.toggle(viewer, targetId);
        viewer.sendSystemMessage(CointCoreMessages.forPlayer(
                viewer,
                ignored ? CointCoreMessages.IGNORE_ADDED : CointCoreMessages.IGNORE_REMOVED,
                targetName
        ));
        return ignored;
    }

    public static Optional<UUID> resolveTargetId(MinecraftServer server, String name) {
        ServerPlayer onlineMatch = null;

        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            if (!online.getGameProfile().getName().equalsIgnoreCase(name)) {
                continue;
            }

            if (onlineMatch != null) {
                return Optional.empty();
            }

            onlineMatch = online;
        }

        if (onlineMatch != null) {
            return Optional.of(onlineMatch.getUUID());
        }

        return server.getProfileCache()
                .get(name)
                .map(profile -> profile.getId());
    }

    public static Optional<String> resolveName(MinecraftServer server, UUID playerId) {
        ServerPlayer online = server.getPlayerList().getPlayer(playerId);
        if (online != null) {
            return Optional.of(online.getGameProfile().getName());
        }

        return server.getProfileCache()
                .get(playerId)
                .map(profile -> profile.getName());
    }

    public static boolean shouldHideFrom(ServerPlayer recipient, ServerPlayer sender) {
        return sender != null && IgnoreManager.isIgnored(recipient, sender.getUUID());
    }

    public static boolean shouldHideFrom(ServerPlayer recipient, UUID senderId) {
        return IgnoreManager.isIgnored(recipient, senderId);
    }

    public static boolean shouldHideOutgoingPacket(ServerPlayer recipient, Packet<?> packet) {
        return shouldHideChatPacket(recipient, packet) || shouldHideSystemChatPacket(recipient, packet);
    }

    public static boolean shouldHideChatPacket(ServerPlayer recipient, Packet<?> packet) {
        if (!(packet instanceof ClientboundPlayerChatPacket chatPacket)) {
            return false;
        }

        UUID senderId = chatPacket.sender();
        return senderId != null && IgnoreManager.isIgnored(recipient, senderId);
    }

    public static boolean shouldHideSystemChatPacket(ServerPlayer recipient, Packet<?> packet) {
        if (!(packet instanceof ClientboundSystemChatPacket systemPacket)) {
            return false;
        }

        return shouldHideSystemChat(recipient, systemPacket.content());
    }

    public static boolean shouldHideSystemChat(ServerPlayer recipient, Component content) {
        if (recipient == null || content == null) {
            return false;
        }

        Set<UUID> ignored = IgnoreManager.ignoredTargets(recipient);
        if (ignored.isEmpty()) {
            return false;
        }

        String text = content.getString();
        if (text.isEmpty()) {
            return false;
        }

        MinecraftServer server = recipient.server;
        for (UUID ignoredId : ignored) {
            Optional<String> name = resolveName(server, ignoredId);
            if (name.isPresent() && isChatMessageFromPlayer(text, name.get())) {
                return true;
            }
        }

        return false;
    }

    public static boolean shouldHidePlayerChatMessage(ServerPlayer recipient, PlayerChatMessage message) {
        UUID senderId = extractSenderId(message);
        return senderId != null && IgnoreManager.isIgnored(recipient, senderId);
    }

    private static UUID extractSenderId(PlayerChatMessage message) {
        if (message.link() != null) {
            return message.link().sender();
        }

        return null;
    }

    private static boolean isChatMessageFromPlayer(String text, String playerName) {
        if (isOutgoingPrivateMessage(text, playerName)) {
            return false;
        }

        if (isIncomingPrivateMessage(text, playerName)) {
            return true;
        }

        int searchFrom = 0;
        while (searchFrom < text.length()) {
            int nameIndex = text.indexOf(playerName, searchFrom);
            if (nameIndex < 0) {
                return false;
            }

            int colonIndex = nameIndex + playerName.length();
            if (colonIndex < text.length() && text.charAt(colonIndex) == ':') {
                if (nameIndex == 0 || isSenderNameBoundary(text.charAt(nameIndex - 1))) {
                    return true;
                }
            }

            searchFrom = nameIndex + 1;
        }

        return false;
    }

    private static boolean isOutgoingPrivateMessage(String text, String playerName) {
        return text.contains("-> " + playerName + ":")
                || text.contains("-> " + playerName + " ");
    }

    private static boolean isIncomingPrivateMessage(String text, String playerName) {
        return text.contains(playerName + " -> You")
                || text.contains(playerName + " -> Вам");
    }

    private static boolean isSenderNameBoundary(char beforeName) {
        return beforeName == ' ' || beforeName == ']' || beforeName == '>';
    }
}
