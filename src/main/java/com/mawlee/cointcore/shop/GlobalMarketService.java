package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.config.TraderOffersConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GlobalMarketService {
    private GlobalMarketService() {
    }

    public static List<PlayerTraderListing> catalog(MinecraftServer server) {
        expire(server);
        long now = System.currentTimeMillis();
        double commission = TraderOffersConfig.playerShopCommissionPercent();
        GlobalMarketSavedData data = GlobalMarketSavedData.get(server);
        Map<String, Long> marketByKey = new HashMap<>();
        List<PlayerTraderListing> rows = new ArrayList<>();
        for (GlobalMarketListing listing : data.allListings()) {
            if (!listing.active(now)) {
                continue;
            }
            TraderOffer offer = TraderOffer.of(
                    listing.id().toString(),
                    listing.stack(),
                    listing.countPerDeal(),
                    listing.pricePerDeal(),
                    0L,
                    commission
            );
            String key = itemKey(listing.stack());
            long market = marketByKey.computeIfAbsent(key, ignored -> recommendUnitPrice(data, listing.stack(), now));
            rows.add(new PlayerTraderListing(
                    listing.id().toString(),
                    offer,
                    listing.dealsLeft(),
                    listing.sellerName(),
                    listing.sellerId().toString(),
                    listing.createdAt(),
                    itemMod(listing.stack()),
                    listing.expiresAt(),
                    market
            ));
        }
        return rows;
    }

    public static List<PlayerShopOfferSnapshot> ownSnapshots(MinecraftServer server, UUID sellerId) {
        expire(server);
        List<PlayerShopOfferSnapshot> rows = new ArrayList<>();
        for (GlobalMarketListing listing : GlobalMarketSavedData.get(server).allListings()) {
            if (!sellerId.equals(listing.sellerId()) || listing.dealsLeft() < 1) {
                continue;
            }
            rows.add(new PlayerShopOfferSnapshot(
                    listing.id().toString(),
                    listing.stack(),
                    listing.countPerDeal(),
                    listing.pricePerDeal(),
                    0L,
                    listing.dealsLeft()
            ));
        }
        return rows;
    }

    public static boolean create(
            ServerPlayer player,
            ItemStack template,
            int countPerDeal,
            int deals,
            int price
    ) {
        expire(player.server);
        int count = PlayerShopValidation.clampCount(countPerDeal);
        int safeDeals = GlobalMarketMath.clampDeals(deals);
        int safePrice = GlobalMarketMath.clampPrice(price);
        ItemStack sample = GhostTemplate.sanitize(template);
        if (sample.isEmpty() || safeDeals < 1 || safePrice < 1) {
            return false;
        }
        GlobalMarketSavedData data = GlobalMarketSavedData.get(player.server);
        if (!GlobalMarketMath.canCreateListing(
                data.listingCount(player.getUUID()),
                TraderOffersConfig.playerShopMaxListings(),
                safeDeals,
                safePrice
        )) {
            return false;
        }
        int items = GlobalMarketMath.itemsForDeals(count, safeDeals);
        ItemStack unit = sample.copy();
        unit.setCount(count);
        if (TraderInventory.countMatching(player.getInventory(), unit) < items) {
            return false;
        }
        long fee = GlobalMarketMath.listingFee(TraderOffersConfig.playerShopListingFee());
        if (fee > 0L && !GluonWallet.trySubtract(player.server, player.getUUID(), fee)) {
            return false;
        }
        if (!TraderInventory.removeMatching(player.getInventory(), unit, items)) {
            if (fee > 0L) {
                GluonWallet.add(player.server, player.getUUID(), fee);
            }
            return false;
        }
        long now = System.currentTimeMillis();
        ItemStack escrow = unit.copy();
        escrow.setCount(count);
        data.put(new GlobalMarketListing(
                UUID.randomUUID(),
                player.getUUID(),
                player.getGameProfile().getName(),
                escrow,
                safeDeals,
                safePrice,
                now,
                GlobalMarketMath.expiresAt(now, TraderOffersConfig.playerShopListingLifetimeDays()),
                0
        ));
        return true;
    }

    public static boolean cancel(ServerPlayer player, String listingId) {
        expire(player.server);
        UUID id = parseId(listingId);
        if (id == null) {
            return false;
        }
        GlobalMarketSavedData data = GlobalMarketSavedData.get(player.server);
        GlobalMarketListing listing = data.listing(id);
        if (listing == null) {
            return false;
        }
        if (!GlobalMarketMath.canCancel(
                player.getUUID().toString(),
                listing.sellerId().toString(),
                PlayerShopAccess.canAdmin(player)
        )) {
            return false;
        }
        data.remove(id);
        giveOrReturn(player, listing.remainingGoods());
        return true;
    }

    public static void claimReturns(ServerPlayer player) {
        List<ItemStack> stacks = GlobalMarketSavedData.get(player.server).claimReturns(player.getUUID());
        for (ItemStack stack : stacks) {
            giveOrReturn(player, stack);
        }
    }

    public static void buy(ServerPlayer player, PlayerTraderMenu menu, String listingId, boolean max) {
        expire(player.server);
        UUID id = parseId(listingId);
        if (id == null) {
            fail(player, menu);
            return;
        }
        GlobalMarketSavedData data = GlobalMarketSavedData.get(player.server);
        GlobalMarketListing first = data.listing(id);
        if (first == null || !first.active(System.currentTimeMillis())) {
            fail(player, menu);
            return;
        }
        if (GlobalMarketMath.canBuyOwn(player.getUUID().toString(), first.sellerId().toString())) {
            fail(player, menu);
            return;
        }
        List<GlobalMarketListing> chain = matchingCheapest(data, first);
        int available = 0;
        for (GlobalMarketListing listing : chain) {
            available += listing.dealsLeft();
        }
        double commission = TraderOffersConfig.playerShopCommissionPercent();
        long balance = GluonWallet.get(player);
        int cap = max ? available : 1;
        int affordable = 0;
        long running = 0L;
        outer:
        for (GlobalMarketListing listing : chain) {
            Commission.Result unit = Commission.of(listing.pricePerDeal(), commission);
            for (int step = 0; step < listing.dealsLeft() && affordable < cap; step++) {
                long next = GlobalMarketMath.saturatingAdd(running, unit.total());
                if (next > balance) {
                    break outer;
                }
                running = next;
                affordable++;
            }
        }
        int requested = affordable;
        if (requested < 1) {
            fail(player, menu);
            return;
        }
        int remaining = requested;
        List<Taken> taken = new ArrayList<>();
        for (GlobalMarketListing listing : chain) {
            if (remaining < 1) {
                break;
            }
            int take = Math.min(remaining, listing.dealsLeft());
            ItemStack goods = listing.takeDeals(take);
            if (goods.isEmpty()) {
                continue;
            }
            if (listing.dealsLeft() < 1) {
                data.remove(listing.id());
            } else {
                data.put(listing);
            }
            taken.add(new Taken(listing, take, goods));
            remaining -= take;
        }
        if (taken.isEmpty()) {
            fail(player, menu);
            return;
        }
        long totalDebit = 0L;
        for (Taken row : taken) {
            Commission.Result unit = Commission.of(row.listing.pricePerDeal(), commission);
            totalDebit = GlobalMarketMath.saturatingAdd(
                    totalDebit,
                    GlobalMarketMath.buyerDebit(row.listing.pricePerDeal(), unit.fee(), row.deals)
            );
        }
        if (!GluonWallet.trySubtract(player.server, player.getUUID(), totalDebit)) {
            for (Taken row : taken) {
                restoreListing(data, row);
            }
            fail(player, menu);
            return;
        }
        for (Taken row : taken) {
            Commission.Result unit = Commission.of(row.listing.pricePerDeal(), commission);
            long sliceDebit = GlobalMarketMath.buyerDebit(row.listing.pricePerDeal(), unit.fee(), row.deals);
            long credit = GlobalMarketMath.sellerCredit(row.listing.pricePerDeal(), row.deals);
            GluonWallet.add(player.server, row.listing.sellerId(), credit);
            data.recordSold(row.listing.sellerId(), row.deals, credit);
            data.recordSale(
                    itemKey(row.listing.stack()),
                    GlobalMarketPriceMath.unitPrice(row.listing.pricePerDeal(), row.listing.countPerDeal()),
                    System.currentTimeMillis()
            );
            long buyerAfter = GluonWallet.get(player.server, player.getUUID());
            long sellerAfter = GluonWallet.get(player.server, row.listing.sellerId());
            CurrencyMovementService.record(
                    player.server,
                    player.getUUID(),
                    player.getGameProfile().getName(),
                    row.listing.sellerId(),
                    row.listing.sellerName(),
                    credit,
                    CurrencyMovementType.PLAYER_SHOP_BUY,
                    row.listing.id().toString(),
                    List.of(
                            new CurrencyMovement.Delta(player.getUUID(), -sliceDebit, buyerAfter),
                            new CurrencyMovement.Delta(row.listing.sellerId(), credit, sellerAfter)
                    ),
                    null
            );
            TraderInventory.giveOrFail(player.getInventory(), row.goods);
        }
        refreshShop(player, menu);
    }

    public static void expire(MinecraftServer server) {
        GlobalMarketSavedData data = GlobalMarketSavedData.get(server);
        long now = System.currentTimeMillis();
        for (GlobalMarketListing listing : data.allListings()) {
            if (!GlobalMarketMath.expired(now, listing.expiresAt()) && listing.dealsLeft() >= 1) {
                continue;
            }
            data.remove(listing.id());
            ItemStack leftover = listing.remainingGoods();
            if (!leftover.isEmpty() && listing.sellerId() != null) {
                data.addReturn(listing.sellerId(), leftover);
            }
        }
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 200 == 0) {
            expire(server);
        }
    }

    private static void restoreListing(GlobalMarketSavedData data, Taken taken) {
        GlobalMarketListing current = data.listing(taken.listing.id());
        int deals = taken.deals + (current == null ? 0 : current.dealsLeft());
        int sold = Math.max(0, taken.listing.soldDeals() - taken.deals);
        data.put(new GlobalMarketListing(
                taken.listing.id(),
                taken.listing.sellerId(),
                taken.listing.sellerName(),
                taken.listing.stack(),
                deals,
                taken.listing.pricePerDeal(),
                taken.listing.createdAt(),
                taken.listing.expiresAt(),
                sold
        ));
    }

    private static List<GlobalMarketListing> matchingCheapest(GlobalMarketSavedData data, GlobalMarketListing first) {
        List<GlobalMarketListing> same = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (GlobalMarketListing listing : data.allListings()) {
            if (!listing.active(now)) {
                continue;
            }
            if (!ItemStack.isSameItemSameComponents(listing.stack(), first.stack())) {
                continue;
            }
            if (listing.countPerDeal() != first.countPerDeal()) {
                continue;
            }
            same.add(listing);
        }
        same.sort(Comparator.comparingInt(GlobalMarketListing::pricePerDeal).thenComparingLong(GlobalMarketListing::createdAt));
        return same;
    }

    private static void giveOrReturn(ServerPlayer player, ItemStack goods) {
        if (goods.isEmpty()) {
            return;
        }
        TraderInventory.giveOrFail(player.getInventory(), goods);
    }

    private static void fail(ServerPlayer player, PlayerTraderMenu menu) {
        refreshShop(player, menu);
    }

    private static void refreshShop(ServerPlayer player, PlayerTraderMenu menu) {
        List<PlayerTraderListing> listings = catalog(player.server);
        long balance = GluonWallet.get(player);
        menu.refresh(balance, menu.ownerName(), listings);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player,
                new PlayerTraderCatalogPayload(menu.containerId, balance, menu.ownerName(), listings)
        );
    }

    private static UUID parseId(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static String itemKey(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return GlobalMarketPriceMath.itemKey(
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                stack.getComponentsPatch().toString()
        );
    }

    public static long recommendUnitPrice(MinecraftServer server, ItemStack stack) {
        expire(server);
        return recommendUnitPrice(GlobalMarketSavedData.get(server), stack, System.currentTimeMillis());
    }

    public static List<MarketPriceHint> priceHints(MinecraftServer server, Iterable<ItemStack> stacks) {
        expire(server);
        GlobalMarketSavedData data = GlobalMarketSavedData.get(server);
        long now = System.currentTimeMillis();
        Map<String, Long> unique = new LinkedHashMap<>();
        if (stacks != null) {
            for (ItemStack stack : stacks) {
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                String key = itemKey(stack);
                if (key.isBlank() || unique.containsKey(key)) {
                    continue;
                }
                long price = recommendUnitPrice(data, stack, now);
                if (price > 0L) {
                    unique.put(key, price);
                }
                if (unique.size() >= 64) {
                    break;
                }
            }
        }
        List<MarketPriceHint> hints = new ArrayList<>(unique.size());
        for (Map.Entry<String, Long> entry : unique.entrySet()) {
            hints.add(new MarketPriceHint(entry.getKey(), entry.getValue()));
        }
        return hints;
    }

    private static long recommendUnitPrice(GlobalMarketSavedData data, ItemStack stack, long now) {
        if (stack == null || stack.isEmpty()) {
            return 0L;
        }
        String key = itemKey(stack);
        return GlobalMarketPriceMath.recommend(data.recentUnitPrices(key, now), lowestListingUnitPrice(data, key, now));
    }

    private static long lowestListingUnitPrice(GlobalMarketSavedData data, String itemKey, long now) {
        long lowest = 0L;
        for (GlobalMarketListing listing : data.allListings()) {
            if (!listing.active(now) || !itemKey.equals(itemKey(listing.stack()))) {
                continue;
            }
            long unit = GlobalMarketPriceMath.unitPrice(listing.pricePerDeal(), listing.countPerDeal());
            if (unit <= 0L) {
                continue;
            }
            if (lowest <= 0L || unit < lowest) {
                lowest = unit;
            }
        }
        return lowest;
    }

    private static String itemMod(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
    }

    private record Taken(GlobalMarketListing listing, int deals, ItemStack goods) {
    }
}
