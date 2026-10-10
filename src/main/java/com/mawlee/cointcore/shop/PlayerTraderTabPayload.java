package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.CointCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PlayerTraderTabPayload(int containerId, boolean manage) implements CustomPacketPayload {
    public static final Type<PlayerTraderTabPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "player_trader_tab")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerTraderTabPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    PlayerTraderTabPayload::containerId,
                    ByteBufCodecs.BOOL,
                    PlayerTraderTabPayload::manage,
                    PlayerTraderTabPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(PlayerTraderTabPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ContainerLevelAccess access;
            if (player.containerMenu instanceof PlayerTraderMenu menu
                    && menu.containerId == payload.containerId()) {
                access = menu.access();
            } else if (player.containerMenu instanceof PlayerTraderManageMenu menu
                    && menu.containerId == payload.containerId()) {
                access = menu.access();
            } else {
                return;
            }
            access.evaluate((level, pos) -> {
                if (!(level.getBlockEntity(pos) instanceof PlayerTraderBlockEntity shop)) {
                    return false;
                }
                boolean manage = PlayerShopAccess.allowOpenManage(payload.manage(), player, shop.ownerId());
                PlayerTraderMenus.open(player, shop, manage);
                return true;
            }, false);
        });
    }
}
