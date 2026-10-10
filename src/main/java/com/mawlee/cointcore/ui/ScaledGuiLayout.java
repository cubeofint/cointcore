package com.mawlee.cointcore.ui;

/**
 * Screen-space fitting for inventory GUIs. All sizes are already GUI-scaled pixels
 * ({@code 1920×1080} at scale 4 is {@code 480×270}).
 */
public final class ScaledGuiLayout {
    public static final int MARGIN = 4;
    public static final int SLOT = 18;
    public static final int PLAYER_INV_HEIGHT = 82;
    public static final int PLAYER_INV_LABEL = 12;
    public static final int PLAYER_INV_BLOCK = PLAYER_INV_LABEL + PLAYER_INV_HEIGHT;
    public static final int SCALE4_1080P_W = 480;
    public static final int SCALE4_1080P_H = 270;
    public static final int SCALE3_1080P_W = 640;
    public static final int SCALE3_1080P_H = 360;

    private ScaledGuiLayout() {
    }

    public static int clampSize(int preferred, int screen, int min, int margin) {
        int max = Math.max(min, screen - Math.max(0, margin) * 2);
        if (preferred <= 0) {
            return Math.max(min, max);
        }
        return Math.max(min, Math.min(preferred, max));
    }

    public static int origin(int screen, int size, int leadingReserve, int trailingReserve, int margin) {
        int min = Math.max(0, margin + Math.max(0, leadingReserve));
        int max = screen - Math.max(0, margin) - Math.max(0, trailingReserve) - size;
        if (max < min) {
            return Math.max(0, Math.min(min, screen - size));
        }
        int centered = (screen - size) / 2;
        return Math.max(min, Math.min(max, centered));
    }

    public static int pageRows(int availableHeight, int rowHeight, int minRows, int maxRows) {
        int row = Math.max(1, rowHeight);
        int min = Math.max(1, minRows);
        int max = Math.max(min, maxRows);
        if (availableHeight < row) {
            return min;
        }
        return Math.max(min, Math.min(max, availableHeight / row));
    }

    public static boolean fits(int width, int height, int screenW, int screenH, int margin) {
        int pad = Math.max(0, margin) * 2;
        return width > 0
                && height > 0
                && width + pad <= screenW
                && height + pad <= screenH;
    }

    public record Box(int x, int y, int w, int h) {
        public int right() {
            return x + w;
        }

        public int bottom() {
            return y + h;
        }

        public boolean inside(int screenW, int screenH) {
            return x >= 0 && y >= 0 && right() <= screenW && bottom() <= screenH;
        }
    }
}
