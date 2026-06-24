package com.mawlee.cointcore.join;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Set;

public final class JoinLeaveMessageFilter {
    private static final Set<String> JOIN_LEAVE_KEYS = Set.of(
            "multiplayer.player.joined",
            "multiplayer.player.left",
            "multiplayer.player.joined.renamed"
    );

    private static final Set<String> JOIN_LEAVE_PLAIN_MARKERS = Set.of(
            "joined the game",
            "left the game",
            "зашёл в игру",
            "зашла в игру",
            "покинул игру",
            "покинула игру"
    );

    private JoinLeaveMessageFilter() {
    }

    public static boolean shouldSuppressOutgoingPacket(Packet<?> packet) {
        if (!(packet instanceof ClientboundSystemChatPacket systemPacket)) {
            return false;
        }

        return isJoinOrLeaveMessage(systemPacket.content());
    }

    public static boolean isJoinOrLeaveMessage(Component message) {
        if (message == null) {
            return false;
        }

        if (containsJoinLeaveTranslatable(message)) {
            return true;
        }

        return matchesJoinLeavePlainText(message.getString());
    }

    private static boolean containsJoinLeaveTranslatable(Component message) {
        Deque<Component> pending = new ArrayDeque<>();
        pending.add(message);

        while (!pending.isEmpty()) {
            Component current = pending.removeFirst();
            ComponentContents contents = current.getContents();
            if (contents instanceof TranslatableContents translatable && isJoinLeaveKey(translatable.getKey())) {
                return true;
            }
            pending.addAll(current.getSiblings());
        }

        return false;
    }

    private static boolean matchesJoinLeavePlainText(String plain) {
        if (plain == null || plain.isBlank()) {
            return false;
        }

        String normalized = plain.toLowerCase(Locale.ROOT);
        for (String marker : JOIN_LEAVE_PLAIN_MARKERS) {
            if (normalized.contains(marker)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isJoinLeaveKey(String key) {
        return key != null && JOIN_LEAVE_KEYS.contains(key);
    }
}
