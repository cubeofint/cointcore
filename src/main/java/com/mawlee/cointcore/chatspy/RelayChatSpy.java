package com.mawlee.cointcore.chatspy;

import com.mawlee.cointcore.chat.LocalChatService;
import com.mawlee.cointcore.config.RelpChatPrefixConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.ServerChatEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks L/G chat delivered by Re-LPChatPrefix after it cancels vanilla {@link ServerChatEvent}.
 */
public final class RelayChatSpy {
    private RelayChatSpy() {
    }

    public static void onServerChat(ServerChatEvent event) {
        if (!event.isCanceled() || ChatSpyService.isSuppressingTracking()) {
            return;
        }

        ServerPlayer sender = event.getPlayer();
        String rawText = event.getRawText();
        if (rawText.isBlank()) {
            return;
        }

        ParsedRelayChat parsed = parse(rawText);
        if (parsed.message().isBlank() || parsed.global()) {
            return;
        }

        Set<UUID> recipients = resolveRecipients(sender);
        ChatSpyService.notifyRelayChat(sender, parsed.message(), recipients);
    }

    private static ParsedRelayChat parse(String rawText) {
        String globalPrefix = RelpChatPrefixConfig.getGlobalPrefix();
        String trimmed = rawText.stripLeading();
        if (!globalPrefix.isEmpty() && trimmed.startsWith(globalPrefix)) {
            return new ParsedRelayChat(true, trimmed.substring(globalPrefix.length()).stripLeading());
        }

        return new ParsedRelayChat(false, rawText);
    }

    private static Set<UUID> resolveRecipients(ServerPlayer sender) {
        Set<UUID> recipients = new HashSet<>();
        recipients.add(sender.getUUID());

        for (ServerPlayer online : sender.server.getPlayerList().getPlayers()) {
            if (online.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                continue;
            }

            if (LocalChatService.isWithinLocalRange(sender, online)) {
                recipients.add(online.getUUID());
            }
        }

        return recipients;
    }

    private record ParsedRelayChat(boolean global, String message) {
    }
}
