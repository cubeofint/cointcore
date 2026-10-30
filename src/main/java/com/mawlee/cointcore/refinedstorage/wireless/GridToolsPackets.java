package com.mawlee.cointcore.refinedstorage.wireless;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.claim.ClaimGuard;
import com.mawlee.cointcore.mixin.refinedstorage.AutocrafterPatternsAccessor;
import com.mawlee.cointcore.refinedstorage.DepositUndo;
import com.refinedmods.refinedstorage.api.network.node.GraphNetworkComponent;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.security.SecurityHelper;
import com.refinedmods.refinedstorage.common.api.support.network.InWorldNetworkNodeContainer;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.autocrafting.autocrafter.*;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import com.refinedmods.refinedstorage.common.support.containermenu.ExtendedMenuProvider;
import com.refinedmods.refinedstorage.common.support.resource.ResourceCodecs;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.*;

public final class GridToolsPackets {
    public static final int FIND = 0, OPEN = 1, UNDO = 2;
    private static final Map<ServerPlayer, Integer> LAST_REQUEST = new WeakHashMap<>();
    private static final Map<ServerPlayer, CachedFind> LAST_FIND = new WeakHashMap<>();
    private GridToolsPackets() { }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(CointCore.MOD_ID);
        registrar.playToServer(Request.TYPE, Request.CODEC, (packet, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) handle(player, packet);
        }));
        registrar.playToClient(Results.TYPE, Results.CODEC, (packet, context) -> context.enqueueWork(() -> GridToolsClient.results(packet)));
    }

    private static void handle(ServerPlayer player, Request request) {
        if (request.action < FIND || request.action > UNDO) return;
        int interval = request.action == FIND ? 20 : 5;
        if (player.tickCount - LAST_REQUEST.getOrDefault(player, player.tickCount - interval) < interval) return;
        LAST_REQUEST.put(player, player.tickCount);
        var access = GridAccess.from(player, request.menuId);
        if (access == null) return;
        if (request.action == UNDO) {
            if (net.neoforged.fml.ModList.get().isLoaded("sophisticatedbackpacks")) DepositUndo.undo(access);
            return;
        }
        if (request.action == FIND) {
            var matches = find(access, request.resource);
            LAST_FIND.put(player, new CachedFind(access.network(), request.resource,
                    matches.stream().map(Match::position).toList(), player.tickCount + 1200));
            var hits = matches.stream().map(match -> new Hit(match.position, match.block.getName(), match.matches)).toList();
            PacketDistributor.sendToPlayer(player, new Results(request.menuId, request.resource, hits));
        } else {
            var cached = LAST_FIND.get(player);
            if (cached == null || cached.network != access.network() || !cached.resource.equals(request.resource)
                    || cached.expires < player.tickCount || request.index < 0 || request.index >= cached.positions.size()) return;
            var position = cached.positions.get(request.index);
            var level = player.getServer().getLevel(position.dimension());
            if (level == null || !level.hasChunkAt(position.pos()) || !(level.getBlockEntity(position.pos()) instanceof AutocrafterBlockEntity block)) return;
            var target = new Match(position, block, 0);
            if (!editable(access, target)) {
                player.displayClientMessage(Component.translatable("message.cointcore.finder.denied"), true);
                return;
            }
            com.refinedmods.refinedstorage.common.Platform.INSTANCE.getMenuOpener().openMenu(player, new Provider(access, target));
        }
    }

    static List<Match> find(GridAccess access, PlatformResourceKey resource) {
        if (!access.valid()) return List.of();
        var result = new ArrayList<Match>();
        int inspected = 0;
        for (var container : access.network().getComponent(GraphNetworkComponent.class).getContainers()) {
            if (++inspected > 32_768 || result.size() >= 128) break;
            if (!(container instanceof InWorldNetworkNodeContainer world)) continue;
            var position = world.getPosition();
            var level = access.player().getServer().getLevel(position.dimension());
            if (level == null || !level.hasChunkAt(position.pos())
                    || !(level.getBlockEntity(position.pos()) instanceof AutocrafterBlockEntity block)
                    || !ClaimGuard.canInteract(access.player(), level, position.pos())) continue;
            var patterns = ((AutocrafterPatternsAccessor) block).cointcore$patterns();
            int matches = 0;
            for (int slot = 0; slot < patterns.getContainerSize(); slot++) {
                var pattern = RefinedStorageApi.INSTANCE.getPattern(patterns.getItem(slot), level);
                if (pattern.isPresent() && pattern.get().layout().outputs().stream().anyMatch(output -> output.resource().equals(resource))) matches++;
            }
            if (matches > 0 && result.stream().noneMatch(match -> match.position.equals(position))) result.add(new Match(position, block, matches));
        }
        result.sort(Comparator.comparing((Match match) -> match.position.dimension().location().toString())
                .thenComparingLong(match -> match.position.pos().asLong()));
        return List.copyOf(result);
    }

    private static boolean editable(GridAccess access, Match match) {
        var level = match.block.getLevel();
        if (!access.valid() || match.block.isRemoved() || level == null || !level.hasChunkAt(match.position.pos())
                || !ClaimGuard.canEdit(access.player(), level, match.position.pos())) return false;
        for (var permission : List.of(BuiltinPermission.BUILD, BuiltinPermission.EXTRACT, BuiltinPermission.INSERT))
            if (!SecurityHelper.isAllowed(access.player(), permission, access.network())) return false;
        return match.block.getContainerProvider().getContainers().stream().anyMatch(c -> c.getNode().getNetwork() == access.network());
    }

    record Match(GlobalPos position, AutocrafterBlockEntity block, int matches) { }
    private record CachedFind(com.refinedmods.refinedstorage.api.network.Network network, PlatformResourceKey resource,
                              List<GlobalPos> positions, int expires) { }

    private record Provider(GridAccess access, Match match) implements ExtendedMenuProvider<AutocrafterData> {
        @Override public AutocrafterData getMenuData() { return match.block.getMenuData(); }
        @Override public StreamEncoder<RegistryFriendlyByteBuf, AutocrafterData> getMenuCodec() { return AutocrafterData.STREAM_CODEC; }
        @Override public Component getDisplayName() { return match.block.getName(); }
        @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            return new RemoteAutocrafterMenu(id, inventory, access, match);
        }
    }

    private static final class RemoteAutocrafterMenu extends AutocrafterContainerMenu {
        private final GridAccess access;
        private final Match match;
        RemoteAutocrafterMenu(int id, Inventory inventory, GridAccess access, Match match) {
            super(id, inventory, match.block);
            this.access = access;
            this.match = match;
            this.disabledSlot = access.reference();
        }
        @Override public boolean stillValid(Player player) { return player == access.player() && editable(access, match); }
        @Override public void clicked(int slot, int button, ClickType type, Player player) {
            if (!stillValid(player)) return;
            if (!access.device().isEmpty() && slot >= 0 && slot < slots.size() && slots.get(slot).getItem() == access.device()) return;
            if (!access.device().isEmpty() && type == ClickType.SWAP && button >= 0 && button < player.getInventory().getContainerSize()
                    && player.getInventory().getItem(button) == access.device()) return;
            super.clicked(slot, button, type, player);
        }
        @Override public ItemStack quickMoveStack(Player player, int slot) {
            if (!stillValid(player) || slot < 0 || slot >= slots.size()
                    || !access.device().isEmpty() && slots.get(slot).getItem() == access.device()) return ItemStack.EMPTY;
            return super.quickMoveStack(player, slot);
        }
        @Override public boolean canTakeItemForPickAll(ItemStack stack, net.minecraft.world.inventory.Slot slot) {
            return (access.device().isEmpty() || slot.getItem() != access.device()) && super.canTakeItemForPickAll(stack, slot);
        }
    }

    public record Request(int menuId, PlatformResourceKey resource, int action, int index) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "grid_tools_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Request::menuId, ResourceCodecs.STREAM_CODEC, Request::resource,
                ByteBufCodecs.VAR_INT, Request::action, ByteBufCodecs.VAR_INT, Request::index, Request::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Hit(GlobalPos position, Component name, int matches) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Hit> CODEC = StreamCodec.composite(
                GlobalPos.STREAM_CODEC, Hit::position, ComponentSerialization.STREAM_CODEC, Hit::name,
                ByteBufCodecs.VAR_INT, Hit::matches, Hit::new);
    }
    public record Results(int menuId, PlatformResourceKey resource, List<Hit> hits) implements CustomPacketPayload {
        public static final Type<Results> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "grid_tools_results"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Results> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Results::menuId, ResourceCodecs.STREAM_CODEC, Results::resource,
                Hit.CODEC.apply(ByteBufCodecs.list(128)), Results::hits, Results::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
