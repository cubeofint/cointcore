package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.CointCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.mawlee.cointcore.shop.client.PlayerTraderClientSync;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record PlayerTraderManageSyncPayload(
        int containerId,
        String ownerName,
        long lifetimeRevenue,
        List<PlayerShopOfferSnapshot> offers,
        boolean lastActionOk,
        int returnCount,
        List<MarketPriceHint> priceHints
) implements CustomPacketPayload {
    public static final Type<PlayerTraderManageSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "player_trader_manage_sync")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerTraderManageSyncPayload> STREAM_CODEC =
            StreamCodec.of(PlayerTraderManageSyncPayload::encode, PlayerTraderManageSyncPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, PlayerTraderManageSyncPayload payload) {
        ByteBufCodecs.VAR_INT.encode(buffer, payload.containerId);
        ByteBufCodecs.STRING_UTF8.encode(buffer, payload.ownerName == null ? "" : payload.ownerName);
        buffer.writeLong(payload.lifetimeRevenue);
        buffer.writeBoolean(payload.lastActionOk);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.returnCount);
        PlayerShopOfferSnapshot.writeList(buffer, payload.offers);
        MarketPriceHint.writeList(buffer, payload.priceHints);
    }

    private static PlayerTraderManageSyncPayload decode(RegistryFriendlyByteBuf buffer) {
        int containerId = ByteBufCodecs.VAR_INT.decode(buffer);
        String ownerName = ByteBufCodecs.STRING_UTF8.decode(buffer);
        long revenue = buffer.readLong();
        boolean ok = buffer.readBoolean();
        int returns = ByteBufCodecs.VAR_INT.decode(buffer);
        return new PlayerTraderManageSyncPayload(
                containerId,
                ownerName,
                revenue,
                PlayerShopOfferSnapshot.readList(buffer),
                ok,
                returns,
                MarketPriceHint.readList(buffer)
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(PlayerTraderManageSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PlayerTraderClientSync.applyManage(payload));
    }
}
