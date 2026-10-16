package com.mawlee.cointcore.privilege;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModInfo;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public final class PrivilegeItemScope {
    private static final Method CURIOS_FOR_EACH = loadCuriosForEach();

    private PrivilegeItemScope() {
    }

    public static void forEachOwnedItem(ServerPlayer player, Consumer<ItemStack> consumer) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty()) {
                consumer.accept(stack);
            }
        }
        if (CURIOS_FOR_EACH == null) {
            return;
        }
        try {
            CURIOS_FOR_EACH.invoke(null, player, consumer);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static Method loadCuriosForEach() {
        if (!isModLoaded("curios")) {
            return null;
        }
        try {
            Class<?> type = Class.forName("com.mawlee.cointcore.privilege.integrations.CuriosPrivilegeStacks");
            return type.getMethod("forEach", Player.class, Consumer.class);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static boolean isModLoaded(String modId) {
        return LoadingModList.get().getMods().stream()
                .map(ModInfo::getModId)
                .anyMatch(modId::equals);
    }
}
