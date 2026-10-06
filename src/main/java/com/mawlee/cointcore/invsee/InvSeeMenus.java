package com.mawlee.cointcore.invsee;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.invsee.menu.InvSeeAttachmentMenu;
import com.mawlee.cointcore.invsee.menu.InvSeeCuriosMenu;
import com.mawlee.cointcore.invsee.menu.InvSeeEnderMenu;
import com.mawlee.cointcore.invsee.menu.InvSeePlayerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class InvSeeMenus {
    public static final DeferredRegister<MenuType<?>> REGISTER = DeferredRegister.create(Registries.MENU, CointCore.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<InvSeePlayerMenu>> PLAYER =
            REGISTER.register("inv_see_player", () -> IMenuTypeExtension.create(InvSeePlayerMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<InvSeeEnderMenu>> ENDER =
            REGISTER.register("inv_see_ender", () -> IMenuTypeExtension.create(InvSeeEnderMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<InvSeeCuriosMenu>> CURIOS =
            REGISTER.register("inv_see_curios", () -> IMenuTypeExtension.create(InvSeeCuriosMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<InvSeeAttachmentMenu>> ATTACHMENT =
            REGISTER.register("inv_see_attachment", () -> IMenuTypeExtension.create(InvSeeAttachmentMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<?>> COSMETIC =
            REGISTER.register("inv_see_cosmetic", () -> IMenuTypeExtension.create(InvSeeMenus::cosmeticFromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<?>> POCKET =
            REGISTER.register("inv_see_pocket", () -> IMenuTypeExtension.create(InvSeeMenus::pocketFromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<?>> BACKPACK =
            REGISTER.register("inv_see_backpack", () -> IMenuTypeExtension.create(InvSeeMenus::backpackFromNetwork));

    private InvSeeMenus() {
    }

    private static AbstractContainerMenu cosmeticFromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        return invokeFromNetwork("com.mawlee.cointcore.invsee.menu.InvSeeCosmeticMenu", id, inv, buf);
    }

    private static AbstractContainerMenu pocketFromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        return invokeFromNetwork("com.mawlee.cointcore.invsee.menu.InvSeePocketMenu", id, inv, buf);
    }

    private static AbstractContainerMenu backpackFromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        return invokeFromNetwork("com.mawlee.cointcore.invsee.menu.InvSeeBackpackMenu", id, inv, buf);
    }

    private static AbstractContainerMenu invokeFromNetwork(
            String className,
            int id,
            Inventory inv,
            RegistryFriendlyByteBuf buf
    ) {
        try {
            Class<?> cls = Class.forName(className);
            return (AbstractContainerMenu) cls
                    .getMethod("fromNetwork", int.class, Inventory.class, RegistryFriendlyByteBuf.class)
                    .invoke(null, id, inv, buf);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to open InvSee menu " + className, exception);
        }
    }
}
