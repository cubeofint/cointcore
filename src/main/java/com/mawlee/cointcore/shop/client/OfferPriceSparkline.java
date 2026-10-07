package com.mawlee.cointcore.shop.client;

import com.mawlee.cointcore.shop.OfferPriceVerdict;
import com.mawlee.cointcore.shop.PriceSparklineLayout;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Vanilla-looking 40×14 sparkline: inset well and a 1px polyline.
 */
public final class OfferPriceSparkline {
    public static final int WIDTH = 40;
    public static final int HEIGHT = 14;
    private static final int PAD = 1;
    private static final int WELL = 0xFF373737;
    private static final int EDGE_DARK = 0xFF8B8B8B;
    private static final int EDGE_LIGHT = 0xFFFFFFFF;
    private static final int COLOR_CHEAP = 0xFF3D8C3D;
    private static final int COLOR_EXPENSIVE = 0xFFA03A3A;
    private static final int COLOR_FAIR = 0xFF7A7A7A;
    private static final int COLOR_UNKNOWN = 0xFF6B6B6B;

    private OfferPriceSparkline() {
    }

    public static int color(OfferPriceVerdict verdict) {
        return switch (verdict) {
            case CHEAP -> COLOR_CHEAP;
            case EXPENSIVE -> COLOR_EXPENSIVE;
            case FAIR -> COLOR_FAIR;
            case UNKNOWN -> COLOR_UNKNOWN;
        };
    }

    public static void blit(GuiGraphics graphics, int x, int y, long[] history, OfferPriceVerdict verdict) {
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, WELL);
        graphics.fill(x, y, x + WIDTH, y + 1, EDGE_DARK);
        graphics.fill(x, y, x + 1, y + HEIGHT, EDGE_DARK);
        graphics.fill(x, y + HEIGHT - 1, x + WIDTH, y + HEIGHT, EDGE_LIGHT);
        graphics.fill(x + WIDTH - 1, y, x + WIDTH, y + HEIGHT, EDGE_LIGHT);
        int innerW = WIDTH - PAD * 2;
        int innerH = HEIGHT - PAD * 2;
        int[] ys = PriceSparklineLayout.toY(history, innerH);
        if (ys.length == 0) {
            int mid = y + PAD + (innerH - 1) / 2;
            graphics.fill(x + PAD, mid, x + PAD + innerW, mid + 1, color(OfferPriceVerdict.UNKNOWN));
            return;
        }
        int line = color(verdict);
        int originX = x + PAD;
        int originY = y + PAD;
        if (ys.length == 1) {
            int px = originX + PriceSparklineLayout.xForIndex(0, 1, innerW);
            int py = originY + ys[0];
            graphics.fill(px, py, px + 1, py + 1, line);
            return;
        }
        for (int index = 1; index < ys.length; index++) {
            int x0 = originX + PriceSparklineLayout.xForIndex(index - 1, ys.length, innerW);
            int y0 = originY + ys[index - 1];
            int x1 = originX + PriceSparklineLayout.xForIndex(index, ys.length, innerW);
            int y1 = originY + ys[index];
            drawLine(graphics, x0, y0, x1, y1, line);
        }
    }

    private static void drawLine(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0;
        int y = y0;
        while (true) {
            graphics.fill(x, y, x + 1, y + 1, color);
            if (x == x1 && y == y1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }
}
