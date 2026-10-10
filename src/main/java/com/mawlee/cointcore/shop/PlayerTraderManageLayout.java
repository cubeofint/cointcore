package com.mawlee.cointcore.shop;

/**
 * Seller «Мои лоты» geometry. Fits 1080p at GUI scale 3.
 * Widget widths come from measured {@code font.width}.
 */
public final class PlayerTraderManageLayout {
    public static final int MAX_GUI_WIDTH = 360;
    public static final int MAX_GUI_HEIGHT = 350;
    public static final int SLOT = 18;
    public static final int PAD = 8;
    public static final int GUI_WIDTH = 360;
    public static final int LEFT_WIDTH = 176;
    public static final int RIGHT_X = 176;
    public static final int RIGHT_INNER_X = RIGHT_X + PAD;
    public static final int RIGHT_INNER_W = GUI_WIDTH - RIGHT_INNER_X - PAD;

    public static final int TAB_Y = 4;
    public static final int TAB_H = 16;
    public static final int TITLE_Y = 22;
    public static final int REVENUE_Y = 32;
    public static final int SECTION_LABEL_Y = 44;

    public static final int LIST_PAGE_SIZE = 4;
    public static final int LIST_ROW_H = 18;
    public static final int LIST_Y = SECTION_LABEL_Y + 10;
    public static final int LIST_INNER_H = LIST_PAGE_SIZE * LIST_ROW_H;
    public static final int LIST_PAGER_H = 14;
    public static final int LIST_PAGER_Y = LIST_Y + LIST_INNER_H;
    public static final int LIST_BOTTOM = LIST_PAGER_Y + LIST_PAGER_H;

    public static final int EDITOR_Y = LIST_Y;
    public static final int GHOST_X = RIGHT_INNER_X;
    public static final int GHOST_Y = EDITOR_Y;
    public static final int SAMPLE_LABEL_X = GHOST_X + SLOT + 4;
    public static final int FIELD_START_Y = EDITOR_Y + SLOT + 4;
    public static final int LABEL_H = 10;
    public static final int FIELD_H = 12;
    public static final int FIELD_STRIDE = LABEL_H + FIELD_H + 2;
    public static final int FIELD_COUNT = 3;
    public static final int BUTTON_Y = FIELD_START_Y + FIELD_COUNT * FIELD_STRIDE;
    public static final int BUTTON_H = 16;
    public static final int BUTTON_TEXT_PAD = 8;
    public static final int BUTTON_GAP = 4;
    public static final int BUTTON_ROW_GAP = 2;

    public static final int PLAYER_INV_LABEL_Y = Math.max(LIST_BOTTOM, BUTTON_Y + BUTTON_H * 2 + BUTTON_ROW_GAP) + 4;
    public static final int PLAYER_INV_Y = PLAYER_INV_LABEL_Y + 11;
    public static final int PLAYER_HOTBAR_Y = PLAYER_INV_Y + 58;
    public static final int GUI_HEIGHT = Math.min(MAX_GUI_HEIGHT, PLAYER_HOTBAR_Y + SLOT + PAD);

    public static final int HIGHLIGHT = 0x68FFFF00;
    public static final int PAGER_W = 14;

    private PlayerTraderManageLayout() {
    }

    public static int playerSlotX(int col) {
        return PAD + col * SLOT;
    }

    public static int playerInvRowY(int row) {
        return PLAYER_INV_Y + row * SLOT;
    }

    public static int listRowX() {
        return PAD;
    }

    public static int listRowY(int row) {
        return LIST_Y + row * LIST_ROW_H;
    }

    public static int listRowW() {
        return LEFT_WIDTH - PAD * 2;
    }

    public static Rect pagerPrev() {
        return new Rect(PAD, LIST_PAGER_Y, PAGER_W, LIST_PAGER_H);
    }

    public static Rect pagerNext() {
        return new Rect(LEFT_WIDTH - PAD - PAGER_W, LIST_PAGER_Y, PAGER_W, LIST_PAGER_H);
    }

    public static int fieldLabelY(int index) {
        return FIELD_START_Y + index * FIELD_STRIDE;
    }

    public static Rect fieldBox(int index) {
        return new Rect(RIGHT_INNER_X, fieldLabelY(index) + LABEL_H, RIGHT_INNER_W, FIELD_H);
    }

    public static int tabWidth(int textWidth) {
        return Math.max(54, textWidth + 16);
    }

    public static int actionButtonWidth(int textWidth) {
        return Math.max(32, textWidth + BUTTON_TEXT_PAD);
    }

    public static Rect[] actionButtons(int... textWidths) {
        Rect[] rects = new Rect[textWidths.length];
        int x = RIGHT_INNER_X;
        int y = BUTTON_Y;
        int rowRight = RIGHT_INNER_X + RIGHT_INNER_W;
        for (int index = 0; index < textWidths.length; index++) {
            int width = actionButtonWidth(textWidths[index]);
            if (index > 0 && x + width > rowRight) {
                x = RIGHT_INNER_X;
                y += BUTTON_H + BUTTON_ROW_GAP;
            }
            rects[index] = new Rect(x, y, width, BUTTON_H);
            x += width + BUTTON_GAP;
        }
        return rects;
    }

    public static int maxOfferPage(int offerCount) {
        if (offerCount <= LIST_PAGE_SIZE) {
            return 0;
        }
        return (offerCount - 1) / LIST_PAGE_SIZE;
    }

    public record Rect(int x, int y, int w, int h) {
        public int right() {
            return x + w;
        }

        public int bottom() {
            return y + h;
        }

        public boolean contains(double px, double py) {
            return px >= x && px < right() && py >= y && py < bottom();
        }
    }
}
