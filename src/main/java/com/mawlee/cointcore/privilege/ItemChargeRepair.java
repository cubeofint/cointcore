package com.mawlee.cointcore.privilege;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class ItemChargeRepair {
    private static final int ENERGY_FILL_GUARD = 64;

    private ItemChargeRepair() {
    }

    public static boolean canRepair(ItemStack stack) {
        return !stack.isEmpty() && stack.isDamageableItem() && stack.isDamaged();
    }

    public static boolean repair(ItemStack stack) {
        if (!canRepair(stack)) {
            return false;
        }
        stack.setDamageValue(0);
        return true;
    }

    public static ChargeResult charge(ItemStack stack) {
        if (stack.isEmpty()) {
            return ChargeResult.NO_STORAGE;
        }
        IEnergyStorage storage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (storage == null || !storage.canReceive()) {
            return ChargeResult.NO_STORAGE;
        }
        if (storage.getEnergyStored() >= storage.getMaxEnergyStored()) {
            return ChargeResult.ALREADY_FULL;
        }
        boolean receivedAny = false;
        for (int i = 0; i < ENERGY_FILL_GUARD; i++) {
            int space = storage.getMaxEnergyStored() - storage.getEnergyStored();
            if (space <= 0) {
                return ChargeResult.CHARGED;
            }
            int received = storage.receiveEnergy(space, false);
            if (received <= 0) {
                break;
            }
            receivedAny = true;
        }
        if (!receivedAny) {
            return ChargeResult.NO_STORAGE;
        }
        return ChargeResult.CHARGED;
    }

    public enum ChargeResult {
        CHARGED,
        ALREADY_FULL,
        NO_STORAGE
    }
}
