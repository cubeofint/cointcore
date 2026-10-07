package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.CointCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client request to buy or sell one trader listing. Quantity is decided on the server
 * ({@code stack} means fill one item stack of that listing).
 */
public record TraderTradePayload(int containerId, int offerIndex, boolean sell, boolean stack)
        implements CustomPacketPayload {
    public static final Type<TraderTradePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "trader_trade")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, TraderTradePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    TraderTradePayload::containerId,
                    ByteBufCodecs.VAR_INT,
                    TraderTradePayload::offerIndex,
                    ByteBufCodecs.BOOL,
                    TraderTradePayload::sell,
                    ByteBufCodecs.BOOL,
                    TraderTradePayload::stack,
                    TraderTradePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(TraderTradePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!(player.containerMenu instanceof TraderMenu menu)) {
                return;
            }
            if (menu.containerId != payload.containerId() || !menu.stillValid(player)) {
                return;
            }
            if (payload.sell()) {
                TraderTrades.sell(player, menu, payload.offerIndex(), payload.stack());
            } else {
                TraderTrades.buy(player, menu, payload.offerIndex(), payload.stack());
            }
        });
    }
}
