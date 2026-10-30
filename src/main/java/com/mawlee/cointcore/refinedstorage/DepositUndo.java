package com.mawlee.cointcore.refinedstorage;

import com.mawlee.cointcore.claim.ClaimGuard;
import com.mawlee.cointcore.refinedstorage.wireless.GridAccess;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.common.api.security.SecurityHelper;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.iface.InterfaceBlockEntity;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import java.util.*;

/** Last successful backpack deposit only; an undo withdraws existing resources, never creates them. */
public final class DepositUndo {
    private static final Map<ServerPlayer, Entry> LAST = new WeakHashMap<>();
    private DepositUndo() { }

    public static void remember(ServerPlayer player, ItemStack backpack, Network network, GlobalPos source,
                                Map<ItemResource, Long> accepted) {
        if (accepted.isEmpty()) return;
        BackpackWrapper.fromStack(backpack).getContentsUuid().ifPresent(id -> LAST.put(player, new Entry(id, network, source,
                player.getServer().overworld().getGameTime() + 12_000, new LinkedHashMap<>(accepted))));
    }

    public static void undo(GridAccess access) {
        var player = access.player();
        var entry = LAST.get(player);
        if (entry == null || player.getServer().overworld().getGameTime() > entry.expires) {
            LAST.remove(player);
            player.displayClientMessage(Component.translatable("message.cointcore.undo.none"), true);
            return;
        }
        ItemStack backpack = ItemStack.EMPTY;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof BackpackItem && BackpackWrapper.fromExistingData(stack)
                    .flatMap(wrapper -> wrapper.getContentsUuid()).filter(entry.backpack::equals).isPresent()) {
                backpack = stack;
                break;
            }
        }
        var level = player.getServer().getLevel(entry.source.dimension());
        if (!access.valid() || entry.network != access.network() || backpack.isEmpty() || level == null
                || !level.hasChunkAt(entry.source.pos())
                || !(level.getBlockEntity(entry.source.pos()) instanceof InterfaceBlockEntity iface)
                || !ClaimGuard.canInteract(player, level, entry.source.pos())
                || iface.getContainerProvider().getContainers().stream().noneMatch(c -> c.getNode().getNetwork() == entry.network)
                || !SecurityHelper.isAllowed(player, BuiltinPermission.EXTRACT, entry.network)) {
            player.displayClientMessage(Component.translatable("message.cointcore.undo.unavailable"), true);
            return;
        }
        var inventory = BackpackWrapper.fromStack(backpack).getInventoryHandler();
        var storage = entry.network.getComponent(StorageNetworkComponent.class);
        var actor = new PlayerActor(player);
        long restored = 0;
        var iterator = entry.remaining.entrySet().iterator();
        while (iterator.hasNext()) {
            if (!access.valid()) break;
            var resource = iterator.next();
            var offered = resource.getKey().toItemStack(Math.min(resource.getValue(), Integer.MAX_VALUE));
            int planned = offered.getCount() - insert(inventory, offered, true).getCount();
            if (planned <= 0) continue;
            long extracted = storage.extract(resource.getKey(), planned, Action.EXECUTE, actor);
            if (extracted <= 0) continue;
            var remainder = insert(inventory, resource.getKey().toItemStack(extracted), false);
            long returned = 0;
            if (!remainder.isEmpty()) {
                returned = storage.insert(resource.getKey(), remainder.getCount(), Action.EXECUTE, actor);
                remainder.shrink((int) returned);
                if (!remainder.isEmpty()) player.getInventory().placeItemBackInInventory(remainder);
            }
            long taken = extracted - returned;
            restored += taken;
            if (taken > 0 && access.wireless() != null) access.wireless().drainEnergy(
                    com.refinedmods.refinedstorage.common.Platform.INSTANCE.getConfig().getWirelessGrid().getExtractEnergyUsage());
            resource.setValue(resource.getValue() - taken);
            if (resource.getValue() == 0) iterator.remove();
        }
        if (entry.remaining.isEmpty()) LAST.remove(player);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.displayClientMessage(Component.translatable("message.cointcore.undo.restored", restored), true);
    }

    private static ItemStack insert(IItemHandler inventory, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < inventory.getSlots() && !remaining.isEmpty(); slot++) remaining = inventory.insertItem(slot, remaining, simulate);
        return remaining;
    }

    private record Entry(UUID backpack, Network network, GlobalPos source, long expires,
                         LinkedHashMap<ItemResource, Long> remaining) { }
}
