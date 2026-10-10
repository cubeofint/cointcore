package com.mawlee.cointcore.shop;

/**
 * Seller-picker modal geometry. Sized for GUI scale 4 (480×270).
 */
public final class MarketSellerListLayout {
    public static final int Z = 400;
    public static final int DIM = 0xB0000000;
    public static final int PANEL = 0xFFC6C6C6;
    public static final int BORDER = 0xFF373737;
    public static final int PAD = 6;
    public static final int ROW_H = 28;
    public static final int TITLE_H = 12;
    public static final int BUY_H = 16;
    public static final int ACTION_H = 16;
    public static final int PANEL_MIN_W = 248;

    private MarketSellerListLayout() {
    }

    public record Geom(
            int panelX,
            int panelY,
            int panelW,
            int panelH,
            int titleX,
            int titleY,
            int listX,
            int listY,
            int listW,
            int rowH,
            int pageSize,
            int buyW,
            int buyH,
            int cheapestX,
            int cheapestY,
            int cheapestW,
            int closeX,
            int closeY,
            int closeW,
            int actionH
    ) {
        public boolean contains(int x, int y) {
            return x >= panelX && x < panelX + panelW && y >= panelY && y < panelY + panelH;
        }

        public int rowY(int index) {
            return listY + index * rowH;
        }

        public int buyX() {
            return listX + listW - buyW;
        }

        public int buyY(int index) {
            return rowY(index) + Math.max(0, (rowH - buyH) / 2);
        }

        public int textW() {
            return Math.max(16, listW - buyW - 4);
        }

        public boolean disjoint(int ax, int ay, int aw, int ah, int bx, int by, int bw, int bh) {
            return ax + aw <= bx || bx + bw <= ax || ay + ah <= by || by + bh <= ay;
        }
    }

    public static Geom of(
            int guiX,
            int guiY,
            int guiW,
            int guiH,
            int listTop,
            int buyW,
            int cheapestW,
            int closeW
    ) {
        int panelW = Math.min(guiW - 8, Math.max(PANEL_MIN_W, guiW - 12));
        int panelX = guiX + Math.max(4, (guiW - panelW) / 2);
        int buyWClamped = Math.max(36, buyW);
        int cheapestWClamped = Math.max(56, cheapestW);
        int closeWClamped = Math.max(40, closeW);
        int header = TITLE_H + 4;
        int footer = ACTION_H + PAD;
        int maxPanelH = Math.max(ROW_H * 2 + header + footer + PAD * 2, guiH - 8);
        maxPanelH = Math.min(maxPanelH, guiH - 8);
        int availableList = Math.max(ROW_H, maxPanelH - PAD * 2 - header - footer);
        int pageSize = Math.max(2, Math.min(5, availableList / ROW_H));
        int panelH = PAD + header + pageSize * ROW_H + footer + PAD;
        int panelY = guiY + Math.max(4, listTop);
        if (panelY + panelH > guiY + guiH - 4) {
            panelY = Math.max(guiY + 4, guiY + guiH - panelH - 4);
        }
        int innerX = panelX + PAD;
        int titleY = panelY + PAD;
        int listY = titleY + header;
        int listW = panelW - PAD * 2;
        int actionY = panelY + panelH - PAD - ACTION_H;
        int cheapestX = innerX;
        int closeX = cheapestX + cheapestWClamped + 6;
        if (closeX + closeWClamped > panelX + panelW - PAD) {
            int leftover = Math.max(80, (panelX + panelW - PAD) - innerX - 6);
            cheapestWClamped = Math.max(56, leftover / 2);
            closeWClamped = Math.max(40, leftover - cheapestWClamped);
            closeX = cheapestX + cheapestWClamped + 6;
        }
        return new Geom(
                panelX,
                panelY,
                panelW,
                panelH,
                innerX,
                titleY,
                innerX,
                listY,
                listW,
                ROW_H,
                pageSize,
                buyWClamped,
                BUY_H,
                cheapestX,
                actionY,
                cheapestWClamped,
                closeX,
                actionY,
                closeWClamped,
                ACTION_H
        );
    }
}
