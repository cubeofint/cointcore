package com.mawlee.cointcore.shop;

/**
 * Who may finish mining shop blocks. Claim plugins still cancel afterwards.
 */
public final class ShopBreakPolicy {
    private ShopBreakPolicy() {
    }

    public static boolean allowNpcTraderBreak(boolean admin) {
        return admin;
    }

    public static boolean allowPlayerTraderBreak(boolean canManage) {
        return canManage;
    }

    /**
     * Client has no LuckPerms. Treat vanilla permission level 2+ as admin for
     * destroy-progress only; the server re-checks {@code playershop.admin}.
     */
    public static boolean clientOperator(boolean hasOpLevel2) {
        return hasOpLevel2;
    }
}
