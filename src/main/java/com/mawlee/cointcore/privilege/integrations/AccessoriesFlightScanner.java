package com.mawlee.cointcore.privilege.integrations;

import com.mawlee.cointcore.privilege.FlightItemMatcher;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class AccessoriesFlightScanner {
    private AccessoriesFlightScanner() {
    }

    public static boolean hasFlightItemEquipped(ServerPlayer player) {
        AccessoriesCapability capability = AccessoriesCapability.getOptionally(player).orElse(null);
        if (capability == null) {
            return false;
        }

        for (AccessoriesContainer container : capability.getContainers().values()) {
            if (containsFlightItem(container.getAccessories()) || containsFlightItem(container.getCosmeticAccessories())) {
                return true;
            }
        }

        return false;
    }

    private static boolean containsFlightItem(io.wispforest.accessories.impl.ExpandedSimpleContainer container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (FlightItemMatcher.isFlightItem(stack)) {
                return true;
            }
        }
        return false;
    }
}
