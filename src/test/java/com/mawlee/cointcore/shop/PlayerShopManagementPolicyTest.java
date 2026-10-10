package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerShopManagementPolicyTest {
    private static final String OWNER = "11111111-1111-1111-1111-111111111111";
    private static final String OTHER = "22222222-2222-2222-2222-222222222222";

    @Test
    void ownerCanManageWithoutAdmin() {
        assertTrue(PlayerShopManagementPolicy.canManage(OWNER, OWNER, false));
    }

    @Test
    void strangerCannotManageWithoutAdmin() {
        assertFalse(PlayerShopManagementPolicy.canManage(OTHER, OWNER, false));
    }

    @Test
    void adminCanManageForeignShop() {
        assertTrue(PlayerShopManagementPolicy.canManage(OTHER, OWNER, true));
    }

    @Test
    void missingIdentitiesNeverManage() {
        assertFalse(PlayerShopManagementPolicy.canManage(null, OWNER, false));
        assertFalse(PlayerShopManagementPolicy.canManage("", OWNER, true));
        assertFalse(PlayerShopManagementPolicy.canManage(OTHER, null, false));
        assertFalse(PlayerShopManagementPolicy.canManage(OTHER, "", false));
    }

    @Test
    void clientManageTabIsIgnoredForStrangers() {
        assertFalse(PlayerShopManagementPolicy.allowOpenManage(true, false));
        assertFalse(PlayerShopManagementPolicy.allowOpenManage(true, PlayerShopManagementPolicy.canManage(OTHER, OWNER, false)));
        assertTrue(PlayerShopManagementPolicy.allowOpenManage(true, PlayerShopManagementPolicy.canManage(OWNER, OWNER, false)));
        assertFalse(PlayerShopManagementPolicy.allowOpenManage(false, true));
    }

    @Test
    void sellerUuidIsAlwaysTheActingPlayer() {
        assertEquals(OTHER, PlayerShopManagementPolicy.sellerId(OTHER));
        assertEquals(OWNER, PlayerShopManagementPolicy.sellerId(OWNER));
        assertNull(PlayerShopManagementPolicy.sellerId(null));
        assertNull(PlayerShopManagementPolicy.sellerId(""));
        assertFalse(OWNER.equals(PlayerShopManagementPolicy.sellerId(OTHER)));
    }
}
