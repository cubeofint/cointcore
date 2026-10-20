package com.mawlee.cointcore.chat;

import com.mawlee.cointcore.chatspy.ChatSpyService;
import com.mawlee.cointcore.config.ChatChannelsConfig;
import com.mawlee.cointcore.ignore.IgnoreService;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.lang.LegacyTextParser;
import com.mawlee.cointcore.luckperms.LuckPermsIntegration;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Local / global / trade chat (port of the CointCoreGTO chat).
 *
 * <p>Local and trade lines are delivered here. Global lines stay on the vanilla broadcast path so
 * {@code discord_chat_mod} keeps relaying them to Discord; each recipient's copy is restyled in
 * {@code ServerGamePacketListenerImplMixin} via {@link #interceptGlobalPlayerChat}.
 *
 * <p>Clients with cointcore get every line through {@link ChatChannelMessagePayload} and filter tabs
 * themselves; other clients are filtered here by their {@code /chat} view.
 */
public final class ChatChannelService {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<UUID, ChatChannel.View> SERVER_VIEWS = new HashMap<>();
    private static final Map<UUID, Long> LAST_TRADE_MS = new HashMap<>();

    /** Text of the global line being broadcast right now (set in the chat event, read per recipient). */
    private static UUID pendingGlobalSender;
    private static String pendingGlobalText;

    private ChatChannelService() {
    }

    public static void onServerChat(ServerChatEvent event) {
        ChatChannelsConfig.Settings settings = ChatChannelsConfig.get();
        if (!settings.enabled() || event.isCanceled()) {
            return;
        }
        ServerPlayer sender = event.getPlayer();
        ChatRouting.Route route = ChatRouting.route(
                event.getRawText(), settings.globalChar(), settings.tradeChar(), serverView(sender));

        if (route.channel() == null) {
            event.setCanceled(true);
            String key = ChatRouting.PRIVATE_VIEW.equals(route.error())
                    ? CointCoreMessages.CHAT_PRIVATE_VIEW
                    : CointCoreMessages.CHAT_EMPTY_AFTER_PREFIX;
            sender.displayClientMessage(CointCoreMessages.forPlayer(sender, key,
                    event.getRawText().startsWith(settings.tradeChar()) ? settings.tradeChar() : settings.globalChar()), true);
            return;
        }

        switch (route.channel()) {
            case GLOBAL -> {
                // Leave the event alive: vanilla broadcast feeds discord_chat_mod; recipients are restyled later.
                pendingGlobalSender = sender.getUUID();
                pendingGlobalText = route.text();
                event.setMessage(Component.literal(route.text()));
            }
            case TRADE -> {
                event.setCanceled(true);
                sendTrade(sender, route.text(), settings);
            }
            default -> {
                event.setCanceled(true);
                sendLocal(sender, route.text(), settings);
            }
        }
    }

    private static void sendLocal(ServerPlayer sender, String text, ChatChannelsConfig.Settings settings) {
        Component line = format(sender, settings.localPrefix(), text);
        Set<UUID> recipients = new HashSet<>();
        for (ServerPlayer target : sender.server.getPlayerList().getPlayers()) {
            if (!LocalChatService.isWithinLocalRange(sender, target) || IgnoreService.shouldHideFrom(target, sender)) {
                continue;
            }
            deliver(target, ChatChannel.LOCAL, line);
            recipients.add(target.getUUID());
        }
        if (settings.nobodyNearbyHint() && recipients.size() <= 1) {
            sender.displayClientMessage(
                    CointCoreMessages.forPlayer(sender, CointCoreMessages.CHAT_NOBODY_NEARBY, settings.globalChar()), true);
        }
        ChatSpyService.notifyRelayChat(sender, text, recipients);
        ChatDiscordRelay.relayLocalChat(sender, text);
    }

    private static void sendTrade(ServerPlayer sender, String text, ChatChannelsConfig.Settings settings) {
        long now = System.currentTimeMillis();
        long cooldownMs = settings.tradeCooldownSeconds() * 1000L;
        Long last = LAST_TRADE_MS.get(sender.getUUID());
        if (cooldownMs > 0L && last != null && now - last < cooldownMs) {
            long left = (cooldownMs - (now - last) + 999L) / 1000L;
            sender.displayClientMessage(
                    CointCoreMessages.forPlayer(sender, CointCoreMessages.CHAT_TRADE_COOLDOWN, left), true);
            return;
        }
        LAST_TRADE_MS.put(sender.getUUID(), now);

        Component line = format(sender, settings.tradePrefix(), text);
        for (ServerPlayer target : sender.server.getPlayerList().getPlayers()) {
            if (!IgnoreService.shouldHideFrom(target, sender)) {
                deliver(target, ChatChannel.TRADE, line);
            }
        }
        LOGGER.info("CHAT-TRADE {}: {}", sender.getGameProfile().getName(), text);
    }

    /**
     * Called per recipient for vanilla player chat. Restyles a global line as {@code [G] prefix name: text} and
     * sends it through the channel path.
     *
     * @return {@code true} if the line was delivered here and the vanilla packet must be dropped
     */
    public static boolean interceptGlobalPlayerChat(ServerPlayer recipient, PlayerChatMessage message, ChatType.Bound bound) {
        ChatChannelsConfig.Settings settings = ChatChannelsConfig.get();
        if (!settings.enabled() || !bound.chatType().is(ChatType.CHAT)) {
            return false;
        }
        ServerPlayer sender = recipient.server.getPlayerList().getPlayer(message.sender());
        if (sender == null) {
            return false;
        }
        String text = message.sender().equals(pendingGlobalSender) && pendingGlobalText != null
                ? pendingGlobalText
                : stripPrefix(message.signedContent(), settings.globalChar());
        deliver(recipient, ChatChannel.GLOBAL, format(sender, settings.globalPrefix(), text));
        return true;
    }

    /** Delivers a channel line: tagged payload for cointcore clients, filtered system message otherwise. */
    public static void deliver(ServerPlayer target, ChatChannel channel, Component message) {
        if (!ChatChannelsConfig.isEnabled()) {
            target.sendSystemMessage(message);
            return;
        }
        if (target.connection != null && target.connection.hasChannel(ChatChannelMessagePayload.TYPE)) {
            PacketDistributor.sendToPlayer(target, new ChatChannelMessagePayload(channel.ordinal(), message));
            return;
        }
        if (serverView(target).shows(channel)) {
            target.sendSystemMessage(message);
        }
    }

    /** {@code /chat <view>}: cointcore clients switch their tabs, others are filtered server-side. */
    public static void setView(ServerPlayer player, ChatChannel.View view) {
        if (player.connection != null && player.connection.hasChannel(ChatViewPayload.TYPE)) {
            // The client switches its tab and prints the notice itself.
            PacketDistributor.sendToPlayer(player, new ChatViewPayload(view.ordinal()));
            return;
        }
        SERVER_VIEWS.put(player.getUUID(), view);
        player.sendSystemMessage(CointCoreMessages.forPlayer(player, ChatChannel.viewNoticeKey(view)));
    }

    public static ChatChannel.View serverView(ServerPlayer player) {
        return SERVER_VIEWS.getOrDefault(player.getUUID(), ChatChannel.View.ALL);
    }

    /** Tells a cointcore client right away that this server runs chat channels, so the tabs show before any line. */
    public static void onPlayerJoin(ServerPlayer player) {
        if (ChatChannelsConfig.isEnabled()
                && player.connection != null
                && player.connection.hasChannel(ChatViewPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, new ChatViewPayload(ChatViewPayload.ACTIVATE_ONLY));
        }
    }

    public static void onPlayerLeave(ServerPlayer player) {
        SERVER_VIEWS.remove(player.getUUID());
        LAST_TRADE_MS.remove(player.getUUID());
    }

    private static Component format(ServerPlayer sender, String channelPrefix, String text) {
        String lpPrefix = LuckPermsIntegration.getPrefix(sender.getUUID()).orElse("");
        MutableComponent line = Component.empty()
                .append(LegacyTextParser.parse(channelPrefix + lpPrefix))
                .append(LegacyTextParser.parse("&f" + sender.getGameProfile().getName() + "&7: "));
        return line.append(ChatLinkFormatter.linkify(Component.literal(text)));
    }

    private static String stripPrefix(String raw, String globalChar) {
        String text = raw == null ? "" : raw;
        return globalChar != null && !globalChar.isEmpty() && text.startsWith(globalChar)
                ? text.substring(globalChar.length()).trim()
                : text;
    }
}
