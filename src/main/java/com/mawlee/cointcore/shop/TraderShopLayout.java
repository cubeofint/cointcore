package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.ui.ScaledGuiLayout;

/**
 * Buyer-facing shop / global-market geometry computed from the scaled screen size.
 */
public record TraderShopLayout(
        int guiWidth,
        int guiHeight,
        int titleHeight,
        int rowHeight,
        int pageSize,
        int statusHeight,
        int pageBarHeight,
        int offerPanelHeight,
        int playerInvY,
        int playerInvLeft,
        int tabY,
        int searchY,
        int titleY,
        int balanceY,
        boolean tabs
) {
    public static final int MARKET_PREFERRED_W = 360;
    public static final int MARKET_MAX_ROWS = 3;
    public static final int MARKET_ROW_H = 36;
    public static final int TERMINAL_PREFERRED_W = 256;
    public static final int TERMINAL_MAX_ROWS = 4;
    public static final int TERMINAL_ROW_H = 54;
    public static final int MIN_WIDTH = 176;
    public static final int STATUS_H = 12;
    public static final int PAGE_BAR_H = 16;
    public static final int INV_GAP = 10;

    public static TraderShopLayout market(int screenW, int screenH, boolean tabs) {
        int titleH = tabs ? 40 : 36;
        return compute(
                screenW,
                screenH,
                MARKET_PREFERRED_W,
                titleH,
                MARKET_ROW_H,
                MARKET_MAX_ROWS,
                tabs
        );
    }

    public static TraderShopLayout terminal(int screenW, int screenH) {
        return compute(
                screenW,
                screenH,
                TERMINAL_PREFERRED_W,
                24,
                TERMINAL_ROW_H,
                TERMINAL_MAX_ROWS,
                false
        );
    }

    public static TraderShopLayout preferredMarket(boolean tabs) {
        return market(ScaledGuiLayout.SCALE3_1080P_W, ScaledGuiLayout.SCALE3_1080P_H, tabs);
    }

    public static TraderShopLayout preferredTerminal() {
        return terminal(ScaledGuiLayout.SCALE3_1080P_W, ScaledGuiLayout.SCALE3_1080P_H);
    }

    private static TraderShopLayout compute(
            int screenW,
            int screenH,
            int preferredW,
            int titleH,
            int rowH,
            int maxRows,
            boolean tabs
    ) {
        int width = ScaledGuiLayout.clampSize(preferredW, screenW, MIN_WIDTH, ScaledGuiLayout.MARGIN);
        int maxH = Math.max(160, screenH - ScaledGuiLayout.MARGIN * 2);
        int chrome = titleH + STATUS_H + PAGE_BAR_H + INV_GAP + ScaledGuiLayout.PLAYER_INV_HEIGHT;
        int availableRows = Math.max(rowH, maxH - chrome);
        int rows = ScaledGuiLayout.pageRows(availableRows, rowH, 1, maxRows);
        int offerPanel = titleH + rows * rowH + STATUS_H + PAGE_BAR_H;
        int invY = offerPanel + INV_GAP;
        int height = invY + ScaledGuiLayout.PLAYER_INV_HEIGHT;
        if (height > maxH) {
            rows = 1;
            offerPanel = titleH + rows * rowH + STATUS_H + PAGE_BAR_H;
            invY = offerPanel + INV_GAP;
            height = Math.min(maxH, invY + ScaledGuiLayout.PLAYER_INV_HEIGHT);
            invY = Math.min(invY, height - ScaledGuiLayout.PLAYER_INV_HEIGHT);
            offerPanel = Math.max(titleH + rowH, invY - INV_GAP);
        }
        int tabY = 4;
        int searchY = tabs ? 22 : 18;
        int titleY = tabs ? 6 : 6;
        int balanceY = tabs ? 24 : 16;
        return new TraderShopLayout(
                width,
                height,
                titleH,
                rowH,
                rows,
                STATUS_H,
                PAGE_BAR_H,
                offerPanel,
                invY,
                (width - TraderMenu.VANILLA_INV_WIDTH) / 2 + 8,
                tabY,
                searchY,
                titleY,
                balanceY,
                tabs
        );
    }

    public int listTop() {
        return titleHeight + 2;
    }

    public int rowY(int row) {
        return listTop() + row * rowHeight;
    }

    public int pagerY() {
        return offerPanelHeight - pageBarHeight + 1;
    }

    public int statusY() {
        return titleHeight + pageSize * rowHeight + 1;
    }

    public boolean insideScreen(int screenW, int screenH) {
        return ScaledGuiLayout.fits(guiWidth, guiHeight, screenW, screenH, ScaledGuiLayout.MARGIN);
    }
}
