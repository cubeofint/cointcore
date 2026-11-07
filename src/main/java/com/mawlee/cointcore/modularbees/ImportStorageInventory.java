package com.mawlee.cointcore.modularbees;

import com.glodblock.github.modularbees.common.inventory.MBBigItemInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

/** The import hatch's buffer, sized per slot by the hatch's amounts instead of ModularBees' fixed 8 stacks. */
public final class ImportStorageInventory extends MBBigItemInventory {
    private final ImportAmountHost amounts;

    public ImportStorageInventory(BlockEntity host, int multiplier, int size, ImportAmountHost amounts) {
        super(host, multiplier, size);
        this.amounts = amounts;
    }

    @Override
    public int getSlotLimit(int slot) {
        return Integer.MAX_VALUE;
    }

    @Override
    protected int getStackLimit(int slot, @NotNull ItemStack stack) {
        return amounts.cointcore$target(slot, stack);
    }
}
