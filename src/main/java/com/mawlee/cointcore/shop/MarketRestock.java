package com.mawlee.cointcore.shop;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Pure restock arithmetic: template match, stock-cap room, simulate vs commit.
 */
public final class MarketRestock {
    private MarketRestock() {
    }

    public record LotView(
            String id,
            String sellerId,
            String itemId,
            String components,
            long stockItems,
            long createdAt,
            long expiresAt
    ) {
        public LotView {
            id = id == null ? "" : id;
            sellerId = sellerId == null ? "" : sellerId;
            itemId = itemId == null ? "" : itemId;
            components = components == null ? "" : components;
            stockItems = Math.max(0L, stockItems);
            createdAt = Math.max(0L, createdAt);
            expiresAt = Math.max(0L, expiresAt);
        }
    }

    public record Decision(String lotId, int accepted, String reason) {
        public Decision {
            lotId = lotId == null ? "" : lotId;
            accepted = Math.max(0, accepted);
            reason = reason == null ? "" : reason;
        }

        public boolean ok() {
            return accepted > 0 && !lotId.isBlank();
        }

        public static Decision reject(String reason) {
            return new Decision("", 0, reason);
        }

        public static Decision accept(String lotId, int accepted) {
            return new Decision(lotId, accepted, "ok");
        }
    }

    public static int acceptCount(long stockItems, long maxStock, int incoming) {
        if (incoming <= 0 || maxStock <= 0L || stockItems >= maxStock) {
            return 0;
        }
        long room = maxStock - Math.max(0L, stockItems);
        return (int) Math.min(incoming, Math.min(Integer.MAX_VALUE, room));
    }

    /**
     * Same item and the same data-component patch. Count is ignored so a 64-stack
     * restocks a 1-per-deal lot.
     */
    public static boolean matchesTemplate(ItemStack template, ItemStack incoming) {
        if (template == null || template.isEmpty() || incoming == null || incoming.isEmpty()) {
            return false;
        }
        return ItemStack.isSameItemSameComponents(template, incoming);
    }

    public static boolean keysMatch(
            String templateItem,
            String templateComponents,
            String incomingItem,
            String incomingComponents
    ) {
        if (templateItem == null || templateItem.isBlank() || incomingItem == null || incomingItem.isBlank()) {
            return false;
        }
        if (!templateItem.equals(incomingItem)) {
            return false;
        }
        String left = templateComponents == null ? "" : templateComponents;
        String right = incomingComponents == null ? "" : incomingComponents;
        return left.equals(right);
    }

    public static Decision plan(
            List<LotView> lots,
            String ownerId,
            String itemId,
            String components,
            int incoming,
            long now,
            long cap,
            boolean blacklisted
    ) {
        if (ownerId == null || ownerId.isBlank()) {
            return Decision.reject("no_owner");
        }
        if (incoming <= 0 || itemId == null || itemId.isBlank()) {
            return Decision.reject("empty");
        }
        if (blacklisted) {
            return Decision.reject("blacklist");
        }
        if (lots == null || lots.isEmpty()) {
            return Decision.reject("no_lots");
        }
        LotView target = null;
        boolean matchedItem = false;
        for (LotView lot : lots) {
            if (lot == null || !ownerId.equals(lot.sellerId())) {
                continue;
            }
            if (GlobalMarketMath.expired(now, lot.expiresAt())) {
                continue;
            }
            if (!keysMatch(lot.itemId(), lot.components(), itemId, components)) {
                continue;
            }
            matchedItem = true;
            if (acceptCount(lot.stockItems(), cap, incoming) < 1) {
                continue;
            }
            if (target == null || lot.createdAt() < target.createdAt()) {
                target = lot;
            }
        }
        if (target == null) {
            return Decision.reject(matchedItem ? "cap" : "no_match");
        }
        int accepted = acceptCount(target.stockItems(), cap, incoming);
        if (accepted < 1) {
            return Decision.reject("cap");
        }
        return Decision.accept(target.id(), accepted);
    }

    /**
     * Simulate leaves stock unchanged. A real commit adds {@code accepted} items.
     */
    public static long commitStock(long current, int accepted, boolean simulate) {
        if (simulate || accepted <= 0) {
            return Math.max(0L, current);
        }
        return GlobalMarketMath.saturatingAdd(Math.max(0L, current), accepted);
    }
}
