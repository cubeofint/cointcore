package com.mawlee.cointcore.shop;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Seller «Мои лоты»: ghost sample slot plus the player's inventory. No machine stock.
 */
public class PlayerTraderManageMenu extends AbstractContainerMenu {
    public static final int GHOST_SLOT_INDEX = 0;
    public static final int GUI_WIDTH = PlayerTraderManageLayout.GUI_WIDTH;
    public static final int TITLE_HEIGHT = PlayerTraderManageLayout.TITLE_Y;
    public static final int GHOST_SLOT = PlayerTraderManageLayout.GHOST_X;
    public static final int PLAYER_INV_Y = PlayerTraderManageLayout.PLAYER_INV_Y;
    public static final int GUI_HEIGHT = PlayerTraderManageLayout.GUI_HEIGHT;

    private final ContainerLevelAccess access;
    private final SimpleContainer ghost;
    private String ownerName;
    private long lifetimeRevenue;
    private List<PlayerShopOfferSnapshot> offers;
    private int returnCount;
    private List<MarketPriceHint> priceHints;

    public PlayerTraderManageMenu(
            int containerId,
            Inventory playerInventory,
            ContainerLevelAccess access,
            String ownerName,
            long lifetimeRevenue,
            List<PlayerShopOfferSnapshot> offers,
            int returnCount,
            List<MarketPriceHint> priceHints
    ) {
        super(ShopMenus.PLAYER_TRADER_MANAGE.get(), containerId);
        this.access = access;
        this.ghost = new SimpleContainer(1);
        this.ownerName = ownerName == null ? "" : ownerName;
        this.lifetimeRevenue = Math.max(0L, lifetimeRevenue);
        this.offers = List.copyOf(offers);
        this.returnCount = Math.max(0, returnCount);
        this.priceHints = priceHints == null ? List.of() : List.copyOf(priceHints);

        addSlot(new Slot(ghost, 0, PlayerTraderManageLayout.GHOST_X, PlayerTraderManageLayout.GHOST_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !stack.isEmpty();
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(
                        playerInventory,
                        col + row * 9 + 9,
                        PlayerTraderManageLayout.playerSlotX(col),
                        PlayerTraderManageLayout.playerInvRowY(row)
                ));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(
                    playerInventory,
                    col,
                    PlayerTraderManageLayout.playerSlotX(col),
                    PlayerTraderManageLayout.PLAYER_HOTBAR_Y
            ));
        }
    }

    public static PlayerTraderManageMenu fromNetwork(
            int containerId,
            Inventory playerInventory,
            RegistryFriendlyByteBuf buffer
    ) {
        String ownerName = ByteBufCodecs.STRING_UTF8.decode(buffer);
        long revenue = buffer.readLong();
        int returns = ByteBufCodecs.VAR_INT.decode(buffer);
        List<PlayerShopOfferSnapshot> offers = PlayerShopOfferSnapshot.readList(buffer);
        List<MarketPriceHint> hints = MarketPriceHint.readList(buffer);
        return new PlayerTraderManageMenu(
                containerId,
                playerInventory,
                ContainerLevelAccess.NULL,
                ownerName,
                revenue,
                offers,
                returns,
                hints
        );
    }

    public static void writeOpenData(
            RegistryFriendlyByteBuf buffer,
            String ownerName,
            long revenue,
            List<PlayerShopOfferSnapshot> offers,
            int returnCount,
            List<MarketPriceHint> priceHints
    ) {
        ByteBufCodecs.STRING_UTF8.encode(buffer, ownerName == null ? "" : ownerName);
        buffer.writeLong(revenue);
        ByteBufCodecs.VAR_INT.encode(buffer, returnCount);
        PlayerShopOfferSnapshot.writeList(buffer, offers);
        MarketPriceHint.writeList(buffer, priceHints);
    }

    ContainerLevelAccess access() {
        return access;
    }

    public String ownerName() {
        return ownerName;
    }

    public long lifetimeRevenue() {
        return lifetimeRevenue;
    }

    public int returnCount() {
        return returnCount;
    }

    public List<MarketPriceHint> priceHints() {
        return priceHints;
    }

    public long recommendedUnitPrice(ItemStack stack) {
        return MarketPriceHint.lookup(priceHints, GlobalMarketService.itemKey(stack));
    }

    public List<PlayerShopOfferSnapshot> offers() {
        return offers;
    }

    public ItemStack ghostItem() {
        return ghost.getItem(0);
    }

    public void setGhost(ItemStack stack) {
        ghost.setItem(0, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
    }

    public void refresh(
            String ownerName,
            long revenue,
            List<PlayerShopOfferSnapshot> offers,
            int returnCount,
            List<MarketPriceHint> priceHints
    ) {
        this.ownerName = ownerName == null ? "" : ownerName;
        this.lifetimeRevenue = Math.max(0L, revenue);
        this.offers = List.copyOf(offers);
        this.returnCount = Math.max(0, returnCount);
        this.priceHints = priceHints == null ? List.of() : List.copyOf(priceHints);
        broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player)) {
            return ItemStack.EMPTY;
        }
        if (index == GHOST_SLOT_INDEX) {
            return ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (!stillValid(player)) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ShopBlocks.PLAYER_TRADER.get())
                && (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                || PlayerShopAccess.canUse(serverPlayer));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && !ghost.getItem(0).isEmpty()) {
            player.getInventory().placeItemBackInInventory(ghost.getItem(0));
            ghost.setItem(0, ItemStack.EMPTY);
        }
    }
}
