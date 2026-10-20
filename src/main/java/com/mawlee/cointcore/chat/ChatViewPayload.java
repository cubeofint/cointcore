package com.mawlee.cointcore.chat;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.chat.client.ChatChannelClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → client: {@code /chat <view>} typed by a player whose client filters chat itself, or
 * {@link #ACTIVATE_ONLY} on login to show the tabs without changing the view.
 */
public record ChatViewPayload(int view) implements CustomPacketPayload {
    public static final int ACTIVATE_ONLY = -1;

    public static final Type<ChatViewPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "chat_view")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ChatViewPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ChatViewPayload::view, ChatViewPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(ChatViewPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ChatChannelClient.setView(payload.view()));
    }
}
