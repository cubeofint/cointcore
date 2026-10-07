package com.mawlee.cointcore.invsee;

/**
 * InvSee chrome geometry that stays out of the panel title row.
 */
public final class InvSeeChromeLayout {
    public static final int EDIT_BUTTON_WIDTH = 36;
    public static final int EDIT_BUTTON_HEIGHT = 16;
    public static final int EDIT_BUTTON_GAP = 2;
    public static final int TITLE_X = 8;
    public static final int TITLE_PAD = 8;

    private InvSeeChromeLayout() {
    }

    public static int editButtonX(int leftPos, int imageWidth) {
        return leftPos + imageWidth + EDIT_BUTTON_GAP;
    }

    public static int editButtonY(int topPos) {
        return topPos + 4;
    }

    public static int titleMaxWidth(int imageWidth) {
        return Math.max(0, imageWidth - TITLE_X - TITLE_PAD);
    }

    public static boolean titleOverlapsEditButton(
            int titleX,
            int titleY,
            int titleWidth,
            int titleHeight,
            int buttonX,
            int buttonY,
            int buttonWidth,
            int buttonHeight
    ) {
        return titleX < buttonX + buttonWidth
                && buttonX < titleX + titleWidth
                && titleY < buttonY + buttonHeight
                && buttonY < titleY + titleHeight;
    }
}
