package com.mawlee.cointcore.refinedstorage.wireless;

import com.refinedmods.refinedstorage.common.api.grid.Grid;
import com.refinedmods.refinedstorage.common.api.security.SecurityHelper;
import com.refinedmods.refinedstorage.common.api.support.network.item.NetworkItemContext;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** One editor per currently held device. Real patterns are saved on every inventory mutation. */
public final class WirelessPatternSession {
    private final ServerPlayer player;
    private final ItemStack device;
    private final SlotReference reference;
    private final NetworkItemContext context;
    private WirelessPatternBackend backend;
    private boolean initialized;

    public WirelessPatternSession(ServerPlayer player, ItemStack device, SlotReference reference, NetworkItemContext context) {
        this.player = player;
        this.device = device;
        this.reference = reference;
        this.context = context;
    }

    public void initialize() {
        backend = new WirelessPatternBackend(this, createNativeWirelessGrid(context), context);
        CompoundTag data = device.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (data.contains(WirelessPatternState.KEY, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            backend.loadAdditional(WirelessPatternState.read(data), player.registryAccess());
        }
        backend.setLevel(player.serverLevel());
        backend.refreshNetwork();
        initialized = true;
    }

    private static Grid createNativeWirelessGrid(NetworkItemContext context) {
        // RS does not expose a wireless Grid factory. Reuse its implementation to retain secured
        // insert/extract operations, transmitter range checks, energy usage and watcher cleanup.
        try {
            var type = Class.forName("com.refinedmods.refinedstorage.common.grid.WirelessGrid");
            var constructor = type.getDeclaredConstructor(NetworkItemContext.class);
            constructor.setAccessible(true);
            return (Grid) constructor.newInstance(context);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unsupported Refined Storage wireless Grid implementation", exception);
        }
    }

    public boolean canUse() {
        return player.isAlive() && reference.resolve(player).orElse(ItemStack.EMPTY) == device
                && WirelessPatternIntegration.isUniversalGrid(device) && context.isActive()
                && (!initialized || backend.isGridActive())
                && context.resolveNetwork().filter(network -> SecurityHelper.isAllowed(player, BuiltinPermission.OPEN, network)).isPresent();
    }

    public void refresh() {
        if (initialized) backend.refreshNetwork();
    }

    public void save() {
        if (!initialized) return;
        CompoundTag state = new CompoundTag();
        backend.saveAdditional(state, player.registryAccess());
        // Preserve any other mods' custom data and Universal Grid's own components.
        CustomData.update(DataComponents.CUSTOM_DATA, device, tag -> WirelessPatternState.write(tag, state));
        player.getInventory().setChanged();
    }

    public WirelessPatternBackend backend() { return backend; }
    public SlotReference reference() { return reference; }
}
