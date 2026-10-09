package com.mawlee.cointcore.shop;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Owner management: 27 real stock slots plus a ghost template slot (not persisted).
 */
public class PlayerTraderManageMenu extends AbstractContainerMenu {
    public static final int STOCK_SIZE = PlayerTraderBlockEntity.STOCK_SIZE;
    public static final int GUI_WIDTH = 176;
    public static final int TITLE_HEIGHT = 28;
    public static final int STOCK_Y = 36;
    public static final int EDITOR_Y = STOCK_Y + 54 + 6;
    public static final int GHOST_SLOT_INDEX = STOCK_SIZE;
    public static final int PLAYER_INV_Y = EDITOR_Y + 48;
    public static final int GUI_HEIGHT = PLAYER_INV_Y + 82;

    private final ContainerLevelAccess access;
    private final Container stock;
    private final SimpleContainer ghost;
    private String ownerName;
    private long lifetimeRevenue;
    private List<PlayerShopOfferSnapshot> offers;

    public PlayerTraderManageMenu(
            int containerId,
            Inventory playerInventory,
            ContainerLevelAccess access,
            Container stock,
            String ownerName,
            long lifetimeRevenue,
            List<PlayerShopOfferSnapshot> offers
    ) {
        super(ShopMenus.PLAYER_TRADER_MANAGE.get(), containerId);
        this.access = access;
        this.stock = stock;
        this.ghost = new SimpleContainer(1);
        this.ownerName = ownerName == null ? "" : ownerName;
        this.lifetimeRevenue = Math.max(0L, lifetimeRevenue);
        this.offers = List.copyOf(offers);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(stock, col + row * 9, 8 + col * 18, STOCK_Y + row * 18));
            }
        }
        addSlot(new Slot(ghost, 0, 8, EDITOR_Y + 14) {
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
                        8 + col * 18,
                        PLAYER_INV_Y + row * 18
                ));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, PLAYER_INV_Y + 58));
        }
    }

    public static PlayerTraderManageMenu fromNetwork(
            int containerId,
            Inventory playerInventory,
            RegistryFriendlyByteBuf buffer
    ) {
        String ownerName = ByteBufCodecs.STRING_UTF8.decode(buffer);
        long revenue = buffer.readLong();
        List<PlayerShopOfferSnapshot> offers = PlayerShopOfferSnapshot.readList(buffer);
        return new PlayerTraderManageMenu(
                containerId,
                playerInventory,
                ContainerLevelAccess.NULL,
                new SimpleContainer(STOCK_SIZE),
                ownerName,
                revenue,
                offers
        );
    }

    public static void writeOpenData(
            RegistryFriendlyByteBuf buffer,
            String ownerName,
            long revenue,
            List<PlayerShopOfferSnapshot> offers
    ) {
        ByteBufCodecs.STRING_UTF8.encode(buffer, ownerName == null ? "" : ownerName);
        buffer.writeLong(revenue);
        PlayerShopOfferSnapshot.writeList(buffer, offers);
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

    public List<PlayerShopOfferSnapshot> offers() {
        return offers;
    }

    public ItemStack ghostItem() {
        return ghost.getItem(0);
    }

    public void setGhost(ItemStack stack) {
        ghost.setItem(0, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
    }

    public void refresh(String ownerName, long revenue, List<PlayerShopOfferSnapshot> offers) {
        this.ownerName = ownerName == null ? "" : ownerName;
        this.lifetimeRevenue = Math.max(0L, revenue);
        this.offers = List.copyOf(offers);
        broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int playerStart = STOCK_SIZE + 1;
        if (index < STOCK_SIZE) {
            if (!moveItemStackTo(stack, playerStart, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (index == GHOST_SLOT_INDEX) {
            return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, STOCK_SIZE, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ShopBlocks.PLAYER_TRADER.get())
                && access.evaluate(
                (level, pos) -> level.getBlockEntity(pos) instanceof PlayerTraderBlockEntity shop
                        && shop.canManage(player),
                false
        );
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
