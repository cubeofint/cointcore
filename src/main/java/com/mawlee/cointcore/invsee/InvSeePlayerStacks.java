package com.mawlee.cointcore.invsee;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class InvSeePlayerStacks {
    private InvSeePlayerStacks() {
    }

    public record LocatedStack(String handlerName, String identifier, int slotIndex, ItemStack stack) {
    }

    public static List<LocatedStack> collect(Player target, Predicate<ItemStack> filter) {
        List<LocatedStack> stacks = new ArrayList<>();
        String handlerName = target.getGameProfile().getName();

        for (int slot = 0; slot < target.getInventory().getContainerSize(); slot++) {
            ItemStack stack = target.getInventory().getItem(slot);
            if (filter.test(stack)) {
                stacks.add(new LocatedStack(handlerName, "", slot, stack));
            }
        }

        CuriosApi.getCuriosInventory(target).ifPresent(handler -> handler.getCurios().forEach((identifier, stacksHandler) -> {
            if (stacksHandler.getSlots() <= 0) {
                return;
            }
            for (int slot = 0; slot < stacksHandler.getStacks().getSlots(); slot++) {
                ItemStack stack = stacksHandler.getStacks().getStackInSlot(slot);
                if (filter.test(stack)) {
                    stacks.add(new LocatedStack(handlerName, identifier, slot, stack));
                }
            }
        }));

        return stacks;
    }

    public static String encodeLocation(LocatedStack locatedStack) {
        return locatedStack.handlerName() + '|' + locatedStack.identifier() + '|' + locatedStack.slotIndex();
    }

    public static LocatedStack decodeLocation(Player target, String key) {
        String[] parts = key.split("\\|", 3);
        if (parts.length != 3) {
            return null;
        }

        int slotIndex;
        try {
            slotIndex = Integer.parseInt(parts[2]);
        } catch (NumberFormatException exception) {
            return null;
        }

        String handlerName = parts[0];
        String identifier = parts[1];
        ItemStack stack = resolveStack(target, handlerName, identifier, slotIndex);
        if (stack.isEmpty()) {
            return null;
        }
        return new LocatedStack(handlerName, identifier, slotIndex, stack);
    }

    public static ItemStack resolveStack(Player target, String handlerName, String identifier, int slotIndex) {
        if (!handlerName.equals(target.getGameProfile().getName())) {
            return ItemStack.EMPTY;
        }

        if (identifier.isEmpty()) {
            if (slotIndex < 0 || slotIndex >= target.getInventory().getContainerSize()) {
                return ItemStack.EMPTY;
            }
            return target.getInventory().getItem(slotIndex);
        }

        ItemStack[] resolved = new ItemStack[] {ItemStack.EMPTY};
        CuriosApi.getCuriosInventory(target).ifPresent(handler -> {
            var stacksHandler = handler.getCurios().get(identifier);
            if (stacksHandler == null || slotIndex < 0 || slotIndex >= stacksHandler.getStacks().getSlots()) {
                return;
            }
            resolved[0] = stacksHandler.getStacks().getStackInSlot(slotIndex);
        });
        return resolved[0];
    }
}
