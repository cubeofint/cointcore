package com.mawlee.cointcore.refinedstorage.wireless;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.claim.ClaimGuard;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.node.grid.GridInsertMode;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.StorageImpl;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.grid.Grid;
import com.refinedmods.refinedstorage.common.api.support.network.item.NetworkItemContext;
import com.refinedmods.refinedstorage.common.api.security.SecurityHelper;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.minecraft.world.item.ItemStack;

@EventBusSubscriber(modid = CointCore.MOD_ID)
public final class WirelessMagnetEvents {
    private static final ThreadLocal<Boolean> REMOTE_PICKUP_CHECK = new ThreadLocal<>();

    private WirelessMagnetEvents() { }

    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!ModList.get().isLoaded("refinedstorage") || !ModList.get().isLoaded("universalgrid")
                || !(event.getEntity() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()
                || player.tickCount % 10 != 0) return;
        var magnet = findMagnet(player);
        if (magnet == null) return;
        int processed = 0;
        for (ItemEntity entity : player.serverLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(6))) {
            if (processed++ >= 32) break;
            if (!canCollect(player, entity) || entity.distanceToSqr(player) > 36) continue;
            if (!hasEnergy(magnet.device())) break;
            if (magnet.mode() == MagnetMode.INVENTORY) {
                int before = entity.getItem().getCount();
                entity.playerTouch(player);
                if (!entity.isAlive() || entity.getItem().getCount() < before) {
                    magnet.context().drainEnergy(com.refinedmods.refinedstorage.common.Platform.INSTANCE.getConfig().getWirelessGrid().getInsertEnergyUsage());
                }
                continue;
            }
            // Run other mods' pickup restrictions, without recursively invoking our collision route.
            boolean allowed;
            REMOTE_PICKUP_CHECK.set(true);
            try {
                allowed = EventHooks.fireItemPickupPre(entity, player).canPickup() != TriState.FALSE;
            } finally {
                REMOTE_PICKUP_CHECK.remove();
            }
            if (allowed && entity.isAlive()) insertIntoNetwork(player, entity, magnet.grid());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void pickup(ItemEntityPickupEvent.Pre event) {
        if (Boolean.TRUE.equals(REMOTE_PICKUP_CHECK.get()) || event.canPickup() == TriState.FALSE
                || !ModList.get().isLoaded("refinedstorage") || !ModList.get().isLoaded("universalgrid")
                || !(event.getPlayer() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()
                || !canCollect(player, event.getItemEntity())) return;
        var magnet = findMagnet(player);
        if (magnet == null || magnet.mode() != MagnetMode.NETWORK) return;
        insertIntoNetwork(player, event.getItemEntity(), magnet.grid());
        // Network mode owns this pickup even if storage is full or only accepts part of the stack.
        event.setCanPickup(TriState.FALSE);
    }

    private static boolean canCollect(ServerPlayer player, ItemEntity entity) {
        return entity.isAlive() && !entity.hasPickUpDelay()
                && !entity.getPersistentData().getBoolean("PreventRemoteMovement")
                && (entity.getTarget() == null || entity.getTarget().equals(player.getUUID()))
                && ClaimGuard.canInteract(player, player.level(), entity.blockPosition());
    }

    private static boolean hasEnergy(ItemStack device) {
        return !RefinedStorageApi.INSTANCE.isEnergyRequired()
                || RefinedStorageApi.INSTANCE.getEnergyStorage(device).filter(energy -> energy.getStored() <= 0).isEmpty();
    }

    private static ActiveMagnet findMagnet(ServerPlayer player) {
        for (var reference : WirelessTerminalActions.terminals(player)) {
            var device = reference.resolve(player).orElse(net.minecraft.world.item.ItemStack.EMPTY);
            MagnetMode mode = MagnetState.mode(device);
            if (mode == MagnetMode.OFF || !MagnetState.accepts(MagnetState.card(device, player.registryAccess()))) continue;
            if (!hasEnergy(device)) continue;
            var context = RefinedStorageApi.INSTANCE.getNetworkItemHelper().createContext(device, player, reference);
            var grid = mode == MagnetMode.NETWORK ? WirelessPatternSession.createNativeWirelessGrid(context) : null;
            if (grid != null && (!context.isActive() || !grid.isGridActive() || context.resolveNetwork().filter(network ->
                    SecurityHelper.isAllowed(player, BuiltinPermission.OPEN, network)).isEmpty())) continue;
            // One active installed card per player avoids multiplying range/work with duplicate terminals.
            return new ActiveMagnet(device, mode, context, grid);
        }
        return null;
    }

    private static boolean insertIntoNetwork(ServerPlayer player, ItemEntity entity, Grid grid) {
        var original = entity.getItem().copy();
        if (original.isEmpty()) return false;
        var resource = ItemResource.ofItemStack(original);
        var buffer = new StorageImpl();
        buffer.insert(resource, original.getCount(), Action.EXECUTE, Actor.EMPTY);
        grid.createOperations(resource.getResourceType(), player).insert(resource, GridInsertMode.ENTIRE_RESOURCE, buffer);
        int accepted = original.getCount() - (int) buffer.getStored();
        if (accepted <= 0) return false;
        // Pre-pickup events permit in-place stack changes, but explicitly prohibit ItemEntity.setItem.
        entity.getItem().shrink(accepted);
        if (entity.getItem().isEmpty()) entity.discard();
        player.take(entity, accepted);
        EventHooks.fireItemPickupPost(entity, player, original.copyWithCount(accepted));
        return true;
    }

    private record ActiveMagnet(ItemStack device, MagnetMode mode, NetworkItemContext context, Grid grid) { }
}
