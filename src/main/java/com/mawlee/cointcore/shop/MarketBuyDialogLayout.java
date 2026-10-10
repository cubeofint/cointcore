package com.mawlee.cointcore.shop;

/**
 * Opaque buy-quantity modal geometry. Sized for GUI scale 4 (480×270).
 */
public final class MarketBuyDialogLayout {
    public static final int QTY_W = 90;
    public static final int QTY_H = 14;
    public static final int QUICK_W = 32;
    public static final int QUICK_H = 14;
    public static final int ACTION_H = 16;
    public static final int PAD = 8;
    public static final int ROW_GAP = 6;
    public static final int Z = 400;
    public static final int DIM = 0xB0000000;
    public static final int PANEL = 0xFFC6C6C6;
    public static final int BORDER = 0xFF373737;
    public static final int PANEL_MIN_W = 240;
    public static final int PANEL_H = 92;

    private MarketBuyDialogLayout() {
    }

    public record Geom(
            int panelX,
            int panelY,
            int panelW,
            int panelH,
            int qtyX,
            int qtyY,
            int qtyW,
            int qtyH,
            int oneX,
            int tenX,
            int maxX,
            int quickY,
            int quickW,
            int quickH,
            int quoteX,
            int quoteY,
            int buyX,
            int cancelX,
            int actionY,
            int actionW,
            int actionH
    ) {
        public boolean contains(int x, int y) {
            return x >= panelX && x < panelX + panelW && y >= panelY && y < panelY + panelH;
        }

        public boolean disjoint(int ax, int ay, int aw, int ah, int bx, int by, int bw, int bh) {
            return ax + aw <= bx || bx + bw <= ax || ay + ah <= by || by + bh <= ay;
        }
    }

    public static Geom of(int guiX, int guiY, int guiW, int guiH, int listTop, int labelW, int buyW, int cancelW) {
        int panelW = Math.min(guiW - 12, Math.max(PANEL_MIN_W, labelW + 4 + QTY_W + 4 + QUICK_W * 3 + 8 + PAD * 2));
        int panelH = PANEL_H;
        int panelX = guiX + Math.max(6, (guiW - panelW) / 2);
        int panelY = guiY + listTop + 4;
        if (panelY + panelH > guiY + guiH - 6) {
            panelY = Math.max(guiY + 6, guiY + guiH - panelH - 6);
        }
        int innerX = panelX + PAD;
        int row1Y = panelY + PAD;
        int qtyX = innerX + Math.max(0, labelW) + 4;
        int qtyW = QTY_W;
        int quickW = QUICK_W;
        int oneX = qtyX + qtyW + 4;
        int tenX = oneX + quickW + 2;
        int maxX = tenX + quickW + 2;
        if (maxX + quickW > panelX + panelW - PAD) {
            qtyW = Math.max(64, (panelX + panelW - PAD) - qtyX - (quickW + 2) * 3 - 4);
            oneX = qtyX + qtyW + 4;
            tenX = oneX + quickW + 2;
            maxX = tenX + quickW + 2;
        }
        int quoteY = row1Y + QTY_H + ROW_GAP;
        int actionH = ACTION_H;
        int actionY = panelY + panelH - PAD - actionH;
        int buyWClamped = Math.max(40, buyW);
        int cancelWClamped = Math.max(40, cancelW);
        int buyX = innerX;
        int cancelX = buyX + buyWClamped + 6;
        return new Geom(
                panelX,
                panelY,
                panelW,
                panelH,
                qtyX,
                row1Y,
                qtyW,
                QTY_H,
                oneX,
                tenX,
                maxX,
                row1Y,
                quickW,
                QUICK_H,
                innerX,
                quoteY,
                buyX,
                cancelX,
                actionY,
                buyWClamped,
                actionH
        );
    }
}
