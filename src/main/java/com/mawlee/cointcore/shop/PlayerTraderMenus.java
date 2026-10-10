package com.mawlee.cointcore.shop;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class PlayerTraderMenus {
    private PlayerTraderMenus() {
    }

    public static void open(ServerPlayer player, PlayerTraderBlockEntity shop, boolean manage) {
        if (manage && PlayerShopAccess.canUse(player)) {
            openManage(player, shop);
        } else {
            openShop(player, shop);
        }
    }

    public static void openShop(ServerPlayer player, PlayerTraderBlockEntity shop) {
        GlobalMarketMigration.migrate(player.server, shop);
        long balance = GluonWallet.get(player);
        boolean canManage = PlayerShopAccess.canUse(player);
        List<PlayerTraderListing> listings = listingsOf(player.server);
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
        if (!PlayerShopAccess.canUse(player)) {
            openShop(player, shop);
            return;
        }
        GlobalMarketMigration.migrate(player.server, shop);
        List<PlayerShopOfferSnapshot> offers = GlobalMarketService.ownSnapshots(player.server, player.getUUID());
        GlobalMarketSavedData.SoldStats stats = GlobalMarketSavedData.get(player.server).stats(player.getUUID());
        long revenue = stats.gluons();
        int returns = GlobalMarketSavedData.get(player.server).returnCount(player.getUUID());
        List<MarketPriceHint> hints = priceHints(player, offers);
        player.openMenu(
                new SimpleMenuProvider(
                        (containerId, inventory, opener) -> new PlayerTraderManageMenu(
                                containerId,
                                inventory,
                                ContainerLevelAccess.create(shop.getLevel(), shop.getBlockPos()),
                                shop.ownerName(),
                                revenue,
                                offers,
                                returns,
                                hints
                        ),
                        Component.translatable("container.cointcore.player_trader.manage", player.getGameProfile().getName())
                ),
                buffer -> PlayerTraderManageMenu.writeOpenData(
                        buffer,
                        shop.ownerName(),
                        revenue,
                        offers,
                        returns,
                        hints
                )
        );
    }

    public static List<PlayerTraderListing> listingsOf(MinecraftServer server) {
        return GlobalMarketService.catalog(server);
    }

    public static List<PlayerTraderListing> listingsOf(PlayerTraderBlockEntity shop) {
        if (shop.getLevel() != null && shop.getLevel().getServer() != null) {
            return listingsOf(shop.getLevel().getServer());
        }
        return List.of();
    }

    public static void refreshOpen(ServerPlayer player, PlayerTraderBlockEntity shop) {
        if (player.containerMenu instanceof PlayerTraderMenu menu) {
            menu.refresh(GluonWallet.get(player), shop.ownerName(), listingsOf(player.server));
        } else if (player.containerMenu instanceof PlayerTraderManageMenu menu) {
            GlobalMarketSavedData.SoldStats stats = GlobalMarketSavedData.get(player.server).stats(player.getUUID());
            List<PlayerShopOfferSnapshot> offers = GlobalMarketService.ownSnapshots(player.server, player.getUUID());
            menu.refresh(
                    shop.ownerName(),
                    stats.gluons(),
                    offers,
                    GlobalMarketSavedData.get(player.server).returnCount(player.getUUID()),
                    priceHints(player, offers)
            );
        }
    }

    static List<MarketPriceHint> priceHints(ServerPlayer player, List<PlayerShopOfferSnapshot> offers) {
        List<ItemStack> stacks = new ArrayList<>();
        Inventory inventory = player.getInventory();
        for (int index = 0; index < inventory.getContainerSize(); index++) {
            stacks.add(inventory.getItem(index));
        }
        if (offers != null) {
            for (PlayerShopOfferSnapshot offer : offers) {
                stacks.add(offer.template());
            }
        }
        if (player.containerMenu instanceof PlayerTraderManageMenu menu) {
            stacks.add(menu.ghostItem());
        }
        return GlobalMarketService.priceHints(player.server, stacks);
    }
}
