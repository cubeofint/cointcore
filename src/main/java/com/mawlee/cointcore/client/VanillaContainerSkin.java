package com.mawlee.cointcore.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Vanilla container chrome: 9-slice border from {@code generic_54}, flat inner fill,
 * beveled 18px slot frames only where callers request them, and creative tab sprites.
 */
public final class VanillaContainerSkin {
    public static final ResourceLocation GENERIC_54 =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    public static final ResourceLocation INVENTORY =
            ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png");
    public static final ResourceLocation CREATIVE_TABS =
            ResourceLocation.withDefaultNamespace("textures/gui/container/creative_inventory/tabs.png");

    public static final int LABEL_COLOR = 0x404040;
    public static final int PANEL_FILL = 0xFFC6C6C6;
    public static final int ROW_FILL = 0xFFB8B8B8;
    public static final int ROW_LINE = 0xFF8B8B8B;
    public static final int PANEL_WIDTH = 176;
    public static final int SLOT = 18;
    public static final int TAB_WIDTH = 26;
    public static final int TAB_HEIGHT = 32;
    public static final int TAB_SHIFT = 28;
    public static final int INVENTORY_TEXTURE_HEIGHT = 166;
    public static final int PLAYER_STRIP_HEIGHT = 96;
    public static final int PLAYER_STRIP_V = 125;

    private static final int SRC_W = 176;
    private static final int SRC_H = 222;
    private static final int CORNER = 4;
    private static final int SLOT_U = 7;
    private static final int SLOT_V = 17;
    /** Title-bar sample (no chest slots) used to stretch side bezels. */
    private static final int SIDE_V = 3;

    private VanillaContainerSkin() {
    }

    /**
     * Outer vanilla container: stretched border from {@code generic_54}, inner fill {@code 0xC6C6C6}.
     * Does not tile the chest slot grid.
     */
    public static void blitPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        int innerW = Math.max(0, width - CORNER * 2);
        int innerH = Math.max(0, height - CORNER * 2);
        graphics.fill(x + CORNER, y + CORNER, x + width - CORNER, y + height - CORNER, PANEL_FILL);
        blit(graphics, GENERIC_54, x, y, 0, 0, CORNER, CORNER);
        blit(graphics, GENERIC_54, x + width - CORNER, y, SRC_W - CORNER, 0, CORNER, CORNER);
        blit(graphics, GENERIC_54, x, y + height - CORNER, 0, SRC_H - CORNER, CORNER, CORNER);
        blit(graphics, GENERIC_54, x + width - CORNER, y + height - CORNER, SRC_W - CORNER, SRC_H - CORNER, CORNER, CORNER);
        blitH(graphics, GENERIC_54, x + CORNER, y, CORNER, 0, SRC_W - CORNER * 2, CORNER, innerW);
        blitH(graphics, GENERIC_54, x + CORNER, y + height - CORNER, CORNER, SRC_H - CORNER, SRC_W - CORNER * 2, CORNER, innerW);
        blitV(graphics, GENERIC_54, x, y + CORNER, 0, SIDE_V, CORNER, 1, innerH);
        blitV(graphics, GENERIC_54, x + width - CORNER, y + CORNER, SRC_W - CORNER, SIDE_V, CORNER, 1, innerH);
    }

    public static void blitChestBody(GuiGraphics graphics, int x, int y, int rows) {
        int topH = 17 + rows * SLOT;
        blitPanel(graphics, x, y, PANEL_WIDTH, topH + PLAYER_STRIP_HEIGHT);
    }

    public static void blitPlayerInventoryLayout(GuiGraphics graphics, int x, int y) {
        blitPanel(graphics, x, y, PANEL_WIDTH, INVENTORY_TEXTURE_HEIGHT);
    }

    /**
     * No-op: a second generic_54 strip would overlay a nested border and slot grid.
     * Player slots are drawn with {@link #blitSlot} / {@link #blitMenuSlots} on {@link #blitPanel}.
     */
    public static void blitPlayerStrip(GuiGraphics graphics, int x, int y) {
        // Intentionally empty so callers cannot overlay a nested slot-grid strip.
    }

    public static void blitSlot(GuiGraphics graphics, int screenX, int screenY) {
        blit(graphics, GENERIC_54, screenX, screenY, SLOT_U, SLOT_V, SLOT, SLOT);
    }

    public static void blitMenuSlots(GuiGraphics graphics, int left, int top, List<Slot> slots) {
        for (Slot slot : slots) {
            if (!slot.isActive()) {
                continue;
            }
            blitSlot(graphics, left + slot.x - 1, top + slot.y - 1);
        }
    }

    public static void blitOfferRow(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, ROW_FILL);
        graphics.fill(x, y + height - 1, x + width, y + height, ROW_LINE);
    }

    public static void blitCreativeTab(GuiGraphics graphics, int x, int y, boolean selected, ItemStack icon) {
        int v = selected ? 32 : 0;
        graphics.blit(CREATIVE_TABS, x, y, 0, v, TAB_WIDTH, TAB_HEIGHT);
        if (!icon.isEmpty()) {
            graphics.renderItem(icon, x + 5, y + (selected ? 9 : 11));
        }
    }

    private static void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y, int u, int v, int w, int h) {
        graphics.blit(texture, x, y, u, v, w, h);
    }

    private static void blitH(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int y,
            int u,
            int v,
            int srcW,
            int srcH,
            int destW
    ) {
        int remaining = destW;
        int drawX = x;
        while (remaining > 0) {
            int slice = Math.min(srcW, remaining);
            blit(graphics, texture, drawX, y, u, v, slice, srcH);
            remaining -= slice;
            drawX += slice;
        }
    }

    private static void blitV(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int y,
            int u,
            int v,
            int srcW,
            int srcH,
            int destH
    ) {
        int remaining = destH;
        int drawY = y;
        while (remaining > 0) {
            int slice = Math.min(srcH, remaining);
            blit(graphics, texture, x, drawY, u, v, srcW, slice);
            remaining -= slice;
            drawY += slice;
        }
    }
}
