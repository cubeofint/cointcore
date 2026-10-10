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
        int deals,
        long buyPrice,
        long sellPrice
) implements CustomPacketPayload {
    public enum Action {
        SAVE,
        DELETE,
        CLAIM
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
        ByteBufCodecs.VAR_INT.encode(buffer, payload.deals);
        buffer.writeLong(payload.buyPrice);
        buffer.writeLong(payload.sellPrice);
    }

    private static PlayerTraderManagePayload decode(RegistryFriendlyByteBuf buffer) {
        int containerId = ByteBufCodecs.VAR_INT.decode(buffer);
        int actionOrd = ByteBufCodecs.VAR_INT.decode(buffer);
        Action action = switch (actionOrd) {
            case 1 -> Action.DELETE;
            case 2 -> Action.CLAIM;
            default -> Action.SAVE;
        };
        int offerIndex = ByteBufCodecs.VAR_INT.decode(buffer);
        ItemStack template = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
        int count = ByteBufCodecs.VAR_INT.decode(buffer);
        int deals = ByteBufCodecs.VAR_INT.decode(buffer);
        long buy = buffer.readLong();
        long sell = buffer.readLong();
        return new PlayerTraderManagePayload(containerId, action, offerIndex, template, count, deals, buy, sell);
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
            if (!PlayerShopAccess.canUse(player)) {
                player.closeContainer();
                return;
            }
            boolean ok;
            switch (payload.action) {
                case CLAIM -> {
                    GlobalMarketService.claimReturns(player);
                    ok = true;
                }
                case DELETE -> {
                    if (payload.offerIndex < 0 || payload.offerIndex >= menu.offers().size()) {
                        ok = false;
                    } else {
                        ok = GlobalMarketService.cancel(player, menu.offers().get(payload.offerIndex).id());
                    }
                }
                case SAVE -> {
                    ItemStack template = payload.template == null || payload.template.isEmpty()
                            ? menu.ghostItem()
                            : payload.template;
                    int price = payload.buyPrice > Integer.MAX_VALUE
                            ? Integer.MAX_VALUE
                            : (int) Math.max(0L, payload.buyPrice);
                    ok = GlobalMarketService.create(player, template, payload.count, payload.deals, price);
                }
                default -> {
                    Action unknown = payload.action;
                    throw new IllegalStateException("unexpected manage action " + unknown);
                }
            }
            sync(player, menu, ok);
        });
    }

    private static void sync(ServerPlayer player, PlayerTraderManageMenu menu, boolean ok) {
        GlobalMarketSavedData.SoldStats stats = GlobalMarketSavedData.get(player.server).stats(player.getUUID());
        var offers = GlobalMarketService.ownSnapshots(player.server, player.getUUID());
        int returns = GlobalMarketSavedData.get(player.server).returnCount(player.getUUID());
        menu.refresh(player.getGameProfile().getName(), stats.gluons(), offers, returns);
        PacketDistributor.sendToPlayer(
                player,
                new PlayerTraderManageSyncPayload(
                        menu.containerId,
                        player.getGameProfile().getName(),
                        stats.gluons(),
                        offers,
                        ok,
                        returns
                )
        );
    }
}
