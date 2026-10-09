package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerTraderManageLayoutTest {
    @Test
    void fitsScaleThreeOn1080p() {
        assertTrue(PlayerTraderManageLayout.GUI_WIDTH <= PlayerTraderManageLayout.MAX_GUI_WIDTH);
        assertTrue(PlayerTraderManageLayout.GUI_HEIGHT <= PlayerTraderManageLayout.MAX_GUI_HEIGHT);
    }

    @Test
    void stockGridStaysInLeftColumn() {
        int lastX = PlayerTraderManageLayout.stockSlotX(8) + PlayerTraderManageLayout.SLOT;
        int lastY = PlayerTraderManageLayout.stockSlotY(2) + PlayerTraderManageLayout.SLOT;
        assertTrue(lastX <= PlayerTraderManageLayout.RIGHT_X);
        assertTrue(lastY <= PlayerTraderManageLayout.PLAYER_INV_LABEL_Y);
    }

    @Test
    void labelsSitAboveTheirGrids() {
        assertTrue(PlayerTraderManageLayout.SECTION_LABEL_Y + 8 <= PlayerTraderManageLayout.STOCK_Y);
        assertTrue(PlayerTraderManageLayout.PLAYER_INV_LABEL_Y + 8 <= PlayerTraderManageLayout.PLAYER_INV_Y);
        assertTrue(PlayerTraderManageLayout.PLAYER_INV_LABEL_Y >= PlayerTraderManageLayout.stockSlotY(2) + PlayerTraderManageLayout.SLOT);
    }

    @Test
    void listPagerStaysInsideTheListColumn() {
        PlayerTraderManageLayout.Rect prev = PlayerTraderManageLayout.pagerPrev();
        PlayerTraderManageLayout.Rect next = PlayerTraderManageLayout.pagerNext();
        assertTrue(prev.x() >= PlayerTraderManageLayout.RIGHT_X);
        assertTrue(next.right() <= PlayerTraderManageLayout.GUI_WIDTH - 4);
        assertTrue(prev.bottom() <= PlayerTraderManageLayout.EDITOR_Y);
        assertTrue(next.y() >= PlayerTraderManageLayout.listRowY(PlayerTraderManageLayout.LIST_PAGE_SIZE - 1)
                + PlayerTraderManageLayout.LIST_ROW_H);
        int stockRight = PlayerTraderManageLayout.stockSlotX(8) + PlayerTraderManageLayout.SLOT;
        assertTrue(prev.x() >= stockRight);
    }

    @Test
    void ghostSlotIsOnTheEditorSide() {
        assertTrue(PlayerTraderManageLayout.GHOST_X >= PlayerTraderManageLayout.RIGHT_X);
        assertTrue(PlayerTraderManageLayout.GHOST_Y >= PlayerTraderManageLayout.LIST_BOTTOM);
        assertTrue(PlayerTraderManageLayout.GHOST_X + PlayerTraderManageLayout.SLOT
                <= PlayerTraderManageLayout.GUI_WIDTH - PlayerTraderManageLayout.PAD);
    }

    @Test
    void fieldsStackLabelThenBoxWithoutOverflow() {
        for (int index = 0; index < PlayerTraderManageLayout.FIELD_COUNT; index++) {
            PlayerTraderManageLayout.Rect box = PlayerTraderManageLayout.fieldBox(index);
            assertTrue(PlayerTraderManageLayout.fieldLabelY(index) + 8 <= box.y());
            assertEquals(PlayerTraderManageLayout.RIGHT_INNER_X, box.x());
            assertTrue(box.right() <= PlayerTraderManageLayout.GUI_WIDTH - PlayerTraderManageLayout.PAD);
            if (index > 0) {
                assertTrue(box.y() >= PlayerTraderManageLayout.fieldBox(index - 1).bottom());
            }
        }
        assertTrue(PlayerTraderManageLayout.fieldBox(2).bottom() <= PlayerTraderManageLayout.BUTTON_Y);
    }

    @Test
    void russianActionButtonsFitWithoutTruncationWidths() {
        // Vanilla-font-ish widths for «Новый», «Сохранить», «Удалить».
        PlayerTraderManageLayout.Rect[] buttons = PlayerTraderManageLayout.actionButtons(36, 60, 48);
        int bottom = 0;
        for (PlayerTraderManageLayout.Rect button : buttons) {
            assertTrue(button.x() >= PlayerTraderManageLayout.RIGHT_INNER_X);
            assertTrue(button.right() <= PlayerTraderManageLayout.RIGHT_INNER_X + PlayerTraderManageLayout.RIGHT_INNER_W);
            assertTrue(button.y() >= PlayerTraderManageLayout.BUTTON_Y);
            assertTrue(button.bottom() <= PlayerTraderManageLayout.GUI_HEIGHT - 4);
            bottom = Math.max(bottom, button.bottom());
        }
        assertTrue(buttons[0].w() >= 36 + PlayerTraderManageLayout.BUTTON_TEXT_PAD);
        assertTrue(bottom <= PlayerTraderManageLayout.GUI_HEIGHT);
    }

    @Test
    void englishActionButtonsStayOnOneRow() {
        PlayerTraderManageLayout.Rect[] buttons = PlayerTraderManageLayout.actionButtons(22, 26, 36);
        assertEquals(PlayerTraderManageLayout.BUTTON_Y, buttons[0].y());
        assertEquals(PlayerTraderManageLayout.BUTTON_Y, buttons[2].y());
        assertTrue(buttons[2].x() > buttons[1].right());
    }

    @Test
    void offerPageCountMatchesPageSize() {
        assertEquals(0, PlayerTraderManageLayout.maxOfferPage(0));
        assertEquals(0, PlayerTraderManageLayout.maxOfferPage(3));
        assertEquals(1, PlayerTraderManageLayout.maxOfferPage(4));
        assertEquals(10, PlayerTraderManageLayout.maxOfferPage(32));
    }
}
