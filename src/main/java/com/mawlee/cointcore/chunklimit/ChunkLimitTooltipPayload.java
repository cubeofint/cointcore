package com.mawlee.cointcore.chunklimit;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.chunklimit.client.ChunkLimitTooltipClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → client: placement limits per item (chunk / team / player; -1 = none), so the client can show them in
 * tooltips. Sent on login and whenever {@code chunk-limits.json} rules change.
 */
public record ChunkLimitTooltipPayload(List<Entry> entries) implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 50_000;

    /** {@code *Label} is the shared group's human label from {@code chunk-limits.json} ("" when none). */
    public record Entry(String itemId, int chunk, String chunkLabel, int team, String teamLabel, int player, String playerLabel) {
    }

    public static final Type<ChunkLimitTooltipPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "chunk_limit_tooltips")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ChunkLimitTooltipPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.entries.size());
                for (Entry entry : payload.entries) {
                    buffer.writeUtf(entry.itemId());
                    buffer.writeVarInt(entry.chunk() + 1);
                    buffer.writeUtf(entry.chunkLabel());
                    buffer.writeVarInt(entry.team() + 1);
                    buffer.writeUtf(entry.teamLabel());
                    buffer.writeVarInt(entry.player() + 1);
                    buffer.writeUtf(entry.playerLabel());
                }
            },
            buffer -> {
                int size = Math.min(MAX_ENTRIES, buffer.readVarInt());
                List<Entry> entries = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    entries.add(new Entry(buffer.readUtf(), buffer.readVarInt() - 1, buffer.readUtf(),
                            buffer.readVarInt() - 1, buffer.readUtf(), buffer.readVarInt() - 1, buffer.readUtf()));
                }
                return new ChunkLimitTooltipPayload(List.copyOf(entries));
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(CointCore.MOD_ID).optional().playToClient(TYPE, STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ChunkLimitTooltipClient.accept(payload)));
    }
}
