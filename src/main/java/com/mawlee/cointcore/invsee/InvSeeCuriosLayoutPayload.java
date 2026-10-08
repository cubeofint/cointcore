package com.mawlee.cointcore.invsee;

import com.mawlee.cointcore.CointCore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record InvSeeCuriosLayoutPayload(int containerId, List<InvSeeCurioSlotMeta> slots)
        implements CustomPacketPayload {
    public static final Type<InvSeeCuriosLayoutPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "invsee_curios_layout")
    );

    public static final StreamCodec<FriendlyByteBuf, InvSeeCurioSlotMeta> SLOT_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    InvSeeCurioSlotMeta::identifier,
                    ByteBufCodecs.VAR_INT,
                    InvSeeCurioSlotMeta::index,
                    ByteBufCodecs.BOOL,
                    InvSeeCurioSlotMeta::cosmetic,
                    InvSeeCurioSlotMeta::new
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, InvSeeCuriosLayoutPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    InvSeeCuriosLayoutPayload::containerId,
                    SLOT_CODEC.apply(ByteBufCodecs.list()),
                    InvSeeCuriosLayoutPayload::slots,
                    InvSeeCuriosLayoutPayload::new
            );

    public static void writeList(FriendlyByteBuf buf, List<InvSeeCurioSlotMeta> slots) {
        buf.writeVarInt(slots.size());
        for (InvSeeCurioSlotMeta slot : slots) {
            SLOT_CODEC.encode(buf, slot);
        }
    }

    public static List<InvSeeCurioSlotMeta> readList(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<InvSeeCurioSlotMeta> slots = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            slots.add(SLOT_CODEC.decode(buf));
        }
        return List.copyOf(slots);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(InvSeeCuriosLayoutPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> InvSeeClientDispatchers.applyCuriosLayout(payload));
    }
}
