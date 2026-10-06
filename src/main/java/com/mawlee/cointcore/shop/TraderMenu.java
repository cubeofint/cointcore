package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.config.TraderOffersConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Trader UI: offer ghost slots, player inventory, synced gluon balance.
 * Left-click an offer to buy (price + commission). Right-click to sell (price − commission).
 */
public class TraderMenu extends AbstractContainerMenu {
    public static final int CONTAINER_ROWS = 3;
    public static final int OFFER_SLOTS = CONTAINER_ROWS * 9;
    private static final int BALANCE_SHORTS = 4;

    private final ContainerLevelAccess access;
    private final List<TraderOffer> offers;
    private final SimpleContainer offerContainer;
    private long gluonBalance;
    private final int[] balanceParts = new int[BALANCE_SHORTS];

    public TraderMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL, 0L, List.of());
    }

    public TraderMenu(
            int containerId,
            Inventory playerInventory,
            ContainerLevelAccess access,
            long gluonBalance,
            List<TraderOffer> offers
    ) {
        super(ShopMenus.TRADER.get(), containerId);
        this.access = access;
        this.offers = List.copyOf(offers);
        this.offerContainer = new SimpleContainer(OFFER_SLOTS);
        this.gluonBalance = Math.max(0L, gluonBalance);
        writeBalanceParts(this.gluonBalance);
        fillOfferSlots();

        for (int row = 0; row < CONTAINER_ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9;
                addSlot(new OfferSlot(offerContainer, index, 8 + col * 18, 18 + row * 18));
            }
        }

        int playerInvY = 84 + (CONTAINER_ROWS - 3) * 18;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerInvY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, playerInvY + 58));
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
                    TraderMenu.this.gluonBalance = readBalanceParts();
                }
            });
        }
    }

    public static TraderMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        long balance = buffer.readableBytes() >= Long.BYTES ? buffer.readLong() : 0L;
        List<TraderOffer> offers = new ArrayList<>();
        if (buffer.readableBytes() > 0) {
            int count = ByteBufCodecs.VAR_INT.decode(buffer);
            for (int index = 0; index < count && index < OFFER_SLOTS; index++) {
                ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
                long buyPrice = buffer.readLong();
                long sellPrice = buffer.readLong();
                long buyFee = buffer.readLong();
                long buyTotal = buffer.readLong();
                long sellFee = buffer.readLong();
                long sellNet = buffer.readLong();
                String id = ByteBufCodecs.STRING_UTF8.decode(buffer);
                offers.add(new TraderOffer(id, stack, Math.max(1, stack.getCount()), buyPrice, sellPrice, buyFee, buyTotal, sellFee, sellNet));
            }
        }
        return new TraderMenu(containerId, playerInventory, ContainerLevelAccess.NULL, balance, offers);
    }

    public static void writeOpenData(RegistryFriendlyByteBuf buffer, long balance, List<TraderOffer> offers) {
        buffer.writeLong(balance);
        List<TraderOffer> limited = offers.size() > OFFER_SLOTS ? offers.subList(0, OFFER_SLOTS) : offers;
        ByteBufCodecs.VAR_INT.encode(buffer, limited.size());
        for (TraderOffer offer : limited) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, offer.display());
            buffer.writeLong(offer.buyPrice());
            buffer.writeLong(offer.sellPrice());
            buffer.writeLong(offer.buyFee());
            buffer.writeLong(offer.buyTotal());
            buffer.writeLong(offer.sellFee());
            buffer.writeLong(offer.sellNet());
            ByteBufCodecs.STRING_UTF8.encode(buffer, offer.id());
        }
    }

    public static List<TraderOffer> serverOffers() {
        return TraderOffersConfig.offers();
    }

    public List<TraderOffer> offers() {
        return offers;
    }

    public long gluonBalance() {
        return gluonBalance;
    }

    public void refreshBalance(long balance) {
        gluonBalance = Math.max(0L, balance);
        writeBalanceParts(gluonBalance);
        broadcastChanges();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < offers.size()) {
            if (player instanceof ServerPlayer serverPlayer && (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE)) {
                if (button == 1) {
                    TraderTrades.sell(serverPlayer, this, slotId);
                } else {
                    TraderTrades.buy(serverPlayer, this, slotId);
                }
            }
            return;
        }
        if (slotId >= 0 && slotId < OFFER_SLOTS) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ShopBlocks.TRADER.get());
    }

    private void fillOfferSlots() {
        for (int index = 0; index < Math.min(offers.size(), OFFER_SLOTS); index++) {
            offerContainer.setItem(index, offers.get(index).display().copy());
        }
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

    private static final class OfferSlot extends Slot {
        OfferSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
