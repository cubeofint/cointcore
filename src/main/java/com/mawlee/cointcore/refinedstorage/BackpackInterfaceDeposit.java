package com.mawlee.cointcore.refinedstorage;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.claim.ClaimGuard;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.network.energy.EnergyNetworkComponent;
import com.refinedmods.refinedstorage.api.network.impl.node.AbstractNetworkNode;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.security.SecurityHelper;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.iface.InterfaceBlockEntity;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;

@EventBusSubscriber(modid = CointCore.MOD_ID)
public final class BackpackInterfaceDeposit {
    private BackpackInterfaceDeposit() { }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void deposit(PlayerInteractEvent.RightClickBlock event) {
        if (!ModList.get().isLoaded("refinedstorage") || !ModList.get().isLoaded("sophisticatedbackpacks")) return;
        if (event.getUseBlock() == TriState.FALSE || event.getUseItem() == TriState.FALSE) return;
        if (!event.getEntity().isShiftKeyDown() || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getItemStack().getItem() instanceof BackpackItem)
                || !(event.getLevel().getBlockEntity(event.getPos()) instanceof InterfaceBlockEntity iface)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()
                || !ClaimGuard.canInteract(player, event.getLevel(), event.getPos())) return;
        for (var container : iface.getContainerProvider().getContainers()) {
            if (!(container.getNode() instanceof AbstractNetworkNode node) || !node.isActive()) continue;
            var network = node.getNetwork();
            if (network == null || !SecurityHelper.isAllowed(player, BuiltinPermission.OPEN, network)
                    || !SecurityHelper.isAllowed(player, BuiltinPermission.INSERT, network)) continue;
            if (RefinedStorageApi.INSTANCE.isEnergyRequired()
                    && network.getComponent(EnergyNetworkComponent.class).getStored() <= 0) continue;
            var wrapper = BackpackWrapper.fromStack(event.getItemStack());
            wrapper.fillWithLoot(player);
            var inventory = wrapper.getInventoryHandler();
            var storage = network.getComponent(StorageNetworkComponent.class);
            var actor = new PlayerActor(player);
            long transferred = 0;
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack existing = inventory.getStackInSlot(slot);
                if (existing.isEmpty()) continue;
                final int sourceSlot = slot;
                transferred += DepositTransfer.transfer(storage, new ResourceAmount(ItemResource.ofItemStack(existing), existing.getCount()), actor,
                        amount -> {
                            ItemStack removed = inventory.extractItem(sourceSlot, (int) amount, false);
                            return removed.isEmpty() ? null : new ResourceAmount(ItemResource.ofItemStack(removed), removed.getCount());
                        }, refund -> {
                            ItemStack remainder = inventory.insertItem(sourceSlot, ((ItemResource) refund.resource()).toItemStack(refund.amount()), false);
                            if (!remainder.isEmpty()) player.getInventory().placeItemBackInInventory(remainder);
                        });
            }
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            player.displayClientMessage(Component.translatable("message.cointcore.backpack.deposited", transferred), true);
            return;
        }
        player.displayClientMessage(Component.translatable("message.cointcore.backpack.unavailable"), true);
    }
}
