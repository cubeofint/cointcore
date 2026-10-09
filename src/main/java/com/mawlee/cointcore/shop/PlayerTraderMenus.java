package com.mawlee.cointcore.shop;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;

import java.util.ArrayList;
import java.util.List;

public final class PlayerTraderMenus {
    private PlayerTraderMenus() {
    }

    public static void open(ServerPlayer player, PlayerTraderBlockEntity shop, boolean manage) {
        if (manage && shop.canManage(player)) {
            openManage(player, shop);
        } else {
            openShop(player, shop);
        }
    }

    public static void openShop(ServerPlayer player, PlayerTraderBlockEntity shop) {
        long balance = GluonWallet.get(player);
        boolean canManage = shop.canManage(player);
        List<PlayerTraderListing> listings = listingsOf(shop);
        player.openMenu(
                new SimpleMenuProvider(
                        (containerId, inventory, opener) -> new PlayerTraderMenu(
                                containerId,
                                inventory,
                                ContainerLevelAccess.create(shop.getLevel(), shop.getBlockPos()),
                                balance,
                                shop.ownerName(),
                                canManage,
                                listings
                        ),
                        shop.title()
                ),
                buffer -> PlayerTraderMenu.writeOpenData(
                        buffer,
                        balance,
                        shop.ownerName(),
                        canManage,
                        listings
                )
        );
    }

    public static void openManage(ServerPlayer player, PlayerTraderBlockEntity shop) {
        if (!shop.canManage(player)) {
            openShop(player, shop);
            return;
        }
        List<PlayerShopOfferSnapshot> offers = PlayerShopOfferSnapshot.of(shop);
        player.openMenu(
                new SimpleMenuProvider(
                        (containerId, inventory, opener) -> new PlayerTraderManageMenu(
                                containerId,
                                inventory,
                                ContainerLevelAccess.create(shop.getLevel(), shop.getBlockPos()),
                                shop.stock(),
                                shop.ownerName(),
                                shop.lifetimeRevenue(),
                                offers
                        ),
                        Component.translatable("container.cointcore.player_trader.manage", shop.ownerName())
                ),
                buffer -> PlayerTraderManageMenu.writeOpenData(
                        buffer,
                        shop.ownerName(),
                        shop.lifetimeRevenue(),
                        offers
                )
        );
    }

    public static List<PlayerTraderListing> listingsOf(PlayerTraderBlockEntity shop) {
        List<TraderOffer> offers = shop.traderOffers();
        int[] stock = shop.stockLeftForTraderOffers();
        List<PlayerTraderListing> listings = new ArrayList<>(offers.size());
        for (int index = 0; index < offers.size(); index++) {
            listings.add(new PlayerTraderListing(offers.get(index), stock[index]));
        }
        return listings;
    }

    public static void refreshOpen(ServerPlayer player, PlayerTraderBlockEntity shop) {
        if (player.containerMenu instanceof PlayerTraderMenu menu) {
            menu.refresh(GluonWallet.get(player), shop.ownerName(), listingsOf(shop));
        } else if (player.containerMenu instanceof PlayerTraderManageMenu menu) {
            menu.refresh(shop.ownerName(), shop.lifetimeRevenue(), PlayerShopOfferSnapshot.of(shop));
        }
    }
}
