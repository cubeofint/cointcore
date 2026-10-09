package com.mawlee.cointcore.shop;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Network snapshot of a stored player-shop offer (raw prices, stock count).
 */
public record PlayerShopOfferSnapshot(
        String id,
        ItemStack template,
        int count,
        long buyPrice,
        long sellPrice,
        int stockItems
) {
    private static final int MAX = PlayerTraderBlockEntity.MAX_OFFERS;

    public static List<PlayerShopOfferSnapshot> of(PlayerTraderBlockEntity shop) {
        List<PlayerShopOfferSnapshot> list = new ArrayList<>();
        for (PlayerShopOffer offer : shop.offers()) {
            list.add(new PlayerShopOfferSnapshot(
                    offer.id(),
                    offer.template(),
                    offer.count(),
                    offer.buyPrice(),
                    offer.sellPrice(),
                    ShopContainers.countMatching(shop.stock(), offer.template())
            ));
        }
        return list;
    }

    public static void writeList(RegistryFriendlyByteBuf buffer, List<PlayerShopOfferSnapshot> offers) {
        List<PlayerShopOfferSnapshot> limited = offers.size() > MAX ? offers.subList(0, MAX) : offers;
        ByteBufCodecs.VAR_INT.encode(buffer, limited.size());
        for (PlayerShopOfferSnapshot offer : limited) {
            ByteBufCodecs.STRING_UTF8.encode(buffer, offer.id());
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, offer.template());
            ByteBufCodecs.VAR_INT.encode(buffer, offer.count());
            buffer.writeLong(offer.buyPrice());
            buffer.writeLong(offer.sellPrice());
            ByteBufCodecs.VAR_INT.encode(buffer, offer.stockItems());
        }
    }

    public static List<PlayerShopOfferSnapshot> readList(RegistryFriendlyByteBuf buffer) {
        List<PlayerShopOfferSnapshot> offers = new ArrayList<>();
        if (buffer.readableBytes() <= 0) {
            return offers;
        }
        int count = ByteBufCodecs.VAR_INT.decode(buffer);
        for (int index = 0; index < count && index < MAX; index++) {
            String id = ByteBufCodecs.STRING_UTF8.decode(buffer);
            ItemStack template = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
            int lot = ByteBufCodecs.VAR_INT.decode(buffer);
            long buy = buffer.readLong();
            long sell = buffer.readLong();
            int stock = ByteBufCodecs.VAR_INT.decode(buffer);
            offers.add(new PlayerShopOfferSnapshot(id, template, lot, buy, sell, stock));
        }
        return offers;
    }
}
