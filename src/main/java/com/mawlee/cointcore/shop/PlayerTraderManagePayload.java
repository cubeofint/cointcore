package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.CointCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PlayerTraderManagePayload(
        int containerId,
        Action action,
        int offerIndex,
        ItemStack template,
        int count,
        long buyPrice,
        long sellPrice
) implements CustomPacketPayload {
    public enum Action {
        SAVE,
        DELETE
    }

    public static final Type<PlayerTraderManagePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "player_trader_manage")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerTraderManagePayload> STREAM_CODEC =
            StreamCodec.of(PlayerTraderManagePayload::encode, PlayerTraderManagePayload::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, PlayerTraderManagePayload payload) {
        ByteBufCodecs.VAR_INT.encode(buffer, payload.containerId);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.action.ordinal());
        ByteBufCodecs.VAR_INT.encode(buffer, payload.offerIndex);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, payload.template == null ? ItemStack.EMPTY : payload.template);
        ByteBufCodecs.VAR_INT.encode(buffer, payload.count);
        buffer.writeLong(payload.buyPrice);
        buffer.writeLong(payload.sellPrice);
    }

    private static PlayerTraderManagePayload decode(RegistryFriendlyByteBuf buffer) {
        int containerId = ByteBufCodecs.VAR_INT.decode(buffer);
        int actionOrd = ByteBufCodecs.VAR_INT.decode(buffer);
        Action action = actionOrd == Action.DELETE.ordinal() ? Action.DELETE : Action.SAVE;
        int offerIndex = ByteBufCodecs.VAR_INT.decode(buffer);
        ItemStack template = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
        int count = ByteBufCodecs.VAR_INT.decode(buffer);
        long buy = buffer.readLong();
        long sell = buffer.readLong();
        return new PlayerTraderManagePayload(containerId, action, offerIndex, template, count, buy, sell);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(PlayerTraderManagePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!(player.containerMenu instanceof PlayerTraderManageMenu menu)) {
                return;
            }
            if (menu.containerId != payload.containerId() || !menu.stillValid(player)) {
                return;
            }
            menu.access().evaluate((level, pos) -> {
                if (!(level.getBlockEntity(pos) instanceof PlayerTraderBlockEntity shop)) {
                    return false;
                }
                if (!shop.canManage(player)) {
                    player.closeContainer();
                    return false;
                }
                boolean ok;
                if (payload.action == Action.DELETE) {
                    ok = shop.deleteOffer(payload.offerIndex);
                } else {
                    ItemStack template = payload.template == null || payload.template.isEmpty()
                            ? menu.ghostItem()
                            : payload.template;
                    ok = shop.saveOffer(
                            payload.offerIndex,
                            template,
                            payload.count,
                            payload.buyPrice,
                            payload.sellPrice,
                            player.getUUID(),
                            player.getGameProfile().getName()
                    );
                }
                menu.refresh(shop.ownerName(), shop.lifetimeRevenue(), PlayerShopOfferSnapshot.of(shop));
                PacketDistributor.sendToPlayer(
                        player,
                        new PlayerTraderManageSyncPayload(
                                menu.containerId,
                                shop.ownerName(),
                                shop.lifetimeRevenue(),
                                PlayerShopOfferSnapshot.of(shop),
                                ok
                        )
                );
                return true;
            }, false);
        });
    }
}
