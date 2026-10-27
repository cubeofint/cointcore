package com.mawlee.cointcore.refinedstorage.wireless;

import com.refinedmods.refinedstorage.common.api.grid.Grid;
import com.refinedmods.refinedstorage.common.api.security.SecurityHelper;
import com.refinedmods.refinedstorage.common.api.support.network.item.NetworkItemContext;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.node.grid.GridExtractMode;
import com.refinedmods.refinedstorage.api.storage.limited.LimitedStorageImpl;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
    private boolean updatingPatternPreview;
    private long availablePatterns;

    public long availablePatterns() { return availablePatterns; }

    private ItemResource blankPattern() {
        return new ItemResource(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("refinedstorage", "pattern")));
    }

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
        // Older versions kept real blanks in this slot. Return them before switching to a preview.
        ItemStack previousBlanks = backend.getPatternInput().removeItemNoUpdate(0);
        initialized = true;
        save();
        if (!previousBlanks.isEmpty()) player.getInventory().placeItemBackInInventory(previousBlanks);
        refreshPatternPreview();
    }

    static Grid createNativeWirelessGrid(NetworkItemContext context) {
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
        if (initialized) {
            backend.refreshNetwork();
            refreshPatternPreview();
        }
    }

    public void refreshPatternPreview() {
        if (!initialized) return;
        var blank = blankPattern();
        availablePatterns = canUse() ? backend.getItemStorage().extract(blank, Long.MAX_VALUE, Action.SIMULATE, new PlayerActor(player)) : 0;
        ItemStack preview = availablePatterns > 0 ? blank.toItemStack(1) : ItemStack.EMPTY;
        if (ItemStack.matches(backend.getPatternInput().getItem(0), preview)) return;
        updatingPatternPreview = true;
        try {
            backend.getPatternInput().setItem(0, preview);
        } finally {
            updatingPatternPreview = false;
        }
    }

    public boolean consumeNetworkPattern() {
        if (!canUse()) return false;
        var blank = blankPattern();
        var destination = new LimitedStorageImpl(1);
        backend.createOperations(blank.getResourceType(), player).extract(blank, GridExtractMode.SINGLE_RESOURCE, destination);
        return destination.getStored() == 1;
    }

    public void save() {
        if (!initialized || updatingPatternPreview) return;
        CompoundTag state = new CompoundTag();
        backend.saveAdditional(state, player.registryAccess());
        // The slot mirrors the network; virtual items must never become persistent device contents.
        state.remove("pattern_input");
        // Preserve any other mods' custom data and Universal Grid's own components.
        CustomData.update(DataComponents.CUSTOM_DATA, device, tag -> WirelessPatternState.write(tag, state));
        player.getInventory().setChanged();
    }

    public WirelessPatternBackend backend() { return backend; }
    public SlotReference reference() { return reference; }
}
