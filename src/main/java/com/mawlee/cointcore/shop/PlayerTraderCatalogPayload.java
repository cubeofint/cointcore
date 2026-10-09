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

public record PlayerTraderCatalogPayload(
        int containerId,
        long balance,
        String ownerName,
        List<PlayerTraderListing> listings
) implements CustomPacketPayload {
    public static final Type<PlayerTraderCatalogPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "player_trader_catalog")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerTraderCatalogPayload> STREAM_CODEC =
            StreamCodec.of(PlayerTraderCatalogPayload::encode, PlayerTraderCatalogPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, PlayerTraderCatalogPayload payload) {
        ByteBufCodecs.VAR_INT.encode(buffer, payload.containerId);
        buffer.writeLong(payload.balance);
        ByteBufCodecs.STRING_UTF8.encode(buffer, payload.ownerName == null ? "" : payload.ownerName);
        PlayerTraderMenu.writeListings(buffer, payload.listings);
    }

    private static PlayerTraderCatalogPayload decode(RegistryFriendlyByteBuf buffer) {
        int containerId = ByteBufCodecs.VAR_INT.decode(buffer);
        long balance = buffer.readLong();
        String ownerName = ByteBufCodecs.STRING_UTF8.decode(buffer);
        return new PlayerTraderCatalogPayload(containerId, balance, ownerName, PlayerTraderMenu.readListings(buffer));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(PlayerTraderCatalogPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PlayerTraderClientSync.applyCatalog(payload));
    }
}
