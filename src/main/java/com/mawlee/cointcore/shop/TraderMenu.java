package com.mawlee.cointcore.shop;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Trader UI: player inventory plus synced gluon balance. No offers yet.
 */
public class TraderMenu extends AbstractContainerMenu {
    /** One empty chest row above the player inventory (visual placeholder). */
    public static final int CONTAINER_ROWS = 1;
    private static final int BALANCE_SHORTS = 4;

    private final ContainerLevelAccess access;
    private long gluonBalance;
    private final int[] balanceParts = new int[BALANCE_SHORTS];

    public TraderMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL, 0L);
    }

    public TraderMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access, long gluonBalance) {
        super(ShopMenus.TRADER.get(), containerId);
        this.access = access;
        this.gluonBalance = Math.max(0L, gluonBalance);
        writeBalanceParts(this.gluonBalance);

        int playerInvY = 103 + (CONTAINER_ROWS - 4) * 18;
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
        return new TraderMenu(containerId, playerInventory, ContainerLevelAccess.NULL, balance);
    }

    public long gluonBalance() {
        return gluonBalance;
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
