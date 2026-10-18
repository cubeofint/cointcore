package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.config.TraderOffersConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Player-owned vending machine. Does not implement {@link net.minecraft.world.Container}.
 * Pipes see an insert-only {@link IItemHandler} that restocks the owner's global-market lots.
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
    private boolean marketMigrated;
    private boolean autoPull;
    private final PlayerTraderRestockHandler restockHandler = new PlayerTraderRestockHandler(this);

    public PlayerTraderBlockEntity(BlockPos pos, BlockState state) {
        super(ShopBlockEntities.PLAYER_TRADER.get(), pos, state);
    }

    public PlayerTraderRestockHandler restockHandler() {
        return restockHandler;
    }

    public boolean autoPull() {
        return autoPull;
    }

    public void setAutoPull(boolean autoPull) {
        this.autoPull = autoPull;
        setChanged();
    }

    public boolean toggleAutoPull() {
        setAutoPull(!autoPull);
        return autoPull;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public String ownerName() {
        return ownerName == null || ownerName.isBlank() ? "?" : ownerName;
    }

    public void setOwner(UUID ownerId, String ownerName) {
        String name = ownerName == null ? "" : ownerName;
        if (Objects.equals(this.ownerId, ownerId) && Objects.equals(this.ownerName, name)) {
            return;
        }
        this.ownerId = ownerId;
        this.ownerName = name;
        setChanged();
        syncOwnerToClients();
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

    public boolean marketMigrated() {
        return marketMigrated;
    }

    public void markMarketMigrated() {
        marketMigrated = true;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PlayerTraderBlockEntity shop) {
        if (!(level instanceof ServerLevel) || !shop.autoPull || shop.ownerId == null) {
            return;
        }
        if (level.getGameTime() % 10L != 0L) {
            return;
        }
        shop.pullFromAdjacent();
    }

    private void pullFromAdjacent() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        BlockPos origin = getBlockPos();
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = origin.relative(direction);
            IItemHandler source = level.getCapability(
                    Capabilities.ItemHandler.BLOCK,
                    neighbor,
                    direction.getOpposite()
            );
            if (source == null || source == restockHandler) {
                continue;
            }
            for (int slot = 0; slot < source.getSlots(); slot++) {
                ItemStack simulated = source.extractItem(slot, 64, true);
                if (simulated.isEmpty()) {
                    continue;
                }
                int accepted = GlobalMarketService.tryRestock(this, simulated, true);
                if (accepted < 1) {
                    continue;
                }
                ItemStack taken = source.extractItem(slot, accepted, false);
                if (taken.isEmpty()) {
                    continue;
                }
                int committed = GlobalMarketService.tryRestock(this, taken, false);
                if (committed >= taken.getCount()) {
                    continue;
                }
                ItemStack leftover = taken.copy();
                leftover.shrink(committed);
                ItemStack rejected = leftover;
                for (int back = 0; back < source.getSlots() && !rejected.isEmpty(); back++) {
                    rejected = source.insertItem(back, rejected, false);
                }
            }
        }
    }

    public synchronized void clearOffersAfterMigration() {
        offers.clear();
        setChanged();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() != null && !getLevel().isClientSide && getLevel().getServer() != null) {
            GlobalMarketMigration.migrate(getLevel().getServer(), this);
        }
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

    /**
     * Leftover local stock goes to the owner's market return box so it is not
     * stolen from the floor and not duplicated with global-market escrow.
     * Global listings stay listed; gluons are already in the wallet.
     */
    public void returnContentsToOwner(Level level) {
        if (level == null || level.isClientSide) {
            return;
        }
        if (ownerId == null || level.getServer() == null) {
            Containers.dropContents(level, worldPosition, stock);
            stock.clearContent();
            setChanged();
            return;
        }
        GlobalMarketSavedData data = GlobalMarketSavedData.get(level.getServer());
        for (int slot = 0; slot < stock.getContainerSize(); slot++) {
            ItemStack stack = stock.getItem(slot);
            if (!stack.isEmpty()) {
                data.addReturn(ownerId, stack.copy());
            }
        }
        stock.clearContent();
        setChanged();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (ownerId != null) {
            tag.putUUID("owner_id", ownerId);
        }
        if (ownerName != null && !ownerName.isBlank()) {
            tag.putString("owner_name", ownerName);
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        ownerId = tag.hasUUID("owner_id") ? tag.getUUID("owner_id") : null;
        ownerName = tag.contains("owner_name") ? tag.getString("owner_name") : "";
    }

    private void syncOwnerToClients() {
        if (level instanceof ServerLevel serverLevel) {
            BlockState state = getBlockState();
            serverLevel.sendBlockUpdated(worldPosition, state, state, 3);
        }
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
        tag.putBoolean("market_migrated", marketMigrated);
        tag.putBoolean("auto_pull", autoPull);
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
        marketMigrated = tag.getBoolean("market_migrated");
        autoPull = tag.getBoolean("auto_pull");
    }
}
