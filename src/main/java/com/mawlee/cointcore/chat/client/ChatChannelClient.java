package com.mawlee.cointcore.chat.client;

import com.mawlee.cointcore.chat.ChatChannel;
import com.mawlee.cointcore.chat.ChatViewPayload;
import com.mawlee.cointcore.lang.LegacyTextParser;
import net.minecraft.client.resources.language.I18n;
import com.mawlee.cointcore.mixin.client.accessor.ChatComponentChannelAccessor;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Client side of the chat channels: remembers the channel of each line received through
 * {@code ChatChannelMessagePayload} and hides lines of other channels when a tab is selected.
 * Lines that did not come through a channel (system, mods, Discord) are always shown.
 */
public final class ChatChannelClient {
    private static final ChatChannel[] CHANNELS = ChatChannel.values();
    private static final ChatChannel.View[] VIEWS = ChatChannel.View.values();
    private static final Map<GuiMessage, ChatChannel> TAGS = Collections.synchronizedMap(new WeakHashMap<>());

    private static ChatChannel.View view = ChatChannel.View.ALL;
    /** Channel of the line currently being added through {@link #receive}. */
    private static ChatChannel pending;
    private static boolean channelsSeen;

    private ChatChannelClient() {
    }

    public static ChatChannel.View view() {
        return view;
    }

    /** Tabs are shown once the server signals chat channels (on login, or with the first channel line). */
    public static boolean channelsActive() {
        return channelsSeen;
    }

    public static void receive(int channelOrdinal, Component message) {
        if (channelOrdinal < 0 || channelOrdinal >= CHANNELS.length) {
            return;
        }
        channelsSeen = true;
        pending = CHANNELS[channelOrdinal];
        try {
            Minecraft.getInstance().gui.getChat().addMessage(message);
        } finally {
            pending = null;
        }
    }

    public static void setView(int viewOrdinal) {
        if (viewOrdinal == ChatViewPayload.ACTIVATE_ONLY) {
            channelsSeen = true;
            return;
        }
        if (viewOrdinal >= 0 && viewOrdinal < VIEWS.length) {
            setView(VIEWS[viewOrdinal]);
        }
    }

    public static void setView(ChatChannel.View newView) {
        channelsSeen = true;
        if (newView == view) {
            return;
        }
        view = newView;
        ChatComponent chat = Minecraft.getInstance().gui.getChat();
        ((ChatComponentChannelAccessor) chat).cointcore$refreshTrimmedMessages();
        // Untagged line: stays visible whichever tab is selected next.
        chat.addMessage(LegacyTextParser.parse(I18n.get(ChatChannel.viewNoticeKey(newView))));
    }

    /** Called from the ChatComponent mixin for every line added to the history or the display. */
    public static void tag(GuiMessage message) {
        if (pending != null) {
            TAGS.put(message, pending);
        }
    }

    public static boolean isHidden(GuiMessage message) {
        ChatChannel channel = pending != null ? pending : TAGS.get(message);
        return channel != null && !view.shows(channel);
    }

    public static void reset() {
        view = ChatChannel.View.ALL;
        pending = null;
        channelsSeen = false;
        TAGS.clear();
    }
}
