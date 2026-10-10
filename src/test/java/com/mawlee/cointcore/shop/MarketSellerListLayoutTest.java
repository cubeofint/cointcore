package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.ui.ScaledGuiLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketSellerListLayoutTest {
    @Test
    void scaleFourSellerListFitsAndKeepsBuyButtonsClear() {
        TraderShopLayout market = TraderShopLayout.market(
                ScaledGuiLayout.SCALE4_1080P_W,
                ScaledGuiLayout.SCALE4_1080P_H,
                true
        );
        MarketSellerListLayout.Geom geom = MarketSellerListLayout.of(
                8,
                6,
                market.guiWidth(),
                market.guiHeight(),
                market.listTop(),
                40,
                80,
                50
        );
        assertTrue(geom.pageSize() >= 2);
        assertTrue(geom.rowH() >= 24);
        assertTrue(geom.textW() >= 80);
        assertTrue(geom.panelX() + geom.panelW() <= 8 + market.guiWidth());
        assertTrue(geom.panelY() + geom.panelH() <= 6 + market.guiHeight());
        assertTrue(geom.disjoint(
                geom.listX(),
                geom.rowY(0),
                geom.textW(),
                geom.rowH(),
                geom.buyX(),
                geom.buyY(0),
                geom.buyW(),
                geom.buyH()
        ));
        assertTrue(geom.disjoint(
                geom.cheapestX(),
                geom.cheapestY(),
                geom.cheapestW(),
                geom.actionH(),
                geom.closeX(),
                geom.closeY(),
                geom.closeW(),
                geom.actionH()
        ));
        assertTrue(geom.cheapestY() >= geom.rowY(geom.pageSize() - 1) + geom.rowH());
    }
}
