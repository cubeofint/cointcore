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
    void onlyGhostSlotLivesOnTheEditor() {
        assertTrue(PlayerTraderManageLayout.GHOST_X >= PlayerTraderManageLayout.RIGHT_X);
        assertTrue(PlayerTraderManageLayout.GHOST_Y >= PlayerTraderManageLayout.EDITOR_Y);
        assertTrue(PlayerTraderManageLayout.GHOST_X + PlayerTraderManageLayout.SLOT
                <= PlayerTraderManageLayout.GUI_WIDTH - PlayerTraderManageLayout.PAD);
    }

    @Test
    void labelsSitAbovePlayerInventory() {
        assertTrue(PlayerTraderManageLayout.PLAYER_INV_LABEL_Y + 8 <= PlayerTraderManageLayout.PLAYER_INV_Y);
        assertTrue(PlayerTraderManageLayout.PLAYER_INV_LABEL_Y >= PlayerTraderManageLayout.LIST_BOTTOM);
    }

    @Test
    void listPagerStaysInTheListColumn() {
        PlayerTraderManageLayout.Rect prev = PlayerTraderManageLayout.pagerPrev();
        PlayerTraderManageLayout.Rect next = PlayerTraderManageLayout.pagerNext();
        assertTrue(prev.x() >= 0);
        assertTrue(next.right() <= PlayerTraderManageLayout.LEFT_WIDTH);
        assertTrue(prev.bottom() <= PlayerTraderManageLayout.PLAYER_INV_LABEL_Y);
        assertTrue(next.y() >= PlayerTraderManageLayout.listRowY(PlayerTraderManageLayout.LIST_PAGE_SIZE - 1)
                + PlayerTraderManageLayout.LIST_ROW_H);
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
        PlayerTraderManageLayout.Rect[] buttons = PlayerTraderManageLayout.actionButtons(48, 52, 70);
        for (PlayerTraderManageLayout.Rect button : buttons) {
            assertTrue(button.x() >= PlayerTraderManageLayout.RIGHT_INNER_X);
            assertTrue(button.right() <= PlayerTraderManageLayout.RIGHT_INNER_X + PlayerTraderManageLayout.RIGHT_INNER_W);
            assertTrue(button.bottom() <= PlayerTraderManageLayout.PLAYER_INV_LABEL_Y);
        }
    }

    @Test
    void offerPageCountMatchesPageSize() {
        assertEquals(0, PlayerTraderManageLayout.maxOfferPage(0));
        assertEquals(0, PlayerTraderManageLayout.maxOfferPage(4));
        assertEquals(1, PlayerTraderManageLayout.maxOfferPage(5));
    }
}
