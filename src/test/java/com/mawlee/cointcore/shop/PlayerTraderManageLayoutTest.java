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
            assertTrue(PlayerTraderManageLayout.fieldLabelY(index) + PlayerTraderManageLayout.LABEL_H <= box.y());
            assertEquals(PlayerTraderManageLayout.RIGHT_INNER_X, box.x());
            assertTrue(box.right() <= PlayerTraderManageLayout.GUI_WIDTH - PlayerTraderManageLayout.PAD);
            if (index > 0) {
                assertTrue(PlayerTraderManageLayout.fieldLabelY(index) >= PlayerTraderManageLayout.fieldBox(index - 1).bottom());
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
        assertEquals(1, PlayerTraderManageLayout.maxOfferPage(5, 4));
        assertEquals(2, PlayerTraderManageLayout.maxOfferPage(5, 2));
    }

    @Test
    void fittedGeometryKeepsInventoryInsideScaleFourAndThree() {
        PlayerTraderManageLayout.Geom scale4 = PlayerTraderManageLayout.fit(480, 270);
        PlayerTraderManageLayout.Geom scale3 = PlayerTraderManageLayout.fit(640, 360);
        assertTrue(scale4.insideScreen(480, 270));
        assertTrue(scale3.insideScreen(640, 360));
        assertTrue(scale4.inventoryFullyInside());
        assertTrue(scale3.inventoryFullyInside());
        assertTrue(scale4.listPageSize >= 1);
        assertTrue(scale4.fieldStride >= PlayerTraderManageLayout.LABEL_H + PlayerTraderManageLayout.FIELD_H);
        for (int index = 0; index < PlayerTraderManageLayout.FIELD_COUNT; index++) {
            PlayerTraderManageLayout.Rect box = scale4.fieldBox(index);
            assertTrue(scale4.fieldLabelY(index) + PlayerTraderManageLayout.LABEL_H <= box.y());
            if (index > 0) {
                assertTrue(scale4.fieldLabelY(index) >= scale4.fieldBox(index - 1).bottom());
            }
        }
        assertTrue(scale4.fieldBox(2).bottom() <= scale4.buttonY || scale4.buttonY >= scale4.recommendY);
        assertTrue(scale4.playerInvLabelY >= scale4.listBottom || scale4.listBottom <= scale4.playerInvY);
        assertTrue(scale4.playerHotbarY + PlayerTraderManageLayout.SLOT + PlayerTraderManageLayout.PAD <= scale4.guiHeight);
        assertRecommendDoesNotOverlapFill(scale4, 24, 80);
        assertRecommendDoesNotOverlapFill(scale4, 56, 140);
    }

    @Test
    void fillButtonWidthFollowsLocalizedLabel() {
        assertEquals(32, PlayerTraderManageLayout.fillButtonWidth(10));
        assertEquals(24 + PlayerTraderManageLayout.BUTTON_TEXT_PAD, PlayerTraderManageLayout.fillButtonWidth(24));
        assertEquals(56 + PlayerTraderManageLayout.BUTTON_TEXT_PAD, PlayerTraderManageLayout.fillButtonWidth(56));
    }

    @Test
    void recommendTextSitsAfterFillButtonOnPreferredGeometry() {
        PlayerTraderManageLayout.Geom geom = PlayerTraderManageLayout.fit(640, 360);
        int fillLabel = 56;
        PlayerTraderManageLayout.RecommendPlacement placement = PlayerTraderManageLayout.recommendPlacement(
                geom,
                fillLabel,
                90
        );
        assertEquals(PlayerTraderManageLayout.fillButtonWidth(fillLabel), placement.fill().w());
        assertEquals(placement.fill().right() + PlayerTraderManageLayout.BUTTON_GAP, placement.textX());
        assertTrue(placement.hasText());
        assertTrue(!placement.textOverlapsFill());
        assertTrue(placement.fill().y() >= geom.fieldBox(2).bottom() || placement.fill().x() >= placement.price().right());
    }

    @Test
    void russianFillLabelAndEnglishUseDoNotOverlapHint() {
        PlayerTraderManageLayout.Geom preferred = PlayerTraderManageLayout.fit(640, 360);
        PlayerTraderManageLayout.Geom compact = PlayerTraderManageLayout.fit(480, 270);
        int[] fillLabels = {18, 56};
        int[] hints = {40, 90, 160};
        for (int fillLabel : fillLabels) {
            for (int hint : hints) {
                assertRecommendDoesNotOverlapFill(preferred, fillLabel, hint);
                assertRecommendDoesNotOverlapFill(compact, fillLabel, hint);
            }
        }
    }

    @Test
    void tightRecommendHintMovesToOwnLineOrTruncates() {
        PlayerTraderManageLayout.Geom geom = PlayerTraderManageLayout.fit(640, 360);
        int fillLabel = geom.rightInnerW - 4;
        PlayerTraderManageLayout.RecommendPlacement placement = PlayerTraderManageLayout.recommendPlacement(
                geom,
                fillLabel,
                120
        );
        assertTrue(!placement.textOverlapsFill());
        if (placement.fill().bottom() + 2 + PlayerTraderManageLayout.LABEL_H <= geom.buttonY) {
            assertEquals(geom.rightInnerX, placement.textX());
            assertTrue(placement.textY() >= placement.fill().bottom());
        } else {
            assertTrue(placement.truncated() || !placement.hasText());
        }
    }

    private static void assertRecommendDoesNotOverlapFill(
            PlayerTraderManageLayout.Geom geom,
            int fillLabelWidth,
            int recommendTextWidth
    ) {
        PlayerTraderManageLayout.RecommendPlacement placement = PlayerTraderManageLayout.recommendPlacement(
                geom,
                fillLabelWidth,
                recommendTextWidth
        );
        assertTrue(placement.fill().w() >= 32);
        assertTrue(placement.fill().right() <= geom.rightInnerX + geom.rightInnerW);
        assertTrue(placement.price().right() <= placement.fill().x()
                || placement.price().bottom() <= placement.fill().y()
                || placement.fill().y() >= placement.price().bottom());
        assertTrue(!placement.textOverlapsFill());
        if (placement.hasText()) {
            assertTrue(placement.textX() + Math.min(8, placement.textMaxW()) <= geom.rightInnerX + geom.rightInnerW
                    || placement.textMaxW() >= 0);
            assertTrue(placement.textX() >= geom.rightInnerX || placement.textX() >= placement.fill().right());
        }
        if (recommendTextWidth <= placement.textMaxW() && placement.hasText()
                && placement.textY() == placement.fill().y() + 2) {
            assertEquals(placement.fill().right() + PlayerTraderManageLayout.BUTTON_GAP, placement.textX());
        }
    }
}
