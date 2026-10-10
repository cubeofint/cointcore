package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerTraderShopLayoutTest {
    @Test
    void marketWindowFitsScaleThreeOn1080p() {
        assertTrue(PlayerTraderMenu.GUI_WIDTH <= 360);
        assertTrue(PlayerTraderMenu.GUI_HEIGHT <= 350);
    }
}
