package com.mawlee.cointcore.refinedstorage.wireless;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.mixin.refinedstorage.TerminalSlotProviderAccessor;
import com.refinedmods.refinedstorage.api.network.node.grid.GridExtractMode;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.security.SecurityHelper;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import com.refinedmods.refinedstorage.common.support.AbstractBaseContainerMenu;
import com.ultramega.universalgrid.common.interfaces.MixinDisabledSlot;
import com.ultramega.universalgrid.common.mixin.InvokerRefinedStorageApiProxy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.WeakHashMap;

/** Server-authoritative pick-block extraction. The client supplies only a resource, never a network. */
public final class WirelessTerminalActions {
    private static final WeakHashMap<ServerPlayer, Integer> LAST_PICK = new WeakHashMap<>();

    private WirelessTerminalActions() { }

    public static List<SlotReference> terminals(Player player) {
        var api = RefinedStorageApi.INSTANCE;
        if (api instanceof InvokerRefinedStorageApiProxy proxy) api = proxy.universalgrid$ensureLoaded();
        Set<Item> items = Set.of(
                BuiltInRegistries.ITEM.get(ResourceLocation.parse("universalgrid:wireless_universal_grid")),
                BuiltInRegistries.ITEM.get(ResourceLocation.parse("universalgrid:creative_wireless_universal_grid")));
        return ((TerminalSlotProviderAccessor) api).cointcore$slotReferences().find(player, items);
    }

    public static void registerPackets(RegisterPayloadHandlersEvent event) {
        event.registrar(CointCore.MOD_ID).playToServer(PickPayload.TYPE, PickPayload.CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) pick(player, packet.item(), packet.cursor());
                }));
    }

    private static void pick(ServerPlayer player, ItemStack requested, boolean cursor) {
        if (requested.isEmpty() || !player.isAlive() || player.isSpectator()) return;
        if (LAST_PICK.getOrDefault(player, -1) == player.tickCount) return;
        LAST_PICK.put(player, player.tickCount);
        var resource = ItemResource.ofItemStack(requested.copyWithCount(1));
        var references = new ArrayList<>(terminals(player));
        if (player.containerMenu instanceof AbstractBaseContainerMenu menu) {
            var active = ((MixinDisabledSlot) menu).universalgrid$getDisabledSlot();
            if (active != null && active.resolve(player).filter(WirelessPatternIntegration::isUniversalGrid).isPresent()) {
                references.remove(active);
                references.addFirst(active);
            }
        }
        for (var reference : references) {
            var device = reference.resolve(player).orElse(ItemStack.EMPTY);
            if (!WirelessPatternIntegration.isUniversalGrid(device)) continue;
            var context = RefinedStorageApi.INSTANCE.getNetworkItemHelper().createContext(device, player, reference);
            if (!context.isActive() || context.resolveNetwork().filter(network ->
                    SecurityHelper.isAllowed(player, BuiltinPermission.OPEN, network)).isEmpty()) continue;
            var grid = WirelessPatternSession.createNativeWirelessGrid(context);
            if (!grid.isGridActive()) continue;
            var destination = new PickDestination(player, resource, cursor);
            var buffer = new PickBuffer(resource, destination.capacity());
            grid.createOperations(resource.getResourceType(), player).extract(
                    resource, GridExtractMode.ENTIRE_RESOURCE, buffer);
            if (buffer.getStored() > 0) {
                destination.deliver((int) buffer.getStored());
                player.getInventory().setChanged();
                player.inventoryMenu.broadcastChanges();
                player.containerMenu.broadcastChanges();
                return;
            }
        }
        player.displayClientMessage(Component.translatable("message.cointcore.wireless_pick.unavailable"), true);
    }

    private static final class PickDestination {
        private final ServerPlayer player;
        private final ItemResource item;
        private final boolean cursor;
        private final int target;
        private final int relocate;

        private PickDestination(ServerPlayer player, ItemResource item, boolean cursor) {
            this.player = player;
            this.item = item;
            this.cursor = cursor;
            var plan = PickSlotPlan.choose(player.getInventory().selected,
                    slot -> player.getInventory().getItem(slot).isEmpty(),
                    slot -> compatible(player.getInventory().getItem(slot))
                            && player.getInventory().getItem(slot).getCount() < item.toItemStack().getMaxStackSize());
            target = plan.target();
            relocate = plan.relocate();
        }

        private boolean compatible(ItemStack stack) {
            return stack.isEmpty() || ItemStack.isSameItemSameComponents(stack, item.toItemStack());
        }

        private int capacity() {
            if (!cursor && target < 0) return 0;
            if (!cursor && relocate >= 0 && !player.getInventory().getItem(relocate).isEmpty()) return 0;
            ItemStack existing = cursor ? player.containerMenu.getCarried()
                    : relocate >= 0 ? ItemStack.EMPTY : player.getInventory().getItem(target);
            if (!compatible(existing)) return 0;
            return Math.max(0, item.toItemStack().getMaxStackSize() - existing.getCount());
        }

        private void deliver(int amount) {
            int accepted = Math.min(amount, capacity());
            if (accepted > 0) {
                ItemStack existing = cursor ? player.containerMenu.getCarried()
                        : relocate >= 0 ? ItemStack.EMPTY : player.getInventory().getItem(target);
                ItemStack result = item.toItemStack(existing.getCount() + accepted);
                if (cursor) player.containerMenu.setCarried(result);
                else {
                    if (relocate >= 0) player.getInventory().setItem(relocate, player.getInventory().getItem(target));
                    player.getInventory().setItem(target, result);
                    player.getInventory().selected = target;
                    player.connection.send(new ClientboundSetCarriedItemPacket(target));
                }
            }
            if (amount > accepted) player.getInventory().placeItemBackInInventory(item.toItemStack(amount - accepted));
        }
    }

    public record PickPayload(ItemStack item, boolean cursor) implements CustomPacketPayload {
        public static final Type<PickPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "wireless_pick"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PickPayload> CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, PickPayload::item, ByteBufCodecs.BOOL, PickPayload::cursor, PickPayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
