package com.mawlee.cointcore.invsee;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * One real Curios slot: type id, handler index, cosmetic flag.
 */
public record InvSeeCurioSlotMeta(String identifier, int index, boolean cosmetic) {
    public static final StreamCodec<FriendlyByteBuf, InvSeeCurioSlotMeta> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    InvSeeCurioSlotMeta::identifier,
                    ByteBufCodecs.VAR_INT,
                    InvSeeCurioSlotMeta::index,
                    ByteBufCodecs.BOOL,
                    InvSeeCurioSlotMeta::cosmetic,
                    InvSeeCurioSlotMeta::new
            );

    public boolean present() {
        return identifier != null && !identifier.isEmpty();
    }

    public static void writeList(FriendlyByteBuf buf, List<InvSeeCurioSlotMeta> slots) {
        buf.writeVarInt(slots.size());
        for (InvSeeCurioSlotMeta slot : slots) {
            STREAM_CODEC.encode(buf, slot);
        }
    }

    public static List<InvSeeCurioSlotMeta> readList(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<InvSeeCurioSlotMeta> slots = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            slots.add(STREAM_CODEC.decode(buf));
        }
        return List.copyOf(slots);
    }
}
