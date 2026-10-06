package com.mawlee.cointcore.seeinvisible;

import com.mawlee.cointcore.CointCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SeeInvisibleSyncPayload(boolean canSee) implements CustomPacketPayload {
    public static final Type<SeeInvisibleSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "see_invisible_sync")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SeeInvisibleSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    SeeInvisibleSyncPayload::canSee,
                    SeeInvisibleSyncPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(SeeInvisibleSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SeeInvisibleClient.setCanSeeInvisible(payload.canSee()));
    }
}
