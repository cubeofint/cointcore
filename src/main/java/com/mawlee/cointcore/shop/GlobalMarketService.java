package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.config.MarketMinPricesConfig;
import com.mawlee.cointcore.config.TraderOffersConfig;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GlobalMarketService {
    private static final Logger LOGGER = LogUtils.getLogger();

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
        long now = System.currentTimeMillis();
        for (GlobalMarketListing listing : GlobalMarketSavedData.get(server).allListings()) {
            if (!sellerId.equals(listing.sellerId()) || GlobalMarketMath.expired(now, listing.expiresAt())) {
                continue;
            }
            rows.add(new PlayerShopOfferSnapshot(
                    listing.id().toString(),
                    listing.template(),
                    listing.countPerDeal(),
                    listing.pricePerDeal(),
                    0L,
                    listing.stockItems()
            ));
        }
        return rows;
    }

    public static boolean create(
            ServerPlayer player,
            ItemStack template,
            int countPerDeal,
            long deals,
            int price
    ) {
        expire(player.server);
        int count = PlayerShopValidation.clampCount(countPerDeal);
        long items = Math.max(0L, deals);
        if (items < (long) count && GlobalMarketMath.clampDeals(deals) >= 1L) {
            items = GlobalMarketMath.itemsForDeals(count, deals);
        }
        int safePrice = GlobalMarketMath.clampPrice(price);
        ItemStack sample = GhostTemplate.sanitize(template);
        if (sample.isEmpty() || items < count || safePrice < 1) {
            return false;
        }
        if (rejectBelowMinPrice(player, sample, count, safePrice)) {
            return false;
        }
        GlobalMarketSavedData data = GlobalMarketSavedData.get(player.server);
        if (!GlobalMarketMath.canCreateListing(
                data.listingCount(player.getUUID()),
                TraderOffersConfig.playerShopMaxListings(),
                GlobalMarketMath.dealsFromStock(items, count),
                safePrice
        )) {
            return false;
        }
        if (TraderInventory.countMatchingLong(player.getInventory(), sample) < items) {
            return false;
        }
        long fee = GlobalMarketMath.listingFee(TraderOffersConfig.playerShopListingFee());
        if (fee > 0L && !GluonWallet.trySubtract(player.server, player.getUUID(), fee)) {
            return false;
        }
        if (!TraderInventory.removeMatching(player.getInventory(), sample, items)) {
            if (fee > 0L) {
                GluonWallet.add(player.server, player.getUUID(), fee);
            }
            return false;
        }
        long now = System.currentTimeMillis();
        data.put(new GlobalMarketListing(
                UUID.randomUUID(),
                player.getUUID(),
                player.getGameProfile().getName(),
                sample,
                count,
                safePrice,
                items,
                now,
                GlobalMarketMath.expiresAt(now, TraderOffersConfig.playerShopListingLifetimeDays()),
                0L
        ));
        return true;
    }

    public static boolean update(
            ServerPlayer player,
            String listingId,
            int countPerDeal,
            long deals,
            int price
    ) {
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
        int count = PlayerShopValidation.clampCount(countPerDeal);
        int safePrice = GlobalMarketMath.clampPrice(price);
        long safeDeals = GlobalMarketMath.clampDeals(deals);
        if (count < 1 || safePrice < 1 || safeDeals < 0L) {
            return false;
        }
        if (rejectBelowMinPrice(player, listing.template(), count, safePrice)) {
            return false;
        }
        long leftover = count == listing.countPerDeal()
                ? GlobalMarketMath.leftoverItems(listing.stockItems(), listing.countPerDeal())
                : 0L;
        long desired = GlobalMarketMath.saturatingAdd(GlobalMarketMath.itemsForDeals(count, safeDeals), leftover);
        long delta = desired - listing.stockItems();
        ItemStack sample = listing.template();
        if (delta > 0L && TraderInventory.countMatchingLong(player.getInventory(), sample) < delta) {
            return false;
        }
        if (delta > 0L && !TraderInventory.removeMatching(player.getInventory(), sample, delta)) {
            return false;
        }
        if (delta < 0L) {
            giveAmountOrReturn(player, sample, -delta);
        }
        listing.applyEdit(count, safePrice, desired);
        data.put(listing);
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
        if (player.getUUID().equals(listing.sellerId())) {
            giveAmountOrReturn(player, listing.template(), listing.remainingStock());
        } else {
            // Admin cancelling someone else's lot: the goods belong to the seller.
            data.addReturn(listing.sellerId(), listing.template(), listing.remainingStock());
        }
        return true;
    }

    /** Active lots of one seller, oldest first (for {@code /marketmod list}). */
    public static List<GlobalMarketListing> sellerListings(MinecraftServer server, UUID sellerId) {
        expire(server);
        long now = System.currentTimeMillis();
        List<GlobalMarketListing> rows = new ArrayList<>();
        for (GlobalMarketListing listing : GlobalMarketSavedData.get(server).allListings()) {
            if (sellerId.equals(listing.sellerId()) && listing.active(now)) {
                rows.add(listing);
            }
        }
        rows.sort(Comparator.comparingLong(GlobalMarketListing::createdAt));
        return rows;
    }

    /** Sellers that have active lots now, by name (for command suggestions). */
    public static List<String> activeSellerNames(MinecraftServer server) {
        long now = System.currentTimeMillis();
        return GlobalMarketSavedData.get(server).allListings().stream()
                .filter(listing -> listing.active(now) && listing.sellerName() != null)
                .map(GlobalMarketListing::sellerName)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    /**
     * Moderator removal: the lot disappears and its stock goes to the seller's returns (claimed in the shop
     * management screen). Returns the removed lot, or null if it does not exist.
     */
    public static GlobalMarketListing moderatorRemove(MinecraftServer server, UUID listingId) {
        expire(server);
        GlobalMarketSavedData data = GlobalMarketSavedData.get(server);
        GlobalMarketListing listing = data.remove(listingId);
        if (listing == null) {
            return null;
        }
        data.addReturn(listing.sellerId(), listing.template(), listing.remainingStock());
        refreshOpenViewers(server);
        return listing;
    }

    /** Item id → recommended unit price for every item on sale now (template for market-min-prices.json). */
    public static Map<String, Long> currentPriceTemplate(MinecraftServer server) {
        expire(server);
        GlobalMarketSavedData data = GlobalMarketSavedData.get(server);
        long now = System.currentTimeMillis();
        Map<String, Long> prices = new java.util.TreeMap<>();
        for (GlobalMarketListing listing : data.allListings()) {
            if (!listing.active(now)) {
                continue;
            }
            String id = BuiltInRegistries.ITEM.getKey(listing.stack().getItem()).toString();
            long price = Math.max(1L, recommendUnitPrice(data, listing.stack(), now));
            prices.merge(id, price, Math::min);
        }
        return prices;
    }

    public static long minUnitPrice(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0L;
        }
        return MarketMinPricesConfig.get().minUnitPrice(
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                stack.getTags().map(tag -> tag.location().toString())
        );
    }

    private static boolean rejectBelowMinPrice(ServerPlayer player, ItemStack stack, int countPerDeal, int pricePerDeal) {
        long min = minUnitPrice(stack);
        if (!MarketMinPrices.belowMinimum(pricePerDeal, countPerDeal, min)) {
            return false;
        }
        player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.MARKET_MIN_PRICE,
                stack.getHoverName().getString(), min));
        return true;
    }

    /**
     * Insert items into the owner's matching lot escrow. Returns accepted count.
     * Simulate never mutates. Off-thread / client / no match / blacklist / cap → 0.
     */
    public static int tryRestock(PlayerTraderBlockEntity shop, ItemStack stack, boolean simulate) {
        if (shop == null || stack == null || stack.isEmpty()) {
            logRestock("empty_input", shop, stack, simulate, 0);
            return 0;
        }
        if (shop.ownerId() == null) {
            logRestock("no_owner", shop, stack, simulate, 0);
            return 0;
        }
        net.minecraft.world.level.Level level = shop.getLevel();
        if (level == null || level.isClientSide || level.getServer() == null || !level.getServer().isSameThread()) {
            logRestock("not_server_thread", shop, stack, simulate, 0);
            return 0;
        }
        if (GlobalMarketBlacklist.forbidden(stack, TraderOffersConfig.playerShopItemBlacklist())) {
            logRestock("blacklist", shop, stack, simulate, 0);
            return 0;
        }
        expire(level.getServer());
        GlobalMarketSavedData data = GlobalMarketSavedData.get(level.getServer());
        long now = System.currentTimeMillis();
        long cap = TraderOffersConfig.playerShopMaxStockPerLot();
        UUID owner = shop.ownerId();
        GlobalMarketListing target = null;
        int seenOwnerLots = 0;
        int seenItemLots = 0;
        for (GlobalMarketListing listing : data.allListings()) {
            if (!owner.equals(listing.sellerId()) || GlobalMarketMath.expired(now, listing.expiresAt())) {
                continue;
            }
            seenOwnerLots++;
            if (!MarketRestock.matchesTemplate(listing.template(), stack)) {
                continue;
            }
            seenItemLots++;
            if (MarketRestock.acceptCount(listing.stockItems(), cap, stack.getCount()) < 1) {
                continue;
            }
            if (target == null || listing.createdAt() < target.createdAt()) {
                target = listing;
            }
        }
        if (target == null) {
            String reason = seenOwnerLots == 0 ? "no_lots" : seenItemLots == 0 ? "no_match" : "cap";
            logRestock(reason, shop, stack, simulate, 0);
            return 0;
        }
        int accepted = MarketRestock.acceptCount(target.stockItems(), cap, stack.getCount());
        if (accepted < 1) {
            logRestock("cap", shop, stack, simulate, 0);
            return 0;
        }
        if (simulate) {
            logRestock("simulate", shop, stack, true, accepted);
            return accepted;
        }
        long before = target.stockItems();
        target.addStock(accepted);
        data.put(target);
        logRestock("commit stock=" + before + "->" + target.stockItems(), shop, stack, false, accepted);
        refreshOpenViewers(level.getServer());
        return accepted;
    }

    public static void refreshOpenViewers(MinecraftServer server) {
        if (server == null) {
            return;
        }
        List<PlayerTraderListing> listings = null;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof PlayerTraderMenu menu) {
                if (listings == null) {
                    listings = catalog(server);
                }
                long balance = GluonWallet.get(player);
                String ownerName = menu.ownerName();
                if (TraderViewSync.sameShopView(
                        menu.gluonBalance(),
                        ownerName,
                        menu.listings(),
                        balance,
                        ownerName,
                        listings
                )) {
                    continue;
                }
                menu.refresh(balance, ownerName, listings);
                PacketDistributor.sendToPlayer(
                        player,
                        new PlayerTraderCatalogPayload(menu.containerId, balance, ownerName, listings)
                );
            } else if (player.containerMenu instanceof PlayerTraderManageMenu menu) {
                PlayerTraderManagePayload.syncIfChanged(player, menu, true);
            }
        }
    }

    private static void logRestock(
            String reason,
            PlayerTraderBlockEntity shop,
            ItemStack stack,
            boolean simulate,
            int accepted
    ) {
        if (!TraderOffersConfig.playerShopRestockDebug()) {
            return;
        }
        LOGGER.info(
                "player_trader restock reason={} simulate={} accepted={} owner={} item={} count={} pos={}",
                reason,
                simulate,
                accepted,
                shop == null ? null : shop.ownerId(),
                stack == null || stack.isEmpty() ? "-" : BuiltInRegistries.ITEM.getKey(stack.getItem()),
                stack == null ? 0 : stack.getCount(),
                shop == null ? null : shop.getBlockPos()
        );
    }

    public static void claimReturns(ServerPlayer player) {
        List<ItemStack> stacks = GlobalMarketSavedData.get(player.server).claimReturns(player.getUUID());
        for (ItemStack stack : stacks) {
            giveOrReturn(player, stack);
        }
    }

    public static void buy(ServerPlayer player, PlayerTraderMenu menu, String listingId, long requestedDeals) {
        buy(player, menu, listingId, requestedDeals, 0L, true);
    }

    public static void buy(
            ServerPlayer player,
            PlayerTraderMenu menu,
            String listingId,
            long requestedDeals,
            long expectedUnitPrice,
            boolean cheapest
    ) {
        expire(player.server);
        UUID id = parseId(listingId);
        GlobalMarketSavedData data = GlobalMarketSavedData.get(player.server);
        GlobalMarketListing first = id == null ? null : data.listing(id);
        long now = System.currentTimeMillis();
        boolean own = first != null && GlobalMarketMath.canBuyOwn(
                player.getUUID().toString(),
                first.sellerId() == null ? "" : first.sellerId().toString()
        );
        MarketLotPurchase.Snapshot snapshot = first == null
                ? MarketLotPurchase.Snapshot.missing()
                : new MarketLotPurchase.Snapshot(
                        true,
                        first.active(now),
                        own,
                        first.unitPrice(),
                        first.dealsLeft()
                );
        if (!cheapest) {
            MarketLotPurchase.Decision decision = MarketLotPurchase.validate(
                    snapshot,
                    expectedUnitPrice,
                    requestedDeals
            );
            if (!decision.ok()) {
                failPurchase(player, menu, decision.reason());
                return;
            }
        } else if (first == null || !first.active(now)) {
            fail(player, menu, TraderFeedbackKind.OFFER_UNAVAILABLE, "unavailable");
            return;
        } else if (own) {
            fail(player, menu, TraderFeedbackKind.OWN_SHOP, "own");
            return;
        }
        long requested = GlobalMarketMath.clampDeals(requestedDeals);
        if (requested < 1L) {
            fail(player, menu, TraderFeedbackKind.CANNOT_FILL, "stock");
            return;
        }
        if (expectedUnitPrice > 0L && first.unitPrice() > expectedUnitPrice) {
            fail(player, menu, TraderFeedbackKind.CANNOT_FILL, "price_up");
            return;
        }
        List<GlobalMarketListing> chain = cheapest
                ? matchingAtOrBelow(data, first, player.getUUID())
                : List.of(first);
        if (chain.isEmpty()) {
            fail(player, menu, TraderFeedbackKind.CANNOT_FILL, cheapest ? "price" : "unavailable");
            return;
        }
        long available = 0L;
        for (GlobalMarketListing listing : chain) {
            available = GlobalMarketMath.saturatingAdd(available, listing.dealsLeft());
        }
        if (available < requested) {
            fail(player, menu, TraderFeedbackKind.CANNOT_FILL, "stock");
            return;
        }
        double commission = TraderOffersConfig.playerShopCommissionPercent();
        long debit = debitFor(chain, requested, commission);
        long balance = GluonWallet.get(player);
        if (debit < 1L || debit > balance) {
            fail(player, menu, TraderFeedbackKind.CANNOT_FILL, "money");
            return;
        }
        long remaining = requested;
        List<Taken> taken = new ArrayList<>();
        for (GlobalMarketListing listing : chain) {
            if (remaining < 1L) {
                break;
            }
            if (listing.unitPrice() > first.unitPrice()) {
                restoreAll(data, taken);
                fail(player, menu, TraderFeedbackKind.CANNOT_FILL, "price");
                return;
            }
            long take = Math.min(remaining, listing.dealsLeft());
            long items = listing.takeDeals(take);
            if (items < 1L) {
                continue;
            }
            if (listing.stockItems() < 1L) {
                data.remove(listing.id());
            } else {
                data.put(listing);
            }
            taken.add(new Taken(listing, take, items));
            remaining -= take;
        }
        if (taken.isEmpty() || remaining > 0L) {
            restoreAll(data, taken);
            fail(player, menu, TraderFeedbackKind.CANNOT_FILL, "stock");
            return;
        }
        long totalDebit = 0L;
        long totalFee = 0L;
        long totalItems = 0L;
        for (Taken row : taken) {
            Commission.Result deal = Commission.ofDeal(row.listing.pricePerDeal(), row.deals, commission);
            totalDebit = GlobalMarketMath.saturatingAdd(totalDebit, deal.total());
            totalFee = GlobalMarketMath.saturatingAdd(totalFee, deal.fee());
            totalItems = GlobalMarketMath.saturatingAdd(totalItems, row.items);
        }
        if (!GluonWallet.trySubtract(player.server, player.getUUID(), totalDebit)) {
            restoreAll(data, taken);
            fail(player, menu, TraderFeedbackKind.CANNOT_FILL, "money");
            return;
        }
        for (Taken row : taken) {
            Commission.Result deal = Commission.ofDeal(row.listing.pricePerDeal(), row.deals, commission);
            long sliceDebit = deal.total();
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
            TraderInventory.giveAmount(player.getInventory(), row.listing.template(), row.items);
        }
        String name = first.stack().getHoverName().getString();
        int shown = totalItems > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalItems;
        notify(
                player,
                menu,
                TraderFeedbackKind.BOUGHT,
                shown,
                name,
                totalDebit,
                totalFee,
                CointCoreMessages.TRADER_BOUGHT,
                shown,
                name,
                totalDebit,
                totalFee
        );
    }

    public static void expire(MinecraftServer server) {
        GlobalMarketSavedData data = GlobalMarketSavedData.get(server);
        long now = System.currentTimeMillis();
        for (GlobalMarketListing listing : data.allListings()) {
            if (!GlobalMarketMath.expired(now, listing.expiresAt()) && listing.stockItems() > 0L) {
                continue;
            }
            data.remove(listing.id());
            long leftover = listing.remainingStock();
            if (leftover > 0L && listing.sellerId() != null) {
                data.addReturn(listing.sellerId(), listing.template(), leftover);
            }
        }
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 200 == 0) {
            expire(server);
        }
        if (server.getTickCount() % 20 == 0) {
            refreshOpenViewers(server);
        }
    }

    private static void restoreAll(GlobalMarketSavedData data, List<Taken> taken) {
        for (int index = taken.size() - 1; index >= 0; index--) {
            restoreListing(data, taken.get(index));
        }
    }

    private static void restoreListing(GlobalMarketSavedData data, Taken taken) {
        GlobalMarketListing current = data.listing(taken.listing.id());
        if (current != null) {
            current.restoreTaken(taken.deals, taken.items);
            data.put(current);
            return;
        }
        taken.listing.restoreTaken(taken.deals, taken.items);
        data.put(taken.listing);
    }

    private static List<GlobalMarketListing> matchingAtOrBelow(
            GlobalMarketSavedData data,
            GlobalMarketListing first,
            UUID buyerId
    ) {
        List<GlobalMarketListing> same = new ArrayList<>();
        long now = System.currentTimeMillis();
        long cap = first.unitPrice();
        String buyer = buyerId == null ? "" : buyerId.toString();
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
            if (listing.unitPrice() > cap) {
                continue;
            }
            if (GlobalMarketMath.canBuyOwn(buyer, listing.sellerId().toString())) {
                continue;
            }
            same.add(listing);
        }
        same.sort(Comparator.comparingInt(GlobalMarketListing::pricePerDeal).thenComparingLong(GlobalMarketListing::createdAt));
        return same;
    }

    private static long debitFor(List<GlobalMarketListing> chain, long requested, double commission) {
        long remaining = requested;
        long total = 0L;
        for (GlobalMarketListing listing : chain) {
            if (remaining < 1L) {
                break;
            }
            long take = Math.min(remaining, listing.dealsLeft());
            total = GlobalMarketMath.saturatingAdd(
                    total,
                    Commission.buyerDebit(listing.pricePerDeal(), take, commission)
            );
            remaining -= take;
        }
        return remaining > 0L ? Long.MAX_VALUE : total;
    }

    private static void giveAmountOrReturn(ServerPlayer player, ItemStack template, long amount) {
        TraderInventory.giveAmount(player.getInventory(), template, amount);
    }

    private static void giveOrReturn(ServerPlayer player, ItemStack goods) {
        if (goods.isEmpty()) {
            return;
        }
        TraderInventory.giveOrFail(player.getInventory(), goods);
    }

    private static void fail(ServerPlayer player, PlayerTraderMenu menu, TraderFeedbackKind kind, String reason) {
        String chatKey = switch (kind) {
            case OWN_SHOP -> CointCoreMessages.PLAYER_SHOP_OWN;
            case CANNOT_FILL -> cannotFillChat(reason);
            case OFFER_UNAVAILABLE, INVENTORY_FULL, NOT_ENOUGH_GLUONS, NOT_ENOUGH_ITEMS, BOUGHT, SOLD,
                    OUT_OF_STOCK, STOCK_FULL, OWNER_BROKE -> CointCoreMessages.TRADER_OFFER_UNAVAILABLE;
        };
        notify(player, menu, kind, 0, reason, 0L, 0L, chatKey);
    }

    private static void failPurchase(ServerPlayer player, PlayerTraderMenu menu, MarketLotPurchase.Reason reason) {
        switch (reason) {
            case MISSING, INACTIVE -> fail(player, menu, TraderFeedbackKind.OFFER_UNAVAILABLE, "unavailable");
            case OWN_SHOP -> fail(player, menu, TraderFeedbackKind.OWN_SHOP, "own");
            case PRICE_UP -> fail(player, menu, TraderFeedbackKind.CANNOT_FILL, "price_up");
            case NO_STOCK, BAD_QTY -> fail(player, menu, TraderFeedbackKind.CANNOT_FILL, "stock");
            case OK -> {
            }
        }
    }

    private static String cannotFillChat(String reason) {
        return switch (reason == null ? "" : reason) {
            case "stock" -> CointCoreMessages.PLAYER_SHOP_CANNOT_FILL_STOCK;
            case "money" -> CointCoreMessages.PLAYER_SHOP_CANNOT_FILL_MONEY;
            case "price" -> CointCoreMessages.PLAYER_SHOP_CANNOT_FILL_PRICE;
            case "price_up" -> CointCoreMessages.PLAYER_SHOP_CANNOT_FILL_PRICE_UP;
            default -> CointCoreMessages.PLAYER_SHOP_CANNOT_FILL;
        };
    }

    private static void notify(
            ServerPlayer player,
            PlayerTraderMenu menu,
            TraderFeedbackKind kind,
            int count,
            String itemName,
            long gluons,
            long fee,
            String chatKey,
            Object... chatArgs
    ) {
        PacketDistributor.sendToPlayer(
                player,
                new TraderFeedbackPayload(menu.containerId, kind, count, itemName == null ? "" : itemName, gluons, fee)
        );
        refreshShop(player, menu);
        if (chatArgs.length == 0) {
            player.sendSystemMessage(CointCoreMessages.forPlayer(player, chatKey));
        } else {
            player.sendSystemMessage(CointCoreMessages.forPlayer(player, chatKey, chatArgs));
        }
    }

    private static void refreshShop(ServerPlayer player, PlayerTraderMenu menu) {
        List<PlayerTraderListing> listings = catalog(player.server);
        long balance = GluonWallet.get(player);
        menu.refresh(balance, menu.ownerName(), listings);
        PacketDistributor.sendToPlayer(
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

    private record Taken(GlobalMarketListing listing, long deals, long items) {
    }
}
