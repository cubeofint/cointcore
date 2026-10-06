package com.mawlee.cointcore.claim;

import com.mawlee.cointcore.CointCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClaimFlagEditSyncPayload(int mask) implements CustomPacketPayload {
    public static final Type<ClaimFlagEditSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "claim_flag_edit_sync")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ClaimFlagEditSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    ClaimFlagEditSyncPayload::mask,
                    ClaimFlagEditSyncPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(ClaimFlagEditSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClaimFlagEditClient.apply(payload.mask()));
    }
}
