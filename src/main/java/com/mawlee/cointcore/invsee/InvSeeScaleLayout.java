package com.mawlee.cointcore.invsee;

import com.mawlee.cointcore.ui.ScaledGuiLayout;

/**
 * Keeps InvSee chrome (panel, tabs, edit / pager buttons) inside the scaled screen.
 */
public final class InvSeeScaleLayout {
    public static final int TAB_OVERHANG = 28;
    public static final int EDIT_RESERVE = InvSeeChromeLayout.EDIT_BUTTON_WIDTH + InvSeeChromeLayout.EDIT_BUTTON_GAP;
    public static final int SIDE_BUTTON_W = 18;
    public static final int SIDE_BUTTON_GAP = 4;

    private InvSeeScaleLayout() {
    }

    public record Fit(
            int imageWidth,
            int imageHeight,
            int leftPos,
            int topPos,
            int tabOverhang,
            boolean tabsAbove,
            boolean editInside,
            int viewerYShift,
            int editX,
            int editY
    ) {
        public boolean panelInside(int screenW, int screenH) {
            return new ScaledGuiLayout.Box(leftPos, topPos, imageWidth, imageHeight).inside(screenW, screenH);
        }

        public boolean chromeInside(int screenW, int screenH) {
            int top = tabsAbove ? topPos - tabOverhang : topPos;
            int bottom = topPos + imageHeight;
            int left = leftPos;
            int right = editInside ? leftPos + imageWidth : Math.max(leftPos + imageWidth, editX + InvSeeChromeLayout.EDIT_BUTTON_WIDTH);
            return left >= 0 && top >= 0 && right <= screenW && bottom <= screenH;
        }
    }

    public static Fit fit(int screenW, int screenH, int preferredW, int preferredH) {
        int margin = ScaledGuiLayout.MARGIN;
        int width = ScaledGuiLayout.clampSize(preferredW, screenW, 176, margin);
        int maxH = Math.max(96, screenH - margin * 2);
        boolean tabsAbove = preferredH + TAB_OVERHANG <= maxH;
        int imageH = Math.min(preferredH, tabsAbove ? maxH - TAB_OVERHANG : maxH);
        int overhang = tabsAbove ? TAB_OVERHANG : 0;
        boolean editInside = width + EDIT_RESERVE + margin * 2 > screenW;
        int trailing = editInside ? 0 : EDIT_RESERVE;
        int left = ScaledGuiLayout.origin(screenW, width, 0, trailing, margin);
        int top = ScaledGuiLayout.origin(screenH, imageH, overhang, 0, margin);
        if (top + imageH > screenH - margin) {
            top = Math.max(margin + overhang, screenH - margin - imageH);
        }
        if (top < overhang + margin && tabsAbove) {
            top = overhang + margin;
        }
        int shift = imageH - preferredH;
        int editX;
        int editY;
        if (editInside) {
            editX = left + width - InvSeeChromeLayout.EDIT_BUTTON_WIDTH - 4;
            editY = top + 4;
        } else {
            editX = left + width + InvSeeChromeLayout.EDIT_BUTTON_GAP;
            editY = top + 4;
        }
        return new Fit(width, imageH, left, top, overhang, tabsAbove, editInside, shift, editX, editY);
    }

    public static ScaledGuiLayout.Box sideButton(int leftPos, int imageWidth, int screenW, int y, boolean next) {
        int w = SIDE_BUTTON_W;
        int h = 16;
        if (next) {
            int x = leftPos + imageWidth + SIDE_BUTTON_GAP;
            if (x + w > screenW - ScaledGuiLayout.MARGIN) {
                x = leftPos + imageWidth - w - 2;
            }
            return new ScaledGuiLayout.Box(x, y, w, h);
        }
        int x = leftPos - w - SIDE_BUTTON_GAP;
        if (x < ScaledGuiLayout.MARGIN) {
            x = leftPos + 2;
        }
        return new ScaledGuiLayout.Box(x, y, w, h);
    }
}
