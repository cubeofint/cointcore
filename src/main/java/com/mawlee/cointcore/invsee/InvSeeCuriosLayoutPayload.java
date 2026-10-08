package com.mawlee.cointcore.invsee;

import com.mawlee.cointcore.CointCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record InvSeeCuriosLayoutPayload(int containerId, List<InvSeeCurioSlotMeta> slots)
        implements CustomPacketPayload {
    public static final Type<InvSeeCuriosLayoutPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "invsee_curios_layout")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, InvSeeCuriosLayoutPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    InvSeeCuriosLayoutPayload::containerId,
                    InvSeeCurioSlotMeta.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    InvSeeCuriosLayoutPayload::slots,
                    InvSeeCuriosLayoutPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(InvSeeCuriosLayoutPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> InvSeeClientDispatchers.applyCuriosLayout(payload));
    }
}
