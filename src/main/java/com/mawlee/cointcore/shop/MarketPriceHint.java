package com.mawlee.cointcore.shop;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;

import java.util.ArrayList;
import java.util.List;

/**
 * Compact item-key → recommended unit price pair for the manage screen.
 */
public record MarketPriceHint(String itemKey, long unitPrice) {
    public MarketPriceHint {
        itemKey = itemKey == null ? "" : itemKey;
        unitPrice = Math.max(0L, unitPrice);
    }

    public static void writeList(RegistryFriendlyByteBuf buffer, List<MarketPriceHint> hints) {
        List<MarketPriceHint> limited = hints == null ? List.of() : hints;
        if (limited.size() > 64) {
            limited = limited.subList(0, 64);
        }
        ByteBufCodecs.VAR_INT.encode(buffer, limited.size());
        for (MarketPriceHint hint : limited) {
            ByteBufCodecs.STRING_UTF8.encode(buffer, hint.itemKey());
            buffer.writeLong(hint.unitPrice());
        }
    }

    public static List<MarketPriceHint> readList(RegistryFriendlyByteBuf buffer) {
        if (buffer.readableBytes() <= 0) {
            return List.of();
        }
        int count = ByteBufCodecs.VAR_INT.decode(buffer);
        List<MarketPriceHint> hints = new ArrayList<>(Math.min(count, 64));
        for (int index = 0; index < count && index < 64; index++) {
            String key = ByteBufCodecs.STRING_UTF8.decode(buffer);
            long price = buffer.readLong();
            if (!key.isBlank() && price > 0L) {
                hints.add(new MarketPriceHint(key, price));
            }
        }
        return List.copyOf(hints);
    }

    public static long lookup(List<MarketPriceHint> hints, String itemKey) {
        if (hints == null || itemKey == null || itemKey.isBlank()) {
            return 0L;
        }
        for (MarketPriceHint hint : hints) {
            if (itemKey.equals(hint.itemKey())) {
                return hint.unitPrice();
            }
        }
        return 0L;
    }
}
