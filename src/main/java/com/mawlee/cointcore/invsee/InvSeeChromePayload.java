package com.mawlee.cointcore.invsee;

import com.mawlee.cointcore.CointCore;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record InvSeeChromePayload(UUID targetId, boolean online, int tabMask, int activeTab) implements CustomPacketPayload {
    public static final Type<InvSeeChromePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "invsee_chrome")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, InvSeeChromePayload> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC,
                    InvSeeChromePayload::targetId,
                    ByteBufCodecs.BOOL,
                    InvSeeChromePayload::online,
                    ByteBufCodecs.VAR_INT,
                    InvSeeChromePayload::tabMask,
                    ByteBufCodecs.VAR_INT,
                    InvSeeChromePayload::activeTab,
                    InvSeeChromePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(InvSeeChromePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> InvSeeClientDispatchers.applyChrome(payload));
    }
}
