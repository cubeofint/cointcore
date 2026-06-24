package com.mawlee.cointcore.spawner;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class SpawnerLootCapture {
    private static final ThreadLocal<List<ItemStack>> CAPTURED = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

    private SpawnerLootCapture() {
    }

    public static boolean isActive() {
        return ACTIVE.get();
    }

    public static void begin() {
        CAPTURED.get().clear();
        ACTIVE.set(true);
    }

    public static void capture(ItemStack stack) {
        if (!stack.isEmpty()) {
            CAPTURED.get().add(stack.copy());
        }
    }

    public static void captureItemEntities(Collection<ItemEntity> drops) {
        for (ItemEntity itemEntity : drops) {
            if (itemEntity != null) {
                capture(itemEntity.getItem());
            }
        }
    }

    public static List<ItemStack> end() {
        ACTIVE.set(false);
        List<ItemStack> result = new ArrayList<>(CAPTURED.get());
        CAPTURED.get().clear();
        return result;
    }
}
