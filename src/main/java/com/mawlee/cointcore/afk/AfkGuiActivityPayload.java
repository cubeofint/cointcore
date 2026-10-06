package com.mawlee.cointcore.afk;

import com.mawlee.cointcore.CointCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server signal that the player is actively using an open container GUI
 * (mouse/keyboard). Server only accepts it when a real container menu is open.
 */
public record AfkGuiActivityPayload() implements CustomPacketPayload {
    public static final AfkGuiActivityPayload INSTANCE = new AfkGuiActivityPayload();

    public static final Type<AfkGuiActivityPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "afk_gui_activity")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, AfkGuiActivityPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(AfkGuiActivityPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                AfkService.onGuiHeartbeat(player);
            }
        });
    }
}
