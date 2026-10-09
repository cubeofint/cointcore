package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.CointCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ShopMenus {
    public static final DeferredRegister<MenuType<?>> REGISTER = DeferredRegister.create(Registries.MENU, CointCore.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<TraderMenu>> TRADER =
            REGISTER.register("trader", () -> IMenuTypeExtension.create(TraderMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<PlayerTraderMenu>> PLAYER_TRADER =
            REGISTER.register("player_trader", () -> IMenuTypeExtension.create(PlayerTraderMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<PlayerTraderManageMenu>> PLAYER_TRADER_MANAGE =
            REGISTER.register(
                    "player_trader_manage",
                    () -> IMenuTypeExtension.create(PlayerTraderManageMenu::fromNetwork)
            );

    private ShopMenus() {
    }

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
