package com.mawlee.cointcore.shop;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Customer (and owner-as-customer) view of a player vending machine.
 * Same layout as {@link TraderMenu}; no price-history sparkline.
 */
public class PlayerTraderMenu extends AbstractContainerMenu {
    public static final int PAGE_SIZE = 3;
    public static final int GUI_WIDTH = 360;
    public static final int VANILLA_INV_WIDTH = TraderMenu.VANILLA_INV_WIDTH;
    public static final int ROW_HEIGHT = 36;
    public static final int TITLE_HEIGHT = 52;
    public static final int STATUS_HEIGHT = TraderMenu.STATUS_HEIGHT;
    public static final int PAGE_BAR_HEIGHT = TraderMenu.PAGE_BAR_HEIGHT;
    public static final int OFFER_PANEL_HEIGHT =
            TITLE_HEIGHT + PAGE_SIZE * ROW_HEIGHT + STATUS_HEIGHT + PAGE_BAR_HEIGHT;
    public static final int PLAYER_INV_Y = OFFER_PANEL_HEIGHT + 14;
    public static final int PLAYER_INV_LEFT = (GUI_WIDTH - VANILLA_INV_WIDTH) / 2 + 8;
    public static final int GUI_HEIGHT = PLAYER_INV_Y + 82;
    private static final int BALANCE_SHORTS = 4;
    private static final int MAX_SYNCED_OFFERS = 512;

    private final ContainerLevelAccess access;
    private final boolean canManage;
    private String ownerName;
    private List<PlayerTraderListing> listings;
    private long gluonBalance;
    private final int[] balanceParts = new int[BALANCE_SHORTS];

    public PlayerTraderMenu(
            int containerId,
            Inventory playerInventory,
            ContainerLevelAccess access,
            long gluonBalance,
            String ownerName,
            boolean canManage,
            List<PlayerTraderListing> listings
    ) {
        super(ShopMenus.PLAYER_TRADER.get(), containerId);
        this.access = access;
        this.canManage = canManage;
        this.ownerName = ownerName == null ? "" : ownerName;
        this.listings = List.copyOf(listings);
        this.gluonBalance = Math.max(0L, gluonBalance);
        writeBalanceParts(this.gluonBalance);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(
                        playerInventory,
                        col + row * 9 + 9,
                        PLAYER_INV_LEFT + col * 18,
                        PLAYER_INV_Y + row * 18
                ));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, PLAYER_INV_LEFT + col * 18, PLAYER_INV_Y + 58));
        }

        for (int index = 0; index < BALANCE_SHORTS; index++) {
            final int partIndex = index;
            addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return balanceParts[partIndex];
                }

                @Override
                public void set(int value) {
                    balanceParts[partIndex] = value & 0xFFFF;
                    PlayerTraderMenu.this.gluonBalance = readBalanceParts();
                }
            });
        }
    }

    public static PlayerTraderMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        long balance = buffer.readLong();
        String ownerName = ByteBufCodecs.STRING_UTF8.decode(buffer);
        boolean canManage = buffer.readBoolean();
        return new PlayerTraderMenu(
                containerId,
                playerInventory,
                ContainerLevelAccess.NULL,
                balance,
                ownerName,
                canManage,
                readListings(buffer)
        );
    }

    public static void writeOpenData(
            RegistryFriendlyByteBuf buffer,
            long balance,
            String ownerName,
            boolean canManage,
            List<PlayerTraderListing> listings
    ) {
        buffer.writeLong(balance);
        ByteBufCodecs.STRING_UTF8.encode(buffer, ownerName == null ? "" : ownerName);
        buffer.writeBoolean(canManage);
        writeListings(buffer, listings);
    }

    static void writeListings(RegistryFriendlyByteBuf buffer, List<PlayerTraderListing> listings) {
        List<PlayerTraderListing> limited =
                listings.size() > MAX_SYNCED_OFFERS ? listings.subList(0, MAX_SYNCED_OFFERS) : listings;
        ByteBufCodecs.VAR_INT.encode(buffer, limited.size());
        for (PlayerTraderListing listing : limited) {
            TraderOffer offer = listing.offer();
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, offer.display());
            buffer.writeLong(offer.buyPrice());
            buffer.writeLong(offer.sellPrice());
            buffer.writeLong(offer.buyFee());
            buffer.writeLong(offer.buyTotal());
            buffer.writeLong(offer.sellFee());
            buffer.writeLong(offer.sellNet());
            ByteBufCodecs.STRING_UTF8.encode(buffer, listing.listingId() == null || listing.listingId().isBlank()
                    ? offer.id()
                    : listing.listingId());
            ByteBufCodecs.VAR_INT.encode(buffer, listing.dealsLeft());
            ByteBufCodecs.STRING_UTF8.encode(buffer, listing.sellerName() == null ? "" : listing.sellerName());
            ByteBufCodecs.STRING_UTF8.encode(buffer, listing.sellerId() == null ? "" : listing.sellerId());
            buffer.writeLong(listing.createdAt());
            ByteBufCodecs.STRING_UTF8.encode(buffer, listing.modId() == null ? "" : listing.modId());
        }
    }

    static List<PlayerTraderListing> readListings(RegistryFriendlyByteBuf buffer) {
        List<PlayerTraderListing> listings = new ArrayList<>();
        if (buffer.readableBytes() <= 0) {
            return listings;
        }
        int count = ByteBufCodecs.VAR_INT.decode(buffer);
        for (int index = 0; index < count && index < MAX_SYNCED_OFFERS; index++) {
            ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
            long buyPrice = buffer.readLong();
            long sellPrice = buffer.readLong();
            long buyFee = buffer.readLong();
            long buyTotal = buffer.readLong();
            long sellFee = buffer.readLong();
            long sellNet = buffer.readLong();
            String id = ByteBufCodecs.STRING_UTF8.decode(buffer);
            int deals = ByteBufCodecs.VAR_INT.decode(buffer);
            String sellerName = ByteBufCodecs.STRING_UTF8.decode(buffer);
            String sellerId = ByteBufCodecs.STRING_UTF8.decode(buffer);
            long createdAt = buffer.readLong();
            String modId = ByteBufCodecs.STRING_UTF8.decode(buffer);
            TraderOffer offer = new TraderOffer(
                    id,
                    stack,
                    Math.max(1, stack.getCount()),
                    buyPrice,
                    sellPrice,
                    buyFee,
                    buyTotal,
                    sellFee,
                    sellNet,
                    new long[0]
            );
            listings.add(new PlayerTraderListing(id, offer, Math.max(0, deals), sellerName, sellerId, createdAt, modId));
        }
        return listings;
    }

    ContainerLevelAccess access() {
        return access;
    }

    public List<PlayerTraderListing> listings() {
        return listings;
    }

    public String ownerName() {
        return ownerName;
    }

    public boolean canManage() {
        return canManage;
    }

    public long gluonBalance() {
        return gluonBalance;
    }

    public void refresh(long balance, String ownerName, List<PlayerTraderListing> listings) {
        this.gluonBalance = Math.max(0L, balance);
        writeBalanceParts(this.gluonBalance);
        this.ownerName = ownerName == null ? "" : ownerName;
        this.listings = List.copyOf(listings);
        broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ShopBlocks.PLAYER_TRADER.get());
    }

    private void writeBalanceParts(long balance) {
        long value = Math.max(0L, balance);
        for (int index = 0; index < BALANCE_SHORTS; index++) {
            balanceParts[index] = (int) ((value >>> (index * 16)) & 0xFFFFL);
        }
    }

    private long readBalanceParts() {
        long value = 0L;
        for (int index = 0; index < BALANCE_SHORTS; index++) {
            value |= ((long) (balanceParts[index] & 0xFFFF)) << (index * 16);
        }
        return value;
    }
}
