package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.CointCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → client on login: whether the player has {@code cointcore.playershop.admin}. The client has no LuckPerms,
 * and without this a moderator who is not op level 2 got zero mining progress on shop blocks in survival.
 * Only used for the client's mining animation; the server still decides every break.
 */
public record PlayerShopAdminPayload(boolean admin) implements CustomPacketPayload {
    public static final Type<PlayerShopAdminPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "player_shop_admin")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerShopAdminPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, PlayerShopAdminPayload::admin, PlayerShopAdminPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void sendTo(ServerPlayer player) {
        if (player.connection != null && player.connection.hasChannel(TYPE)) {
            PacketDistributor.sendToPlayer(player, new PlayerShopAdminPayload(PlayerShopAccess.canAdmin(player)));
        }
    }

    public static void handleClient(PlayerShopAdminPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PlayerShopAccess.setClientAdmin(payload.admin()));
    }
}
