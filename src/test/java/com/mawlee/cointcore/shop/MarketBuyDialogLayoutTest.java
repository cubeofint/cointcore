package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.ui.ScaledGuiLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketBuyDialogLayoutTest {
    @Test
    void scaleFourLayoutDoesNotOverlapAndFitsQuantity() {
        TraderShopLayout market = TraderShopLayout.market(
                ScaledGuiLayout.SCALE4_1080P_W,
                ScaledGuiLayout.SCALE4_1080P_H,
                true
        );
        MarketBuyDialogLayout.Geom geom = MarketBuyDialogLayout.of(
                10,
                10,
                market.guiWidth(),
                market.guiHeight(),
                market.listTop(),
                42,
                50,
                50
        );
        assertTrue(geom.qtyW() >= 64);
        assertTrue(geom.panelH() >= 80);
        assertTrue(geom.disjoint(geom.qtyX(), geom.qtyY(), geom.qtyW(), geom.qtyH(), geom.oneX(), geom.quickY(), geom.quickW(), geom.quickH()));
        assertTrue(geom.disjoint(geom.oneX(), geom.quickY(), geom.quickW(), geom.quickH(), geom.tenX(), geom.quickY(), geom.quickW(), geom.quickH()));
        assertTrue(geom.disjoint(geom.tenX(), geom.quickY(), geom.quickW(), geom.quickH(), geom.maxX(), geom.quickY(), geom.quickW(), geom.quickH()));
        assertTrue(geom.quoteY() >= geom.qtyY() + geom.qtyH());
        assertTrue(geom.actionY() >= geom.quoteY() + 8);
        assertTrue(geom.buyX() + geom.actionW() <= geom.cancelX());
        assertTrue(geom.panelX() + geom.panelW() <= 10 + market.guiWidth());
        assertTrue(geom.panelY() + geom.panelH() <= 10 + market.guiHeight());
    }
}
