package com.mawlee.cointcore.refinedstorage.wireless;

import com.mawlee.cointcore.mixin.refinedstorage.GridToolsAccessor;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.common.api.grid.Grid;
import com.refinedmods.refinedstorage.common.api.security.SecurityHelper;
import com.refinedmods.refinedstorage.common.api.support.network.AbstractNetworkNodeContainerBlockEntity;
import com.refinedmods.refinedstorage.common.api.support.network.item.NetworkItemContext;
import com.refinedmods.refinedstorage.common.grid.AbstractGridContainerMenu;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import com.ultramega.universalgrid.common.interfaces.MixinDisabledSlot;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record GridAccess(ServerPlayer player, Grid grid, Network network,
                         NetworkItemContext wireless, SlotReference reference, ItemStack device) {
    public static GridAccess from(ServerPlayer player, int menuId) {
        if (!(player.containerMenu instanceof AbstractGridContainerMenu menu) || menu.containerId != menuId
                || !menu.stillValid(player)) return null;
        var grid = ((GridToolsAccessor) menu).cointcore$grid();
        if (grid == null || !grid.isGridActive()) return null;
        var reference = ((MixinDisabledSlot) menu).universalgrid$getDisabledSlot();
        NetworkItemContext wireless = null;
        Network network = null;
        ItemStack device = ItemStack.EMPTY;
        if (reference != null) {
            device = reference.resolve(player).orElse(ItemStack.EMPTY);
            if (!WirelessPatternIntegration.isUniversalGrid(device)) return null;
            wireless = RefinedStorageApi.INSTANCE.getNetworkItemHelper().createContext(device, player, reference);
            network = wireless.resolveNetwork().orElse(null);
        } else if (grid instanceof AbstractNetworkNodeContainerBlockEntity<?> block) {
            for (var container : block.getContainerProvider().getContainers()) {
                network = container.getNode().getNetwork();
                if (network != null) break;
            }
        }
        if (network == null) return null;
        var access = new GridAccess(player, grid, network, wireless, reference, device);
        return access.valid() ? access : null;
    }

    public boolean valid() {
        if (!player.isAlive() || player.isSpectator() || !SecurityHelper.isAllowed(player, BuiltinPermission.OPEN, network)) return false;
        if (wireless != null) return reference.resolve(player).orElse(ItemStack.EMPTY) == device
                && wireless.isActive() && wireless.resolveNetwork().orElse(null) == network;
        if (!grid.isGridActive() || !grid.canMenuStayOpen(player)) return false;
        if (!(grid instanceof AbstractNetworkNodeContainerBlockEntity<?> block) || block.isRemoved()) return false;
        return block.getContainerProvider().getContainers().stream().anyMatch(container -> container.getNode().getNetwork() == network);
    }
}
