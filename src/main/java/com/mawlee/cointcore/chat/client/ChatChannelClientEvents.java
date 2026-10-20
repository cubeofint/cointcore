package com.mawlee.cointcore.chat.client;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.chat.ChatChannel;
import com.mawlee.cointcore.lang.LegacyTextParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.resources.language.I18n;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Channel tabs ({@code [ALL] [L] [G] [$] [PM]}, localized) above the chat while it is open (as in CointCoreGTO). The selected
 * tab filters the chat and, for G and $, adds the channel prefix to typed lines.
 */
@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ChatChannelClientEvents {
    private static final String GLOBAL_CHAR = "!";
    private static final String TRADE_CHAR = "$";
    private static final int GAP = 6;
    private static final int PAD_X = 4;
    private static final int PAD_Y = 2;

    private static final Tab[] TABS = {
            new Tab("gui.cointcore.chat.tab.all", 0xFFFFFFFF, ChatChannel.View.ALL),
            new Tab("gui.cointcore.chat.tab.local", 0xFF55FF55, ChatChannel.View.LOCAL),
            new Tab("gui.cointcore.chat.tab.global", 0xFFFFAA00, ChatChannel.View.GLOBAL),
            new Tab("gui.cointcore.chat.tab.trade", 0xFF55FFFF, ChatChannel.View.TRADE),
            new Tab("gui.cointcore.chat.tab.private", 0xFFFF55FF, ChatChannel.View.PRIVATE)
    };

    private ChatChannelClientEvents() {
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof ChatScreen) || !ChatChannelClient.channelsActive()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        int width = PAD_X * 2 - GAP;
        for (Tab tab : TABS) {
            width += mc.font.width(tab.label()) + GAP;
        }
        int panelX = 2;
        int panelY = event.getScreen().height - 40 - mc.gui.getChat().getHeight() - 16;
        int height = mc.font.lineHeight + PAD_Y * 2;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 500);
        graphics.fill(panelX, panelY, panelX + width, panelY + height, 0xDD000000);
        int x = panelX + PAD_X;
        int y = panelY + PAD_Y;
        for (Tab tab : TABS) {
            int w = mc.font.width(tab.label());
            boolean active = ChatChannelClient.view() == tab.view;
            graphics.drawString(mc.font, tab.label(), x, y, active ? tab.color : (tab.color & 0x00FFFFFF) | 0x88000000, true);
            if (active) {
                graphics.fill(x, y + mc.font.lineHeight, x + w, y + mc.font.lineHeight + 1, tab.color);
            }
            tab.x1 = x;
            tab.y1 = y;
            tab.x2 = x + w;
            tab.y2 = y + mc.font.lineHeight;
            x += w + GAP;
        }
        graphics.pose().popPose();
    }

    @SubscribeEvent
    public static void onClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof ChatScreen) || !ChatChannelClient.channelsActive() || event.getButton() != 0) {
            return;
        }
        for (Tab tab : TABS) {
            if (tab.contains(event.getMouseX(), event.getMouseY())) {
                ChatChannelClient.setView(tab.view);
                event.setCanceled(true);
                return;
            }
        }
    }

    /** Typing in the G or $ tab sends to that channel; the PM tab only explains how to send a PM. */
    @SubscribeEvent
    public static void onClientChat(ClientChatEvent event) {
        if (!ChatChannelClient.channelsActive()) {
            return;
        }
        String message = event.getMessage();
        if (message.startsWith("/") || message.startsWith(GLOBAL_CHAR) || message.startsWith(TRADE_CHAR)) {
            return;
        }
        switch (ChatChannelClient.view()) {
            case GLOBAL -> event.setMessage(GLOBAL_CHAR + message);
            case TRADE -> event.setMessage(TRADE_CHAR + message);
            case PRIVATE -> {
                event.setCanceled(true);
                Minecraft.getInstance().gui.setOverlayMessage(
                        LegacyTextParser.parse(I18n.get("message.cointcore.chat.private_view")), false);
            }
            default -> {
            }
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ChatChannelClient.reset();
    }

    private static final class Tab {
        final String labelKey;
        final int color;
        final ChatChannel.View view;
        int x1;
        int y1;
        int x2;
        int y2;

        Tab(String labelKey, int color, ChatChannel.View view) {
            this.labelKey = labelKey;
            this.color = color;
            this.view = view;
        }

        String label() {
            return I18n.get(labelKey);
        }

        boolean contains(double x, double y) {
            return x >= x1 && x <= x2 && y >= y1 && y <= y2;
        }
    }
}
