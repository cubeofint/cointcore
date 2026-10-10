package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.ui.ScaledGuiLayout;

/**
 * Seller «Мои лоты» geometry. Preferred size fits 1080p at GUI scale 3;
 * {@link #fit(int, int)} clamps to the scaled screen and drops list rows.
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
        return maxOfferPage(offerCount, LIST_PAGE_SIZE);
    }

    public static int maxOfferPage(int offerCount, int pageSize) {
        int size = Math.max(1, pageSize);
        if (offerCount <= size) {
            return 0;
        }
        return (offerCount - 1) / size;
    }

    /**
     * Geometry for the current scaled screen. Drops list rows until the window fits.
     */
    public static Geom fit(int screenW, int screenH) {
        int width = ScaledGuiLayout.clampSize(GUI_WIDTH, screenW, 220, ScaledGuiLayout.MARGIN);
        for (boolean compact : new boolean[] {false, true}) {
            for (int rows = LIST_PAGE_SIZE; rows >= 1; rows--) {
                Geom geom = Geom.of(width, rows, compact);
                if (geom.insideScreen(screenW, screenH)) {
                    return geom;
                }
            }
        }
        Geom compact = Geom.of(width, 1, true);
        int maxH = Math.max(160, screenH - ScaledGuiLayout.MARGIN * 2);
        if (compact.guiHeight <= maxH) {
            return compact;
        }
        return compact.withHeight(maxH);
    }

    public static final class Geom {
        public final int guiWidth;
        public final int guiHeight;
        public final int leftWidth;
        public final int rightX;
        public final int rightInnerX;
        public final int rightInnerW;
        public final int tabY;
        public final int tabH;
        public final int titleY;
        public final int revenueY;
        public final int sectionLabelY;
        public final int listPageSize;
        public final int listRowH;
        public final int listY;
        public final int listPagerY;
        public final int listBottom;
        public final int editorY;
        public final int ghostX;
        public final int ghostY;
        public final int sampleLabelX;
        public final int fieldStartY;
        public final int fieldStride;
        public final int buttonY;
        public final int recommendY;
        public final int playerInvLabelY;
        public final int playerInvY;
        public final int playerHotbarY;

        private Geom(
                int guiWidth,
                int guiHeight,
                int leftWidth,
                int rightX,
                int rightInnerX,
                int rightInnerW,
                int tabY,
                int tabH,
                int titleY,
                int revenueY,
                int sectionLabelY,
                int listPageSize,
                int listRowH,
                int listY,
                int listPagerY,
                int listBottom,
                int editorY,
                int ghostX,
                int ghostY,
                int sampleLabelX,
                int fieldStartY,
                int fieldStride,
                int buttonY,
                int recommendY,
                int playerInvLabelY,
                int playerInvY,
                int playerHotbarY
        ) {
            this.guiWidth = guiWidth;
            this.guiHeight = guiHeight;
            this.leftWidth = leftWidth;
            this.rightX = rightX;
            this.rightInnerX = rightInnerX;
            this.rightInnerW = rightInnerW;
            this.tabY = tabY;
            this.tabH = tabH;
            this.titleY = titleY;
            this.revenueY = revenueY;
            this.sectionLabelY = sectionLabelY;
            this.listPageSize = listPageSize;
            this.listRowH = listRowH;
            this.listY = listY;
            this.listPagerY = listPagerY;
            this.listBottom = listBottom;
            this.editorY = editorY;
            this.ghostX = ghostX;
            this.ghostY = ghostY;
            this.sampleLabelX = sampleLabelX;
            this.fieldStartY = fieldStartY;
            this.fieldStride = fieldStride;
            this.buttonY = buttonY;
            this.recommendY = recommendY;
            this.playerInvLabelY = playerInvLabelY;
            this.playerInvY = playerInvY;
            this.playerHotbarY = playerHotbarY;
        }

        static Geom of(int width, int rows) {
            return of(width, rows, false);
        }

        static Geom of(int width, int rows, boolean compact) {
            int safeW = Math.max(220, width);
            int left = Math.min(LEFT_WIDTH, Math.max(120, safeW / 2));
            int rightX = left;
            int rightInnerX = rightX + PAD;
            int rightInnerW = Math.max(64, safeW - rightInnerX - PAD);
            int listPageSize = Math.max(1, rows);
            int listY = LIST_Y;
            int listPagerY = listY + listPageSize * LIST_ROW_H;
            int listBottom = listPagerY + LIST_PAGER_H;
            int editorY = LIST_Y;
            int ghostX = rightInnerX;
            int ghostY = editorY;
            int fieldStride = compact ? 20 : FIELD_STRIDE;
            int fieldStartY = editorY + SLOT + (compact ? 2 : 4);
            int lastFieldBottom = fieldStartY + (FIELD_COUNT - 1) * fieldStride + LABEL_H + FIELD_H;
            int recommendY = lastFieldBottom + (compact ? 2 : 4);
            int buttonY = compact ? recommendY : recommendY + 14;
            int editorBottom = buttonY + BUTTON_H * (compact ? 1 : 2) + BUTTON_ROW_GAP;
            int playerInvLabelY = Math.max(listBottom, editorBottom) + (compact ? 2 : 4);
            int playerInvY = playerInvLabelY + (compact ? 8 : 11);
            int playerHotbarY = playerInvY + 58;
            int height = Math.min(MAX_GUI_HEIGHT, playerHotbarY + SLOT + PAD);
            return new Geom(
                    safeW,
                    height,
                    left,
                    rightX,
                    rightInnerX,
                    rightInnerW,
                    TAB_Y,
                    TAB_H,
                    TITLE_Y,
                    REVENUE_Y,
                    SECTION_LABEL_Y,
                    listPageSize,
                    LIST_ROW_H,
                    listY,
                    listPagerY,
                    listBottom,
                    editorY,
                    ghostX,
                    ghostY,
                    ghostX + SLOT + 4,
                    fieldStartY,
                    fieldStride,
                    buttonY,
                    recommendY,
                    playerInvLabelY,
                    playerInvY,
                    playerHotbarY
            );
        }

        Geom withHeight(int height) {
            int clamped = Math.max(playerInvY + ScaledGuiLayout.PLAYER_INV_HEIGHT, height);
            return new Geom(
                    guiWidth,
                    clamped,
                    leftWidth,
                    rightX,
                    rightInnerX,
                    rightInnerW,
                    tabY,
                    tabH,
                    titleY,
                    revenueY,
                    sectionLabelY,
                    listPageSize,
                    listRowH,
                    listY,
                    listPagerY,
                    listBottom,
                    editorY,
                    ghostX,
                    ghostY,
                    sampleLabelX,
                    fieldStartY,
                    fieldStride,
                    buttonY,
                    recommendY,
                    playerInvLabelY,
                    playerInvY,
                    playerHotbarY
            );
        }

        public int listRowX() {
            return PAD;
        }

        public int listRowY(int row) {
            return listY + row * listRowH;
        }

        public int listRowW() {
            return leftWidth - PAD * 2;
        }

        public Rect pagerPrev() {
            return new Rect(PAD, listPagerY, PAGER_W, LIST_PAGER_H);
        }

        public Rect pagerNext() {
            return new Rect(leftWidth - PAD - PAGER_W, listPagerY, PAGER_W, LIST_PAGER_H);
        }

        public int fieldLabelY(int index) {
            return fieldStartY + index * fieldStride;
        }

        public Rect fieldBox(int index) {
            return new Rect(rightInnerX, fieldLabelY(index) + LABEL_H, rightInnerW, FIELD_H);
        }

        public Rect[] actionButtons(int... textWidths) {
            Rect[] rects = new Rect[textWidths.length];
            int x = rightInnerX;
            int y = buttonY;
            int rowRight = rightInnerX + rightInnerW;
            for (int index = 0; index < textWidths.length; index++) {
                int width = actionButtonWidth(textWidths[index]);
                if (index > 0 && x + width > rowRight) {
                    x = rightInnerX;
                    y += BUTTON_H + BUTTON_ROW_GAP;
                }
                rects[index] = new Rect(x, y, width, BUTTON_H);
                x += width + BUTTON_GAP;
            }
            return rects;
        }

        public int playerSlotX(int col) {
            return PAD + col * SLOT;
        }

        public int playerInvRowY(int row) {
            return playerInvY + row * SLOT;
        }

        public boolean insideScreen(int screenW, int screenH) {
            return ScaledGuiLayout.fits(guiWidth, guiHeight, screenW, screenH, ScaledGuiLayout.MARGIN);
        }
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
