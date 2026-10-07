package com.mawlee.cointcore.invsee;

import com.mawlee.cointcore.CointCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record InvSeeOpenNestedPayload(int containerId, int slotIndex) implements CustomPacketPayload {
    public static final Type<InvSeeOpenNestedPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "invsee_open_nested")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, InvSeeOpenNestedPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    InvSeeOpenNestedPayload::containerId,
                    ByteBufCodecs.VAR_INT,
                    InvSeeOpenNestedPayload::slotIndex,
                    InvSeeOpenNestedPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(InvSeeOpenNestedPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                InvSeeService.openNestedFromSlot(player, payload.containerId(), payload.slotIndex());
            }
        });
    }
}
