package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.CointCore;
import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ShopConditions {
    public static final DeferredRegister<MapCodec<? extends ICondition>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, CointCore.MOD_ID);

    static {
        REGISTER.register("player_shop_crafting", () -> PlayerShopCraftingCondition.CODEC);
    }

    private ShopConditions() {
    }

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
