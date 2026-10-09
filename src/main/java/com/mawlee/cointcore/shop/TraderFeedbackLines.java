package com.mawlee.cointcore.shop;

import net.minecraft.network.chat.Component;

public final class TraderFeedbackLines {
    private TraderFeedbackLines() {
    }

    public static Component of(TraderFeedbackPayload payload) {
        TraderFeedbackKind kind = payload.kind();
        return switch (kind) {
            case OFFER_UNAVAILABLE -> Component.translatable("gui.cointcore.trader.error.offer_unavailable");
            case INVENTORY_FULL -> Component.translatable("gui.cointcore.trader.error.inventory_full");
            case NOT_ENOUGH_GLUONS -> Component.translatable("gui.cointcore.trader.error.not_enough", payload.gluons());
            case NOT_ENOUGH_ITEMS -> Component.translatable("gui.cointcore.trader.error.not_enough_items");
            case BOUGHT -> Component.translatable(
                    "gui.cointcore.trader.bought",
                    payload.count(),
                    payload.itemName(),
                    payload.gluons(),
                    payload.fee()
            );
            case SOLD -> Component.translatable(
                    "gui.cointcore.trader.sold",
                    payload.count(),
                    payload.itemName(),
                    payload.gluons(),
                    payload.fee()
            );
            case OUT_OF_STOCK -> Component.translatable("gui.cointcore.player_trader.error.out_of_stock");
            case STOCK_FULL -> Component.translatable("gui.cointcore.player_trader.error.stock_full");
            case OWNER_BROKE -> Component.translatable("gui.cointcore.player_trader.error.owner_broke");
            case OWN_SHOP -> Component.translatable("gui.cointcore.player_trader.error.own_shop");
        };
    }
}
