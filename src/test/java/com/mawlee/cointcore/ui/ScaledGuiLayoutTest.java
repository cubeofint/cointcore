package com.mawlee.cointcore.ui;

import com.mawlee.cointcore.invsee.InvSeeScaleLayout;
import com.mawlee.cointcore.shop.PlayerTraderManageLayout;
import com.mawlee.cointcore.shop.TraderShopLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ScaledGuiLayoutTest {
    @Test
    void marketFitsScaleFourAndScaleThree() {
        assertFits(TraderShopLayout.market(ScaledGuiLayout.SCALE4_1080P_W, ScaledGuiLayout.SCALE4_1080P_H, true));
        assertFits(TraderShopLayout.market(ScaledGuiLayout.SCALE3_1080P_W, ScaledGuiLayout.SCALE3_1080P_H, true));
        assertTrue(TraderShopLayout.market(ScaledGuiLayout.SCALE4_1080P_W, ScaledGuiLayout.SCALE4_1080P_H, true).pageSize() >= 1);
        assertTrue(TraderShopLayout.market(ScaledGuiLayout.SCALE4_1080P_W, ScaledGuiLayout.SCALE4_1080P_H, true).pageSize()
                <= TraderShopLayout.market(ScaledGuiLayout.SCALE3_1080P_W, ScaledGuiLayout.SCALE3_1080P_H, true).pageSize());
    }

    @Test
    void terminalFitsScaleFourAndScaleThree() {
        assertFits(TraderShopLayout.terminal(ScaledGuiLayout.SCALE4_1080P_W, ScaledGuiLayout.SCALE4_1080P_H));
        assertFits(TraderShopLayout.terminal(ScaledGuiLayout.SCALE3_1080P_W, ScaledGuiLayout.SCALE3_1080P_H));
    }

    @Test
    void manageFitsScaleFourAndScaleThree() {
        PlayerTraderManageLayout.Geom small = PlayerTraderManageLayout.fit(
                ScaledGuiLayout.SCALE4_1080P_W,
                ScaledGuiLayout.SCALE4_1080P_H
        );
        PlayerTraderManageLayout.Geom mid = PlayerTraderManageLayout.fit(
                ScaledGuiLayout.SCALE3_1080P_W,
                ScaledGuiLayout.SCALE3_1080P_H
        );
        assertTrue(small.insideScreen(ScaledGuiLayout.SCALE4_1080P_W, ScaledGuiLayout.SCALE4_1080P_H));
        assertTrue(mid.insideScreen(ScaledGuiLayout.SCALE3_1080P_W, ScaledGuiLayout.SCALE3_1080P_H));
        assertTrue(small.listPageSize >= 1);
        assertTrue(small.playerHotbarY + PlayerTraderManageLayout.SLOT <= small.guiHeight);
        assertTrue(small.listBottom <= small.playerInvLabelY);
    }

    @Test
    void invSeeChromeStaysOnScaleFour() {
        InvSeeScaleLayout.Fit player = InvSeeScaleLayout.fit(
                ScaledGuiLayout.SCALE4_1080P_W,
                ScaledGuiLayout.SCALE4_1080P_H,
                176,
                266
        );
        assertTrue(player.panelInside(ScaledGuiLayout.SCALE4_1080P_W, ScaledGuiLayout.SCALE4_1080P_H));
        assertTrue(player.chromeInside(ScaledGuiLayout.SCALE4_1080P_W, ScaledGuiLayout.SCALE4_1080P_H));
        assertTrue(player.imageHeight() <= ScaledGuiLayout.SCALE4_1080P_H - ScaledGuiLayout.MARGIN);

        InvSeeScaleLayout.Fit wide = InvSeeScaleLayout.fit(
                ScaledGuiLayout.SCALE3_1080P_W,
                ScaledGuiLayout.SCALE3_1080P_H,
                176,
                266
        );
        assertTrue(wide.chromeInside(ScaledGuiLayout.SCALE3_1080P_W, ScaledGuiLayout.SCALE3_1080P_H));
        assertTrue(wide.tabsAbove());
    }

    private static void assertFits(TraderShopLayout layout) {
        assertTrue(layout.guiWidth() >= TraderShopLayout.MIN_WIDTH);
        assertTrue(layout.pageSize() >= 1);
        assertTrue(layout.playerInvY() + ScaledGuiLayout.PLAYER_INV_HEIGHT <= layout.guiHeight());
        assertTrue(layout.offerPanelHeight() <= layout.playerInvY());
    }
}
