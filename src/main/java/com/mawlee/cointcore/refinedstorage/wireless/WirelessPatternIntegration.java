package com.mawlee.cointcore.refinedstorage.wireless;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.mixin.refinedstorage.WirelessPatternMenuConstructor;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReferenceFactory;
import com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternGridContainerMenu;
import com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternGridData;
import com.refinedmods.refinedstorage.common.support.containermenu.ExtendedMenuProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamEncoder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WirelessPatternIntegration {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, CointCore.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<PatternGridContainerMenu>> MENU = MENUS.register(
            "wireless_pattern_grid", () -> IMenuTypeExtension.create((id, inventory, buf) -> {
                var data = MenuData.CODEC.decode(buf);
                var menu = new PatternGridContainerMenu(id, inventory, data.patterns());
                ((WirelessPatternMenu) menu).cointcore$initializeWirelessPatterns(data.reference(), null);
                return menu;
            }));

    private WirelessPatternIntegration() {
    }

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }

    public static boolean isUniversalGrid(ItemStack stack) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals("universalgrid") && (id.getPath().equals("wireless_universal_grid")
                || id.getPath().equals("creative_wireless_universal_grid"));
    }

    public static void openFromDevice(ServerPlayer player, ItemStack stack, SlotReference reference) {
        // Resolve the actual server-side device before creating its wireless context.
        if (reference == null || reference.resolve(player).orElse(ItemStack.EMPTY) != stack
                || !isUniversalGrid(stack)) return;
        var context = RefinedStorageApi.INSTANCE.getNetworkItemHelper().createContext(stack, player, reference);
        var session = new WirelessPatternSession(player, stack, reference, context);
        if (!session.canUse()) {
            player.displayClientMessage(Component.translatable("message.cointcore.wireless_patterns.unavailable"), true);
            return;
        }
        session.initialize();
        com.refinedmods.refinedstorage.common.Platform.INSTANCE.getMenuOpener().openMenu(player, new Provider(session));
    }

    public record MenuData(PatternGridData patterns, SlotReference reference) {
        public static final StreamCodec<RegistryFriendlyByteBuf, MenuData> CODEC = StreamCodec.composite(
                PatternGridData.STREAM_CODEC, MenuData::patterns,
                SlotReferenceFactory.STREAM_CODEC, MenuData::reference, MenuData::new);
    }

    private record Provider(WirelessPatternSession session) implements ExtendedMenuProvider<MenuData> {
        @Override public MenuData getMenuData() { return new MenuData(session.backend().getMenuData(), session.reference()); }
        @Override public StreamEncoder<RegistryFriendlyByteBuf, MenuData> getMenuCodec() { return MenuData.CODEC; }
        @Override public Component getDisplayName() { return Component.translatable("menu.cointcore.wireless_pattern_grid"); }
        @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            var menu = WirelessPatternMenuConstructor.cointcore$create(id, inventory, session.backend());
            ((WirelessPatternMenu) menu).cointcore$initializeWirelessPatterns(session.reference(), session);
            return menu;
        }
    }
}
