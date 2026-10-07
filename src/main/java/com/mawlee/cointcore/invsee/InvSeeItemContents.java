package com.mawlee.cointcore.invsee;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.util.ArrayList;
import java.util.List;

public final class InvSeeItemContents {
    public static final int MAX_SLOTS = 54;

    private InvSeeItemContents() {
    }

    public static InvSeeNestedKind kind(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return InvSeeNestedKind.NONE;
        }
        boolean backpack = isBackpackItem(stack);
        boolean bundle = stack.has(DataComponents.BUNDLE_CONTENTS) || stack.getItem() instanceof BundleItem;
        boolean container = stack.has(DataComponents.CONTAINER);
        boolean shulker = isShulkerLike(stack);
        return InvSeeNestedKind.classify(backpack, bundle, container, shulker);
    }

    public static boolean isShulkerLike(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = id.getPath();
        return path.contains("shulker");
    }

    public static boolean isBackpackItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        try {
            Class<?> backpackItem = Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem");
            return backpackItem.isInstance(stack.getItem());
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
            String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            return path.contains("backpack");
        }
    }

    public static List<ItemStack> view(ItemStack stack) {
        InvSeeNestedKind kind = kind(stack);
        return switch (kind) {
            case NONE, BACKPACK -> List.of();
            case BUNDLE -> viewBundle(stack);
            case CONTAINER -> viewContainer(stack);
        };
    }

    public static int slotCount(ItemStack stack) {
        InvSeeNestedKind kind = kind(stack);
        return switch (kind) {
            case NONE, BACKPACK -> 0;
            case BUNDLE -> Math.max(viewBundle(stack).size(), 1);
            case CONTAINER -> {
                ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
                if (contents != null && contents.getSlots() > 0) {
                    yield Math.min(MAX_SLOTS, contents.getSlots());
                }
                yield 27;
            }
        };
    }

    public static void write(ItemStack stack, List<ItemStack> items) {
        InvSeeNestedKind kind = kind(stack);
        switch (kind) {
            case NONE, BACKPACK -> {
            }
            case BUNDLE -> stack.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(copyNonEmpty(items)));
            case CONTAINER -> stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(copyAll(items)));
        }
    }

    private static List<ItemStack> viewContainer(ItemStack stack) {
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents == null) {
            return List.of();
        }
        List<ItemStack> items = new ArrayList<>(contents.getSlots());
        for (int i = 0; i < contents.getSlots(); i++) {
            items.add(contents.getStackInSlot(i).copy());
        }
        return items;
    }

    private static List<ItemStack> viewBundle(ItemStack stack) {
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) {
            return List.of();
        }
        List<ItemStack> items = new ArrayList<>();
        contents.itemsCopy().forEach(items::add);
        return items;
    }

    private static List<ItemStack> copyNonEmpty(List<ItemStack> items) {
        List<ItemStack> copy = new ArrayList<>();
        for (ItemStack item : items) {
            if (item != null && !item.isEmpty()) {
                copy.add(item.copy());
            }
        }
        return copy;
    }

    private static List<ItemStack> copyAll(List<ItemStack> items) {
        List<ItemStack> copy = new ArrayList<>(items.size());
        for (ItemStack item : items) {
            copy.add(item == null || item.isEmpty() ? ItemStack.EMPTY : item.copy());
        }
        return copy;
    }
}
