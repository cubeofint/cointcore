package com.mawlee.cointcore.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Read-only projection of the existing durable journal, newest transactions first. */
public final class MarketHistory {
    public static final int PAGE_SIZE = 10;

    private MarketHistory() { }

    public static List<CurrencyMovement> page(List<CurrencyMovement> journal, UUID player, int page) {
        if (page < 1) return List.of();
        long skip = (long) (page - 1) * PAGE_SIZE;
        var result = new ArrayList<CurrencyMovement>(PAGE_SIZE);
        for (int i = journal.size() - 1; i >= 0; i--) {
            var row = journal.get(i);
            if (!isTrade(row.type()) || (player != null && !player.equals(row.fromId()) && !player.equals(row.toId()))) continue;
            if (skip > 0) { skip--; continue; }
            result.add(row);
            if (result.size() == PAGE_SIZE) break;
        }
        return List.copyOf(result);
    }

    private static boolean isTrade(CurrencyMovementType type) {
        return switch (type) {
            case TRADER_BUY, TRADER_SELL, PLAYER_SHOP_BUY, PLAYER_SHOP_SELL -> true;
            default -> false;
        };
    }
}
