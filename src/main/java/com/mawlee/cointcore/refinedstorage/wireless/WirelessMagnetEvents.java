package com.mawlee.cointcore.refinedstorage.wireless;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.claim.ClaimGuard;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.node.grid.GridInsertMode;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.StorageImpl;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.security.SecurityHelper;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = CointCore.MOD_ID)
public final class WirelessMagnetEvents {
    private WirelessMagnetEvents() { }

    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!ModList.get().isLoaded("refinedstorage") || !ModList.get().isLoaded("universalgrid")
                || !(event.getEntity() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()
                || player.tickCount % 10 != 0) return;
        for (var reference : WirelessTerminalActions.terminals(player)) {
            var device = reference.resolve(player).orElse(net.minecraft.world.item.ItemStack.EMPTY);
            MagnetMode mode = MagnetState.mode(device);
            if (mode == MagnetMode.OFF || !MagnetState.accepts(MagnetState.card(device, player.registryAccess()))) continue;
            if (RefinedStorageApi.INSTANCE.isEnergyRequired()
                    && RefinedStorageApi.INSTANCE.getEnergyStorage(device).filter(energy -> energy.getStored() <= 0).isPresent()) continue;
            var context = RefinedStorageApi.INSTANCE.getNetworkItemHelper().createContext(device, player, reference);
            var grid = mode == MagnetMode.NETWORK ? WirelessPatternSession.createNativeWirelessGrid(context) : null;
            if (grid != null && (!context.isActive() || !grid.isGridActive() || context.resolveNetwork().filter(network ->
                    SecurityHelper.isAllowed(player, BuiltinPermission.OPEN, network)).isEmpty())) continue;
            int processed = 0;
            for (ItemEntity entity : player.serverLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(6))) {
                if (processed++ >= 32) break;
                if (!entity.isAlive() || entity.hasPickUpDelay() || entity.distanceToSqr(player) > 36
                        || entity.getPersistentData().getBoolean("PreventRemoteMovement")
                        || entity.getTarget() != null && !entity.getTarget().equals(player.getUUID())
                        || !ClaimGuard.canInteract(player, player.level(), entity.blockPosition())) continue;
                if (RefinedStorageApi.INSTANCE.isEnergyRequired()
                        && RefinedStorageApi.INSTANCE.getEnergyStorage(device).filter(energy -> energy.getStored() <= 0).isPresent()) break;
                if (mode == MagnetMode.INVENTORY) {
                    int before = entity.getItem().getCount();
                    entity.playerTouch(player);
                    if (!entity.isAlive() || entity.getItem().getCount() < before) {
                        context.drainEnergy(com.refinedmods.refinedstorage.common.Platform.INSTANCE.getConfig().getWirelessGrid().getInsertEnergyUsage());
                    }
                    continue;
                }
                if (EventHooks.fireItemPickupPre(entity, player).canPickup() == TriState.FALSE || !entity.isAlive()) continue;
                var original = entity.getItem().copy();
                if (original.isEmpty()) continue;
                var resource = ItemResource.ofItemStack(original);
                var buffer = new StorageImpl();
                buffer.insert(resource, original.getCount(), Action.EXECUTE, Actor.EMPTY);
                grid.createOperations(resource.getResourceType(), player).insert(resource, GridInsertMode.ENTIRE_RESOURCE, buffer);
                int accepted = original.getCount() - (int) buffer.getStored();
                if (accepted <= 0) continue;
                var remaining = original.copyWithCount(original.getCount() - accepted);
                if (remaining.isEmpty()) entity.discard(); else entity.setItem(remaining);
                player.take(entity, accepted);
                EventHooks.fireItemPickupPost(entity, player, original.copyWithCount(accepted));
            }
            // One active installed card per player avoids multiplying range/work with duplicate terminals.
            return;
        }
    }
}
