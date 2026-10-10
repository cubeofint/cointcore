package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.ui.ScaledGuiLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerTraderShopLayoutTest {
    @Test
    void marketWindowFitsScaleThreeOn1080p() {
        assertTrue(PlayerTraderMenu.GUI_WIDTH <= 360);
        assertTrue(PlayerTraderMenu.GUI_HEIGHT <= 350);
    }

    @Test
    void computedMarketFitsScaledScreens() {
        TraderShopLayout tiny = TraderShopLayout.market(
                ScaledGuiLayout.SCALE4_1080P_W,
                ScaledGuiLayout.SCALE4_1080P_H,
                true
        );
        TraderShopLayout mid = TraderShopLayout.market(
                ScaledGuiLayout.SCALE3_1080P_W,
                ScaledGuiLayout.SCALE3_1080P_H,
                true
        );
        assertTrue(tiny.insideScreen(ScaledGuiLayout.SCALE4_1080P_W, ScaledGuiLayout.SCALE4_1080P_H));
        assertTrue(mid.insideScreen(ScaledGuiLayout.SCALE3_1080P_W, ScaledGuiLayout.SCALE3_1080P_H));
        assertTrue(tiny.searchY() > tiny.titleY());
        assertTrue(tiny.searchY() + 12 <= tiny.titleHeight());
    }
}
