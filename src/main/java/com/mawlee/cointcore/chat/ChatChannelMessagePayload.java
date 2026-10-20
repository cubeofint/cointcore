package com.mawlee.cointcore.chat;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.chat.client.ChatChannelClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server → client: a chat line tagged with its channel so the client tabs can filter it. */
public record ChatChannelMessagePayload(int channel, Component message) implements CustomPacketPayload {
    public static final Type<ChatChannelMessagePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "chat_channel_message")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ChatChannelMessagePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    ChatChannelMessagePayload::channel,
                    ComponentSerialization.TRUSTED_STREAM_CODEC,
                    ChatChannelMessagePayload::message,
                    ChatChannelMessagePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(ChatChannelMessagePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ChatChannelClient.receive(payload.channel(), payload.message()));
    }
}
