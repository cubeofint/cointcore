package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.config.TraderOffersConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Player-owned vending machine. Does not implement {@link net.minecraft.world.Container}
 * so hoppers/pipes get no item-handler capability.
 * <p>
 * Revenue is <strong>not</strong> stored in the block for later withdraw. Each deal
 * immediately debits the customer (or owner, when buying from a player) and credits
 * the other wallet, even if the owner is offline — same contract as {@code /pay}.
 * {@link #lifetimeRevenue} is a statistic of gluons credited to the owner from
 * customer purchases at this block.
 */
public class PlayerTraderBlockEntity extends BlockEntity {
    public static final int STOCK_SIZE = 27;
    public static final int MAX_OFFERS = 32;

    private UUID ownerId;
    private String ownerName = "";
    private long lifetimeRevenue;
    private final SimpleContainer stock = new SimpleContainer(STOCK_SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            PlayerTraderBlockEntity.this.setChanged();
        }
    };
    private final List<PlayerShopOffer> offers = new ArrayList<>();

    public PlayerTraderBlockEntity(BlockPos pos, BlockState state) {
        super(ShopBlockEntities.PLAYER_TRADER.get(), pos, state);
    }

    public UUID ownerId() {
        return ownerId;
    }

    public String ownerName() {
        return ownerName == null || ownerName.isBlank() ? "?" : ownerName;
    }

    public void setOwner(UUID ownerId, String ownerName) {
        this.ownerId = ownerId;
        this.ownerName = ownerName == null ? "" : ownerName;
        setChanged();
    }

    public boolean canManage(Player player) {
        return PlayerShopAccess.canManage(player, ownerId);
    }

    public long lifetimeRevenue() {
        return lifetimeRevenue;
    }

    public SimpleContainer stock() {
        return stock;
    }

    public List<PlayerShopOffer> offers() {
        return List.copyOf(offers);
    }

    public PlayerShopOffer offer(int index) {
        if (index < 0 || index >= offers.size()) {
            return null;
        }
        return offers.get(index);
    }

    public List<TraderOffer> traderOffers() {
        double commission = TraderOffersConfig.playerShopCommissionPercent();
        List<TraderOffer> listed = new ArrayList<>(offers.size());
        for (PlayerShopOffer offer : offers) {
            if (offer.isValid()) {
                listed.add(offer.toTraderOffer(commission));
            }
        }
        return listed;
    }

    public int[] stockLeftForTraderOffers() {
        List<PlayerShopOffer> valid = validOffers();
        int[] left = new int[valid.size()];
        for (int index = 0; index < valid.size(); index++) {
            left[index] = ShopContainers.countMatching(stock, valid.get(index).template());
        }
        return left;
    }

    public List<PlayerShopOffer> validOffers() {
        List<PlayerShopOffer> valid = new ArrayList<>();
        for (PlayerShopOffer offer : offers) {
            if (offer.isValid()) {
                valid.add(offer);
            }
        }
        return valid;
    }

    public synchronized boolean saveOffer(int index, ItemStack template, int count, long buyPrice, long sellPrice) {
        return saveOffer(index, template, count, buyPrice, sellPrice, ownerId, ownerName);
    }

    /**
     * Persist a listing. {@code sellerId} must come from the server-side acting player,
     * never from a client-supplied owner field.
     */
    public synchronized boolean saveOffer(
            int index,
            ItemStack template,
            int count,
            long buyPrice,
            long sellPrice,
            UUID sellerId,
            String sellerName
    ) {
        if (!PlayerShopValidation.offerValid(template == null || template.isEmpty(), count, buyPrice, sellPrice)) {
            return false;
        }
        String sellerKey = PlayerShopManagementPolicy.sellerId(sellerId == null ? null : sellerId.toString());
        UUID storedSeller = sellerKey == null ? null : UUID.fromString(sellerKey);
        if (index >= 0 && index < offers.size()) {
            PlayerShopOffer offer = offers.get(index);
            offer.setTemplate(template);
            offer.setCount(count);
            offer.setBuyPrice(buyPrice);
            offer.setSellPrice(sellPrice);
            offer.setSeller(storedSeller, sellerName);
            setChanged();
            return true;
        }
        if (offers.size() >= MAX_OFFERS) {
            return false;
        }
        offers.add(new PlayerShopOffer(null, storedSeller, sellerName, template, count, buyPrice, sellPrice));
        setChanged();
        return true;
    }

    public synchronized boolean deleteOffer(int index) {
        if (index < 0 || index >= offers.size()) {
            return false;
        }
        offers.remove(index);
        setChanged();
        return true;
    }

    public synchronized boolean takeStockForSale(ItemStack sample, int amount) {
        return ShopContainers.removeMatching(stock, sample, amount);
    }

    public synchronized boolean storePurchase(ItemStack goods) {
        boolean stored = ShopContainers.insert(stock, goods);
        if (stored) {
            setChanged();
        }
        return stored;
    }

    public synchronized void recordSaleRevenue(long buyPriceTimesUnits) {
        if (buyPriceTimesUnits <= 0L) {
            return;
        }
        if (lifetimeRevenue > Long.MAX_VALUE - buyPriceTimesUnits) {
            lifetimeRevenue = Long.MAX_VALUE;
        } else {
            lifetimeRevenue += buyPriceTimesUnits;
        }
        setChanged();
    }

    public void dropStock(Level level, BlockPos pos) {
        net.minecraft.world.Containers.dropContents(level, pos, stock);
        stock.clearContent();
        setChanged();
    }

    public Component title() {
        return Component.translatable("container.cointcore.player_trader", ownerName());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (ownerId != null) {
            tag.putUUID("owner_id", ownerId);
        }
        if (ownerName != null && !ownerName.isBlank()) {
            tag.putString("owner_name", ownerName);
        }
        tag.putLong("lifetime_revenue", lifetimeRevenue);
        ListTag items = new ListTag();
        for (int slot = 0; slot < stock.getContainerSize(); slot++) {
            ItemStack stack = stock.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag slotTag = new CompoundTag();
            slotTag.putByte("Slot", (byte) slot);
            items.add(stack.save(registries, slotTag));
        }
        tag.put("stock", items);
        ListTag offerTags = new ListTag();
        for (PlayerShopOffer offer : offers) {
            offerTags.add(offer.save(registries));
        }
        tag.put("offers", offerTags);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ownerId = tag.hasUUID("owner_id") ? tag.getUUID("owner_id") : null;
        ownerName = tag.getString("owner_name");
        lifetimeRevenue = Math.max(0L, tag.getLong("lifetime_revenue"));
        stock.clearContent();
        ListTag items = tag.getList("stock", Tag.TAG_COMPOUND);
        for (int index = 0; index < items.size(); index++) {
            CompoundTag slotTag = items.getCompound(index);
            int slot = slotTag.getByte("Slot") & 255;
            if (slot >= stock.getContainerSize()) {
                continue;
            }
            ItemStack.parse(registries, slotTag).ifPresent(stack -> stock.setItem(slot, stack));
        }
        offers.clear();
        ListTag offerTags = tag.getList("offers", Tag.TAG_COMPOUND);
        for (int index = 0; index < offerTags.size() && offers.size() < MAX_OFFERS; index++) {
            offers.add(PlayerShopOffer.load(offerTags.getCompound(index), registries));
        }
    }
}
