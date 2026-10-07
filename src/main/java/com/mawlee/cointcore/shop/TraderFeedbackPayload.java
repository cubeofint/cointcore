package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.shop.client.TraderClientFeedback;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TraderFeedbackPayload(
        int containerId,
        TraderFeedbackKind kind,
        int count,
        String itemName,
        long gluons,
        long fee
) implements CustomPacketPayload {
    public static final Type<TraderFeedbackPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "trader_feedback")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, TraderFeedbackPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    TraderFeedbackPayload::containerId,
                    ByteBufCodecs.VAR_INT.map(TraderFeedbackKind::fromOrdinal, TraderFeedbackKind::ordinal),
                    TraderFeedbackPayload::kind,
                    ByteBufCodecs.VAR_INT,
                    TraderFeedbackPayload::count,
                    ByteBufCodecs.STRING_UTF8,
                    TraderFeedbackPayload::itemName,
                    ByteBufCodecs.VAR_LONG,
                    TraderFeedbackPayload::gluons,
                    ByteBufCodecs.VAR_LONG,
                    TraderFeedbackPayload::fee,
                    TraderFeedbackPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(TraderFeedbackPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> TraderClientFeedback.handle(payload));
    }
}
