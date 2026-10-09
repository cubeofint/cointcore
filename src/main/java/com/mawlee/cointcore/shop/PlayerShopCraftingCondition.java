package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.config.TraderOffersConfig;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;

public final class PlayerShopCraftingCondition implements ICondition {
    public static final PlayerShopCraftingCondition INSTANCE = new PlayerShopCraftingCondition();
    public static final MapCodec<PlayerShopCraftingCondition> CODEC = MapCodec.unit(INSTANCE);

    private PlayerShopCraftingCondition() {
    }

    @Override
    public boolean test(IContext context) {
        TraderOffersConfig.load();
        return TraderOffersConfig.playerShopCraftingEnabled();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
