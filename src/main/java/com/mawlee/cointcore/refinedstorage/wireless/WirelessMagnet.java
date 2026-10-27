package com.mawlee.cointcore.refinedstorage.wireless;

import com.mawlee.cointcore.CointCore;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReferenceFactory;
import com.refinedmods.refinedstorage.common.support.AbstractBaseContainerMenu;
import com.refinedmods.refinedstorage.common.support.containermenu.ExtendedMenuProvider;
import com.ultramega.universalgrid.common.interfaces.MixinDisabledSlot;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamEncoder;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import java.util.List;

public final class WirelessMagnet {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CointCore.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, CointCore.MOD_ID);
    public static final DeferredItem<Item> CARD = ITEMS.register("wireless_magnet_card", () -> new Item(new Item.Properties().stacksTo(1)) {
        @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flags) {
            super.appendHoverText(stack, context, lines, flags);
            lines.add(Component.translatable("tooltip.cointcore.magnet.card"));
        }
    });
    public static final DeferredHolder<MenuType<?>, MenuType<MagnetMenu>> MENU = MENUS.register("wireless_magnet",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new MagnetMenu(id, inventory,
                    SlotReferenceFactory.STREAM_CODEC.decode(buf), null)));

    private WirelessMagnet() { }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        MENUS.register(bus);
        bus.addListener(WirelessMagnet::packets);
        bus.addListener(WirelessMagnet::creative);
    }

    private static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(CARD.get());
    }

    private static void packets(RegisterPayloadHandlersEvent event) {
        event.registrar(CointCore.MOD_ID).playToServer(SettingsPayload.TYPE, SettingsPayload.CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) action(player, packet.menuId(), packet.configure());
                }));
    }

    private static void action(ServerPlayer player, int menuId, boolean configure) {
        if (!player.isAlive() || player.isSpectator() || player.containerMenu.containerId != menuId
                || !(player.containerMenu instanceof AbstractBaseContainerMenu menu)) return;
        var reference = ((MixinDisabledSlot) menu).universalgrid$getDisabledSlot();
        if (reference == null) return;
        var device = reference.resolve(player).orElse(ItemStack.EMPTY);
        if (!WirelessPatternIntegration.isUniversalGrid(device)) return;
        if (configure || !MagnetState.installed(device)) {
            com.refinedmods.refinedstorage.common.Platform.INSTANCE.getMenuOpener().openMenu(player, new Provider(reference, device));
        } else {
            MagnetState.cycle(device);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
    }

    private record Provider(SlotReference reference, ItemStack device) implements ExtendedMenuProvider<SlotReference> {
        @Override public SlotReference getMenuData() { return reference; }
        @Override public StreamEncoder<RegistryFriendlyByteBuf, SlotReference> getMenuCodec() { return SlotReferenceFactory.STREAM_CODEC; }
        @Override public Component getDisplayName() { return Component.translatable("menu.cointcore.wireless_magnet"); }
        @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new MagnetMenu(id, inventory, reference, device); }
    }

    public record SettingsPayload(int menuId, boolean configure) implements CustomPacketPayload {
        public static final Type<SettingsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "wireless_magnet_settings"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SettingsPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SettingsPayload::menuId, ByteBufCodecs.BOOL, SettingsPayload::configure, SettingsPayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
