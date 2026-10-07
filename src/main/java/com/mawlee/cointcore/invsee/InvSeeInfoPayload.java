package com.mawlee.cointcore.invsee;

import com.mawlee.cointcore.CointCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record InvSeeInfoPayload(String kind, List<String> lines) implements CustomPacketPayload {
    public static final Type<InvSeeInfoPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "invsee_info")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, InvSeeInfoPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    InvSeeInfoPayload::kind,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                    InvSeeInfoPayload::lines,
                    InvSeeInfoPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(InvSeeInfoPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> InvSeeClientDispatchers.applyInfo(payload));
    }
}
