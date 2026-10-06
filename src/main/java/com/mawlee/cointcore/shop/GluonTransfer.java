package com.mawlee.cointcore.shop;

import java.util.UUID;

/**
 * Pure rules for player-to-player gluon transfers. Wallet persistence lives in {@link GluonWalletSavedData}.
 */
public final class GluonTransfer {
    private GluonTransfer() {
    }

    public static boolean canTransfer(UUID fromId, UUID toId, long amount) {
        if (fromId == null || toId == null) {
            return false;
        }
        if (fromId.equals(toId)) {
            return false;
        }
        return amount > 0L;
    }
}
