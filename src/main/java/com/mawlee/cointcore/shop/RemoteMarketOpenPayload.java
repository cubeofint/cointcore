package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.lang.CointCoreMessages;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client request to open the buy-only remote market. Carries no trade data. */
public record RemoteMarketOpenPayload() implements CustomPacketPayload {
    public static final Type<RemoteMarketOpenPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "remote_market_open")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, RemoteMarketOpenPayload> STREAM_CODEC =
            StreamCodec.unit(new RemoteMarketOpenPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(RemoteMarketOpenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!RemoteMarketAccess.canUse(player)) {
                player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.MARKET_REMOTE_DENIED));
                return;
            }
            PlayerTraderMenus.openRemote(player);
        });
    }
}
