package com.mawlee.cointcore.shop;

/**
 * Server-side rules for who may manage a player vending machine.
 * Never derived from client-sent owner or tab flags.
 */
public final class PlayerShopManagementPolicy {
    private PlayerShopManagementPolicy() {
    }

    /**
     * Owner UUID match or {@code cointcore.playershop.admin}. Missing identities never grant access.
     */
    public static boolean canManage(String actorId, String ownerId, boolean admin) {
        if (actorId == null || actorId.isBlank()) {
            return false;
        }
        if (ownerId != null && !ownerId.isBlank() && actorId.equals(ownerId)) {
            return true;
        }
        return admin;
    }

    /**
     * The manage menu / tab opens only when the server already decided the actor may manage.
     * A client {@code manage=true} flag is ignored otherwise.
     */
    public static boolean allowOpenManage(boolean requestedManage, boolean canManage) {
        return requestedManage && canManage;
    }

    /**
     * Seller of a created or edited listing is always the acting player, never the block owner
     * inferred from the client or copied blindly when a non-owner somehow reaches this path.
     */
    public static String sellerId(String actorId) {
        if (actorId == null || actorId.isBlank()) {
            return null;
        }
        return actorId;
    }
}
