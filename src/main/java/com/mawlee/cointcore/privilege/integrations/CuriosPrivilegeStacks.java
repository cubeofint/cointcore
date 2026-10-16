package com.mawlee.cointcore.privilege.integrations;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.function.Consumer;

public final class CuriosPrivilegeStacks {
    private CuriosPrivilegeStacks() {
    }

    public static void forEach(Player player, Consumer<ItemStack> consumer) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            for (ICurioStacksHandler stacksHandler : handler.getCurios().values()) {
                visit(stacksHandler.getStacks(), consumer);
                visit(stacksHandler.getCosmeticStacks(), consumer);
            }
        });
    }

    private static void visit(IDynamicStackHandler stacks, Consumer<ItemStack> consumer) {
        for (int slot = 0; slot < stacks.getSlots(); slot++) {
            ItemStack stack = stacks.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                consumer.accept(stack);
            }
        }
    }
}
