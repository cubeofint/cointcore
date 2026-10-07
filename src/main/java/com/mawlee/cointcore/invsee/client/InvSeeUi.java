package com.mawlee.cointcore.invsee.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/**
 * Shared drawing for the modern InvSee chrome (flat panels, slot wells, headers).
 */
public final class InvSeeUi {
    private InvSeeUi() {
    }

    public static void fill(GuiGraphics graphics, int x, int y, int w, int h, int argb) {
        graphics.fill(x, y, x + w, y + h, argb);
    }

    public static void panel(GuiGraphics graphics, int x, int y, int w, int h) {
        fill(graphics, x, y, w, h, InvSeeTheme.PANEL);
        // border
        fill(graphics, x, y, w, 1, InvSeeTheme.LINE);
        fill(graphics, x, y + h - 1, w, 1, InvSeeTheme.LINE);
        fill(graphics, x, y, 1, h, InvSeeTheme.LINE);
        fill(graphics, x + w - 1, y, 1, h, InvSeeTheme.LINE);
    }

    public static void accentBar(GuiGraphics graphics, int x, int y, int h) {
        fill(graphics, x, y, 2, h, InvSeeTheme.ACCENT);
    }

    /**
     * Outer shell: header strip + main body around the slot coordinate box.
     */
    public static void drawShell(
            GuiGraphics graphics,
            Font font,
            int left,
            int top,
            int width,
            int height,
            Component brand,
            Component title,
            int viewerInventoryY
    ) {
        int hx = left - InvSeeTheme.PAD;
        int hy = top - InvSeeTheme.HEADER_H - InvSeeTheme.TAB_H - InvSeeTheme.PAD;
        int hw = width + InvSeeTheme.PAD * 2;
        int hh = height + InvSeeTheme.HEADER_H + InvSeeTheme.TAB_H + InvSeeTheme.PAD * 2;

        panel(graphics, hx, hy, hw, hh);
        fill(graphics, hx + 1, hy + 1, hw - 2, InvSeeTheme.HEADER_H, InvSeeTheme.PANEL_INNER);
        fill(graphics, hx + 1, hy + InvSeeTheme.HEADER_H, hw - 2, 1, InvSeeTheme.LINE_SOFT);
        accentBar(graphics, hx, hy, InvSeeTheme.HEADER_H + 1);

        int textY = hy + (InvSeeTheme.HEADER_H - 8) / 2 + 1;
        graphics.drawString(font, brand, hx + 10, textY, InvSeeTheme.ACCENT_TEXT, false);
        int brandW = font.width(brand);
        String sep = " / ";
        graphics.drawString(font, sep, hx + 10 + brandW, textY, InvSeeTheme.FAINT, false);
        int titleX = hx + 10 + brandW + font.width(sep);
        String clipped = font.plainSubstrByWidth(title.getString(), hw - (titleX - hx) - 12);
        graphics.drawString(font, clipped, titleX, textY, InvSeeTheme.TEXT, false);

        if (viewerInventoryY > 0 && viewerInventoryY < height) {
            int sepY = top + viewerInventoryY - 12;
            fill(graphics, left + 6, sepY, width - 12, 1, InvSeeTheme.LINE_SOFT);
        }
    }

    public static void drawSlotWell(GuiGraphics graphics, int screenX, int screenY, boolean hot) {
        int bg = hot ? InvSeeTheme.SLOT_HOVER : InvSeeTheme.SLOT;
        fill(graphics, screenX - 1, screenY - 1, 18, 18, InvSeeTheme.LINE);
        fill(graphics, screenX, screenY, 16, 16, bg);
    }

    public static void drawSlotWells(
            GuiGraphics graphics,
            int left,
            int top,
            List<Slot> slots,
            int mouseX,
            int mouseY
    ) {
        for (Slot slot : slots) {
            if (!slot.isActive()) {
                continue;
            }
            int sx = left + slot.x;
            int sy = top + slot.y;
            boolean hot = mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16;
            drawSlotWell(graphics, sx, sy, hot);
        }
    }

    public static void drawPortraitFrame(GuiGraphics graphics, int x, int y, int w, int h) {
        fill(graphics, x, y, w, h, InvSeeTheme.SLOT);
        fill(graphics, x, y, w, 1, InvSeeTheme.LINE);
        fill(graphics, x, y + h - 1, w, 1, InvSeeTheme.LINE);
        fill(graphics, x, y, 1, h, InvSeeTheme.LINE);
        fill(graphics, x + w - 1, y, 1, h, InvSeeTheme.LINE);
    }

    public static void drawSectionHint(GuiGraphics graphics, Font font, int x, int y, Component text) {
        graphics.drawString(font, text, x, y, InvSeeTheme.FAINT, false);
    }
}
