package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopBreakPolicyTest {
    @Test
    void npcTraderOnlyAdmins() {
        assertFalse(ShopBreakPolicy.allowNpcTraderBreak(false));
        assertTrue(ShopBreakPolicy.allowNpcTraderBreak(true));
    }

    @Test
    void playerTraderFollowsManageAccess() {
        assertTrue(ShopBreakPolicy.allowPlayerTraderBreak(true));
        assertFalse(ShopBreakPolicy.allowPlayerTraderBreak(false));
    }

    @Test
    void clientOperatorMatchesVanillaOpLevel() {
        assertFalse(ShopBreakPolicy.clientOperator(false));
        assertTrue(ShopBreakPolicy.clientOperator(true));
    }
}
