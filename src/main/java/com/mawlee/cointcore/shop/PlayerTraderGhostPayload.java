package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.CointCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sets the manage-screen phantom template. Empty stack clears it.
 * The item is never taken from the player; listing still pulls escrow from inventory.
 */
public record PlayerTraderGhostPayload(int containerId, ItemStack template) implements CustomPacketPayload {
    public static final Type<PlayerTraderGhostPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "player_trader_ghost")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerTraderGhostPayload> STREAM_CODEC =
            StreamCodec.of(PlayerTraderGhostPayload::encode, PlayerTraderGhostPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, PlayerTraderGhostPayload payload) {
        ByteBufCodecs.VAR_INT.encode(buffer, payload.containerId);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, payload.template == null ? ItemStack.EMPTY : payload.template);
    }

    private static PlayerTraderGhostPayload decode(RegistryFriendlyByteBuf buffer) {
        int containerId = ByteBufCodecs.VAR_INT.decode(buffer);
        ItemStack template = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
        return new PlayerTraderGhostPayload(containerId, template);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(PlayerTraderGhostPayload payload, IPayloadContext context) {
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
                return;
            }
            ItemStack incoming = payload.template == null ? ItemStack.EMPTY : payload.template;
            if (incoming.isEmpty()) {
                menu.setGhost(ItemStack.EMPTY);
                return;
            }
            ItemStack unit = GhostTemplate.sanitize(incoming);
            if (!unit.isEmpty()) {
                menu.setGhost(unit);
            }
        });
    }
}
