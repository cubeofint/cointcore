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
 * Client request to buy or sell. For the NPC terminal, {@code stack} fills one item stack.
 * For the global market, {@code deals} is the requested deal count (all-or-nothing).
 */
public record TraderTradePayload(int containerId, int offerIndex, boolean sell, boolean stack, long deals)
        implements CustomPacketPayload {
    public static final Type<TraderTradePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "trader_trade")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, TraderTradePayload> STREAM_CODEC =
            StreamCodec.of(TraderTradePayload::encode, TraderTradePayload::decode);

    public TraderTradePayload(int containerId, int offerIndex, boolean sell, boolean stack) {
        this(containerId, offerIndex, sell, stack, 0L);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, TraderTradePayload payload) {
        ByteBufCodecs.VAR_INT.encode(buffer, payload.containerId);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.offerIndex);
        ByteBufCodecs.BOOL.encode(buffer, payload.sell);
        ByteBufCodecs.BOOL.encode(buffer, payload.stack);
        buffer.writeLong(payload.deals);
    }

    private static TraderTradePayload decode(RegistryFriendlyByteBuf buffer) {
        return new TraderTradePayload(
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.BOOL.decode(buffer),
                buffer.readLong()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(TraderTradePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (player.containerMenu instanceof TraderMenu menu) {
                if (menu.containerId != payload.containerId() || !menu.stillValid(player)) {
                    return;
                }
                if (payload.sell()) {
                    TraderTrades.sell(player, menu, payload.offerIndex(), payload.stack());
                } else {
                    TraderTrades.buy(player, menu, payload.offerIndex(), payload.stack());
                }
                return;
            }
            if (!(player.containerMenu instanceof PlayerTraderMenu menu)) {
                return;
            }
            if (menu.containerId != payload.containerId() || !menu.stillValid(player)) {
                return;
            }
            menu.access().evaluate((level, pos) -> {
                if (!(level.getBlockEntity(pos) instanceof PlayerTraderBlockEntity shop)) {
                    return false;
                }
                if (payload.sell()) {
                    return false;
                }
                if (payload.offerIndex() < 0 || payload.offerIndex() >= menu.listings().size()) {
                    return false;
                }
                GlobalMarketService.buy(
                        player,
                        menu,
                        menu.listings().get(payload.offerIndex()).listingId(),
                        payload.deals()
                );
                return true;
            }, false);
        });
    }
}
