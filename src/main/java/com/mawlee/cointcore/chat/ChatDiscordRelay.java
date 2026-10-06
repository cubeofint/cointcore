package com.mawlee.cointcore.chat;

import com.denisnumb.discord_chat_mod.DiscordChatMod;
import com.denisnumb.discord_chat_mod.discord.utils.DiscordMessageUtils;
import com.mawlee.cointcore.config.ChatDiscordRelayConfig;
import com.mojang.logging.LogUtils;
import com.shadow.net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

/**
 * Logs local chat / PMs to the server log and optionally relays them to a Discord channel
 * via the installed {@code discord_chat_mod} bot.
 */
public final class ChatDiscordRelay {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DISCORD_MOD_ID = "discord_chat_mod";
    private static final int DISCORD_MAX_CONTENT = 1900;

    private static boolean warnedMissingMod;
    private static boolean warnedEmptyChannel;
    private static boolean warnedDisconnected;

    private ChatDiscordRelay() {
    }

    public static void relayLocalChat(ServerPlayer sender, String message) {
        if (!ChatDiscordRelayConfig.isEnabled() || !ChatDiscordRelayConfig.isLogLocalChat()) {
            return;
        }
        if (sender == null || message == null || message.isBlank()) {
            return;
        }

        String line = ChatDiscordRelayConfig.getLocalFormat()
                .replace("{player}", sender.getGameProfile().getName())
                .replace("{message}", message);
        LOGGER.info("CHAT-LOCAL {}", line);
        sendToDiscord(line);
    }

    public static void relayPrivateMessage(ServerPlayer sender, ServerPlayer target, String message) {
        if (!ChatDiscordRelayConfig.isEnabled() || !ChatDiscordRelayConfig.isLogPrivateMessages()) {
            return;
        }
        if (sender == null || target == null || message == null || message.isBlank()) {
            return;
        }

        String line = ChatDiscordRelayConfig.getPmFormat()
                .replace("{from}", sender.getGameProfile().getName())
                .replace("{to}", target.getGameProfile().getName())
                .replace("{message}", message);
        LOGGER.info("CHAT-PM {}", line);
        sendToDiscord(line);
    }

    private static void sendToDiscord(String content) {
        String channelId = ChatDiscordRelayConfig.getDiscordChannelId();
        if (channelId == null || channelId.isBlank()) {
            if (!warnedEmptyChannel) {
                warnedEmptyChannel = true;
                LOGGER.warn("Chat Discord relay enabled but discord_channel_id is empty");
            }
            return;
        }
        if (!ModList.get().isLoaded(DISCORD_MOD_ID)) {
            if (!warnedMissingMod) {
                warnedMissingMod = true;
                LOGGER.warn("Chat Discord relay requires mod {}", DISCORD_MOD_ID);
            }
            return;
        }

        String sanitized = sanitize(truncate(content));
        try {
            DiscordMessageUtils.handleDiscord(() -> {
                if (!DiscordChatMod.isDiscordConnected() || DiscordChatMod.jda == null) {
                    if (!warnedDisconnected) {
                        warnedDisconnected = true;
                        LOGGER.warn("Chat Discord relay: discord_chat_mod is not connected yet");
                    }
                    return;
                }
                warnedDisconnected = false;
                var channel = DiscordChatMod.jda.getChannelById(GuildMessageChannel.class, channelId);
                if (channel == null) {
                    LOGGER.warn("Chat Discord relay: channel id {} not found or bot cannot see it", channelId);
                    return;
                }
                channel.sendMessage(sanitized).queue(
                        ignored -> {
                        },
                        error -> LOGGER.warn(
                                "Chat Discord relay failed to send to {}: {}",
                                channelId,
                                error.toString()
                        )
                );
            });
        } catch (Throwable throwable) {
            LOGGER.warn("Chat Discord relay send error", throwable);
        }
    }

    private static String sanitize(String text) {
        return text
                .replace("@everyone", "@\u200beveryone")
                .replace("@here", "@\u200bhere");
    }

    private static String truncate(String text) {
        if (text.length() <= DISCORD_MAX_CONTENT) {
            return text;
        }
        return text.substring(0, DISCORD_MAX_CONTENT - 3) + "...";
    }
}
