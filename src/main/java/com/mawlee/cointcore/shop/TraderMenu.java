package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.config.TraderOffersConfig;
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
 * Trader container: player inventory plus synced gluon balance and offer catalog.
 * Deals are requested with {@link TraderTradePayload}, not slot clicks.
 */
public class TraderMenu extends AbstractContainerMenu {
    public static final int PAGE_SIZE = 5;
    public static final int GUI_WIDTH = 256;
    public static final int VANILLA_INV_WIDTH = 176;
    public static final int ROW_HEIGHT = 40;
    public static final int TITLE_HEIGHT = 30;
    public static final int STATUS_HEIGHT = 12;
    public static final int PAGE_BAR_HEIGHT = 18;
    public static final int OFFER_PANEL_HEIGHT =
            TITLE_HEIGHT + PAGE_SIZE * ROW_HEIGHT + STATUS_HEIGHT + PAGE_BAR_HEIGHT;
    public static final int PLAYER_INV_Y = OFFER_PANEL_HEIGHT + 14;
    public static final int PLAYER_INV_LEFT = (GUI_WIDTH - VANILLA_INV_WIDTH) / 2 + 8;
    public static final int GUI_HEIGHT = PLAYER_INV_Y + 82;
    private static final int BALANCE_SHORTS = 4;
    private static final int MAX_SYNCED_OFFERS = 512;

    private final ContainerLevelAccess access;
    private final List<TraderOffer> offers;
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
            for (int index = 0; index < count && index < MAX_SYNCED_OFFERS; index++) {
                ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
                long buyPrice = buffer.readLong();
                long sellPrice = buffer.readLong();
                long buyFee = buffer.readLong();
                long buyTotal = buffer.readLong();
                long sellFee = buffer.readLong();
                long sellNet = buffer.readLong();
                String id = ByteBufCodecs.STRING_UTF8.decode(buffer);
                offers.add(new TraderOffer(
                        id,
                        stack,
                        Math.max(1, stack.getCount()),
                        buyPrice,
                        sellPrice,
                        buyFee,
                        buyTotal,
                        sellFee,
                        sellNet
                ));
            }
        }
        return new TraderMenu(containerId, playerInventory, ContainerLevelAccess.NULL, balance, offers);
    }

    public static void writeOpenData(RegistryFriendlyByteBuf buffer, long balance, List<TraderOffer> offers) {
        buffer.writeLong(balance);
        List<TraderOffer> limited = offers.size() > MAX_SYNCED_OFFERS ? offers.subList(0, MAX_SYNCED_OFFERS) : offers;
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
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ShopBlocks.TRADER.get());
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
