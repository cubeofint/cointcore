package com.mawlee.cointcore.radio;

import com.mawlee.cointcore.CointCore;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class CointRadioNetwork {
    private CointRadioNetwork() {}
    public static void register(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(CointCore.MOD_ID).versioned("1");
        registrar.playToClient(PlayRadioPacket.TYPE, PlayRadioPacket.CODEC, PlayRadioPacket::handle);
        registrar.playToClient(StopRadioPacket.TYPE, StopRadioPacket.CODEC, StopRadioPacket::handle);
        registrar.playToClient(ToggleRadioPacket.TYPE, ToggleRadioPacket.CODEC, ToggleRadioPacket::handle);
        registrar.playToClient(SwitchRadioPacket.TYPE, SwitchRadioPacket.CODEC, SwitchRadioPacket::handle);
        registrar.playToClient(OpenRadioScreenPacket.TYPE, OpenRadioScreenPacket.CODEC, OpenRadioScreenPacket::handle);
        registrar.playToServer(SelectStationPacket.TYPE, SelectStationPacket.CODEC, SelectStationPacket::handle);
        registrar.playToServer(ToggleActivePacket.TYPE, ToggleActivePacket.CODEC, ToggleActivePacket::handle);
        registrar.playToServer(SetCustomUrlPacket.TYPE, SetCustomUrlPacket.CODEC, SetCustomUrlPacket::handle);
        registrar.playToServer(ClearCustomUrlPacket.TYPE, ClearCustomUrlPacket.CODEC, ClearCustomUrlPacket::handle);
        registrar.playToServer(NextStationPacket.TYPE, NextStationPacket.CODEC, NextStationPacket::handle);
        registrar.playToServer(RandomStationPacket.TYPE, RandomStationPacket.CODEC, RandomStationPacket::handle);
        registrar.playToServer(SetRadiusPacket.TYPE, SetRadiusPacket.CODEC, SetRadiusPacket::handle);
        registrar.playToClient(YouTubeTransferStartPacket.TYPE, YouTubeTransferStartPacket.CODEC, YouTubeTransferStartPacket::handle);
        registrar.playToClient(YouTubeTransferChunkPacket.TYPE, YouTubeTransferChunkPacket.CODEC, YouTubeTransferChunkPacket::handle);
        registrar.playToClient(YouTubeTransferFinishPacket.TYPE, YouTubeTransferFinishPacket.CODEC, YouTubeTransferFinishPacket::handle);
        registrar.playToClient(YouTubeTransferFailPacket.TYPE, YouTubeTransferFailPacket.CODEC, YouTubeTransferFailPacket::handle);
        registrar.playToClient(LiveTranscodeStartPacket.TYPE, LiveTranscodeStartPacket.CODEC, LiveTranscodeStartPacket::handle);
        registrar.playToClient(LiveTranscodeChunkPacket.TYPE, LiveTranscodeChunkPacket.CODEC, LiveTranscodeChunkPacket::handle);
        registrar.playToClient(LiveTranscodeStopPacket.TYPE, LiveTranscodeStopPacket.CODEC, LiveTranscodeStopPacket::handle);
        registrar.playToClient(LiveTranscodeFailPacket.TYPE, LiveTranscodeFailPacket.CODEC, LiveTranscodeFailPacket::handle);
    }

    public static void sendPlay(ServerPlayer player, String url, String stationId, String radioId) {
        PacketDistributor.sendToPlayer(player, new PlayRadioPacket(url, stationId, radioId));
    }

    public static void sendStop(ServerPlayer player, String radioId) {
        CointRadioTranscodeSession.stop(player.getUUID(), radioId);
        PacketDistributor.sendToPlayer(player, new StopRadioPacket(radioId));
    }

    public static void sendToggle(ServerPlayer player, String url, String stationId, String radioId) {
        PacketDistributor.sendToPlayer(player, new ToggleRadioPacket(url, stationId, radioId));
    }

    public static void sendSwitch(ServerPlayer player, String url, String stationId, String radioId) {
        PacketDistributor.sendToPlayer(player, new SwitchRadioPacket(url, stationId, radioId));
    }

    public static void sendOpenScreen(
            ServerPlayer player,
            BlockPos pos,
            List<String> stations,
            String currentStation,
            boolean active,
            int radius,
            String customUrl
    ) {
        PacketDistributor.sendToPlayer(player,
                new OpenRadioScreenPacket(pos, stations, currentStation, active, radius, customUrl)
        );
    }

    public static void sendSetCustomUrlToServer(BlockPos pos, String customUrl) {
        PacketDistributor.sendToServer(new SetCustomUrlPacket(pos, customUrl == null ? "" : customUrl));
    }

    public static void sendClearCustomUrlToServer(BlockPos pos) {
        PacketDistributor.sendToServer(new ClearCustomUrlPacket(pos));
    }

    public static void sendSelectStationToServer(BlockPos pos, String stationId) {
        PacketDistributor.sendToServer(new SelectStationPacket(pos, stationId));
    }

    public static void sendToggleActiveToServer(BlockPos pos) {
        PacketDistributor.sendToServer(new ToggleActivePacket(pos));
    }

    public static void sendNextStationToServer(BlockPos pos) {
        PacketDistributor.sendToServer(new NextStationPacket(pos));
    }

    public static void sendRandomStationToServer(BlockPos pos) {
        PacketDistributor.sendToServer(new RandomStationPacket(pos));
    }

    public static void sendSetRadiusToServer(BlockPos pos, int radius) {
        PacketDistributor.sendToServer(new SetRadiusPacket(pos, radius));
    }

    public static void sendTranscodedRadio(ServerPlayer player, String url, String station, String radioId) {
        CointRadioTranscodeSession.start(player, url, station, radioId);
    }

    static void sendLiveTranscodeStart(ServerPlayer player, UUID transferId, String station, String radioId) {
        PacketDistributor.sendToPlayer(player, new LiveTranscodeStartPacket(transferId, station, radioId));
    }

    static void sendLiveTranscodeChunk(ServerPlayer player, UUID transferId, byte[] data) {
        PacketDistributor.sendToPlayer(player, new LiveTranscodeChunkPacket(transferId, data));
    }

    static void sendLiveTranscodeFail(ServerPlayer player, UUID transferId, String message) {
        PacketDistributor.sendToPlayer(player, new LiveTranscodeFailPacket(transferId, message == null ? "" : message));
    }

    public static void sendYouTubeCachedMp3(
            ServerPlayer player,
            String youtubeUrl,
            String station,
            String radioId,
            long startOffsetMs
    ) {
        if (!CointRadioExternalTools.isYouTubeAvailable()) {
            UUID transferId = UUID.randomUUID();

            PacketDistributor.sendToPlayer(player,
                    new YouTubeTransferFailPacket(transferId, "YouTube-ссылки не поддерживаются серверным радио.")
            );
            return;
        }

        CompletableFuture.runAsync(() -> {
            UUID transferId = UUID.randomUUID();

            try {
                Path mp3 = CointYouTubeCache.getOrCreateCachedMp3(youtubeUrl);
                byte[] bytes = Files.readAllBytes(mp3);

                if (bytes.length <= 64 * 1024) {
                    throw new IllegalStateException("Cached MP3 is too small: " + bytes.length);
                }

                if (player.getServer() == null) {
                    throw new IllegalStateException("Player server is null");
                }

                System.out.println("[CointMusic] Sending YouTube MP3 to player: "
                        + player.getGameProfile().getName()
                        + ", bytes="
                        + bytes.length
                        + ", transfer="
                        + transferId
                );

                player.getServer().execute(() -> {
                    PacketDistributor.sendToPlayer(player,
                            new YouTubeTransferStartPacket(
                                    transferId,
                                    station,
                                    radioId,
                                    bytes.length,
                                    Math.max(0L, startOffsetMs)
                            )
                    );

                    int chunkSize = 24 * 1024;

                    for (int offset = 0; offset < bytes.length; offset += chunkSize) {
                        int end = Math.min(bytes.length, offset + chunkSize);
                        byte[] chunk = Arrays.copyOfRange(bytes, offset, end);

                        PacketDistributor.sendToPlayer(player,
                                new YouTubeTransferChunkPacket(transferId, chunk)
                        );
                    }

                    PacketDistributor.sendToPlayer(player,
                            new YouTubeTransferFinishPacket(transferId)
                    );
                });
            } catch (Throwable throwable) {
                System.out.println("[CointMusic] Failed to send YouTube MP3: " + shortError(throwable));

                if (player.getServer() != null) {
                    player.getServer().execute(() -> PacketDistributor.sendToPlayer(player,
                            new YouTubeTransferFailPacket(transferId, throwable.getMessage())
                    ));
                }
            }
        });
    }


    private static String shortError(Throwable throwable) {
        if (throwable == null) {
            return "unknown error";
        }

        String message = throwable.getMessage();

        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }

        int lineBreak = message.indexOf('\n');

        if (lineBreak >= 0) {
            message = message.substring(0, lineBreak);
        }

        if (message.length() > 240) {
            message = message.substring(0, 240) + "...";
        }

        return message;
    }

    private static boolean isAllowedCustomUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        String lowered = url.toLowerCase(java.util.Locale.ROOT);

        return url.length() <= 2048
                && (lowered.startsWith("http://") || lowered.startsWith("https://"));
    }

    private static boolean isValidRadioAccess(ServerPlayer player, BlockPos pos) {
        if (player == null) {
            return false;
        }

        if (!player.level().isLoaded(pos)) {
            return false;
        }

        if (player.distanceToSqr(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5
        ) > 64.0D) {
            return false;
        }

        if (!CointRadioProtection.canUseRadio(player, pos, InteractionHand.MAIN_HAND)) {
            CointRadioProtection.denyWithMessage(player);
            return false;
        }

        return true;
    }

    public record LiveTranscodeStartPacket(UUID transferId, String station, String radioId) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        public static final Type<LiveTranscodeStartPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "radio_live_transcode_start_packet"));
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, LiveTranscodeStartPacket> CODEC = net.minecraft.network.codec.StreamCodec.of((buffer, packet) -> LiveTranscodeStartPacket.encode(packet, buffer), LiveTranscodeStartPacket::decode);
        @Override public Type<LiveTranscodeStartPacket> type() { return TYPE; }
        public static void encode(LiveTranscodeStartPacket packet, net.minecraft.network.FriendlyByteBuf buffer) {
            buffer.writeUUID(packet.transferId);
            buffer.writeUtf(packet.station == null ? "" : packet.station, 256);
            buffer.writeUtf(packet.radioId == null ? "" : packet.radioId, 256);
        }

        public static LiveTranscodeStartPacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
            return new LiveTranscodeStartPacket(buffer.readUUID(), buffer.readUtf(256), buffer.readUtf(256));
        }

        public static void handle(LiveTranscodeStartPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {

            context.enqueueWork(() -> CointRadioClientLiveStream.start(packet.transferId, packet.station, packet.radioId));
        }
    }

    public record LiveTranscodeChunkPacket(UUID transferId, byte[] data) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        public static final Type<LiveTranscodeChunkPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "radio_live_transcode_chunk_packet"));
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, LiveTranscodeChunkPacket> CODEC = net.minecraft.network.codec.StreamCodec.of((buffer, packet) -> LiveTranscodeChunkPacket.encode(packet, buffer), LiveTranscodeChunkPacket::decode);
        @Override public Type<LiveTranscodeChunkPacket> type() { return TYPE; }
        public static void encode(LiveTranscodeChunkPacket packet, net.minecraft.network.FriendlyByteBuf buffer) {
            buffer.writeUUID(packet.transferId);
            buffer.writeByteArray(packet.data == null ? new byte[0] : packet.data);
        }

        public static LiveTranscodeChunkPacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
            return new LiveTranscodeChunkPacket(buffer.readUUID(), buffer.readByteArray(32768));
        }

        public static void handle(LiveTranscodeChunkPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {

            context.enqueueWork(() -> CointRadioClientLiveStream.chunk(packet.transferId, packet.data));
        }
    }

    public record LiveTranscodeStopPacket(UUID transferId) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        public static final Type<LiveTranscodeStopPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "radio_live_transcode_stop_packet"));
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, LiveTranscodeStopPacket> CODEC = net.minecraft.network.codec.StreamCodec.of((buffer, packet) -> LiveTranscodeStopPacket.encode(packet, buffer), LiveTranscodeStopPacket::decode);
        @Override public Type<LiveTranscodeStopPacket> type() { return TYPE; }
        public static void encode(LiveTranscodeStopPacket packet, net.minecraft.network.FriendlyByteBuf buffer) {
            buffer.writeUUID(packet.transferId);
        }

        public static LiveTranscodeStopPacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
            return new LiveTranscodeStopPacket(buffer.readUUID());
        }

        public static void handle(LiveTranscodeStopPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {

            context.enqueueWork(() -> CointRadioClientLiveStream.stop(packet.transferId));
        }
    }

    public record LiveTranscodeFailPacket(UUID transferId, String message) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        public static final Type<LiveTranscodeFailPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "radio_live_transcode_fail_packet"));
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, LiveTranscodeFailPacket> CODEC = net.minecraft.network.codec.StreamCodec.of((buffer, packet) -> LiveTranscodeFailPacket.encode(packet, buffer), LiveTranscodeFailPacket::decode);
        @Override public Type<LiveTranscodeFailPacket> type() { return TYPE; }
        public static void encode(LiveTranscodeFailPacket packet, net.minecraft.network.FriendlyByteBuf buffer) {
            buffer.writeUUID(packet.transferId);
            buffer.writeUtf(packet.message == null ? "" : packet.message, 32767);
        }

        public static LiveTranscodeFailPacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
            return new LiveTranscodeFailPacket(buffer.readUUID(), buffer.readUtf(32767));
        }

        public static void handle(LiveTranscodeFailPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {

            context.enqueueWork(() -> CointRadioClientLiveStream.fail(packet.transferId, packet.message));
        }
    }

    public record YouTubeTransferStartPacket(
            UUID transferId,
            String station,
            String radioId,
            int totalBytes,
            long startOffsetMs
    ) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        public static final Type<YouTubeTransferStartPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "radio_you_tube_transfer_start_packet"));
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, YouTubeTransferStartPacket> CODEC = net.minecraft.network.codec.StreamCodec.of((buffer, packet) -> YouTubeTransferStartPacket.encode(packet, buffer), YouTubeTransferStartPacket::decode);
        @Override public Type<YouTubeTransferStartPacket> type() { return TYPE; }
        public static void encode(YouTubeTransferStartPacket packet, net.minecraft.network.FriendlyByteBuf buffer) {
            buffer.writeUUID(packet.transferId);
            buffer.writeUtf(packet.station, 256);
            buffer.writeUtf(packet.radioId, 256);
            buffer.writeInt(packet.totalBytes);
            buffer.writeLong(packet.startOffsetMs);
        }

        public static YouTubeTransferStartPacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
            return new YouTubeTransferStartPacket(
                    buffer.readUUID(),
                    buffer.readUtf(256),
                    buffer.readUtf(256),
                    buffer.readInt(),
                    buffer.readLong()
            );
        }

        public static void handle(YouTubeTransferStartPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {

            context.enqueueWork(() -> CointYouTubeClientTransfer.start(
                            packet.transferId,
                            packet.station,
                            packet.radioId,
                            packet.totalBytes,
                            packet.startOffsetMs
                    ));
        }
    }

    public record YouTubeTransferChunkPacket(UUID transferId, byte[] data) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        public static final Type<YouTubeTransferChunkPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "radio_you_tube_transfer_chunk_packet"));
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, YouTubeTransferChunkPacket> CODEC = net.minecraft.network.codec.StreamCodec.of((buffer, packet) -> YouTubeTransferChunkPacket.encode(packet, buffer), YouTubeTransferChunkPacket::decode);
        @Override public Type<YouTubeTransferChunkPacket> type() { return TYPE; }
        public static void encode(YouTubeTransferChunkPacket packet, net.minecraft.network.FriendlyByteBuf buffer) {
            buffer.writeUUID(packet.transferId);
            buffer.writeByteArray(packet.data);
        }

        public static YouTubeTransferChunkPacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
            return new YouTubeTransferChunkPacket(
                    buffer.readUUID(),
                    buffer.readByteArray(32768)
            );
        }

        public static void handle(YouTubeTransferChunkPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {

            context.enqueueWork(() -> CointYouTubeClientTransfer.chunk(packet.transferId, packet.data));
        }
    }

    public record YouTubeTransferFinishPacket(UUID transferId) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        public static final Type<YouTubeTransferFinishPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "radio_you_tube_transfer_finish_packet"));
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, YouTubeTransferFinishPacket> CODEC = net.minecraft.network.codec.StreamCodec.of((buffer, packet) -> YouTubeTransferFinishPacket.encode(packet, buffer), YouTubeTransferFinishPacket::decode);
        @Override public Type<YouTubeTransferFinishPacket> type() { return TYPE; }
        public static void encode(YouTubeTransferFinishPacket packet, net.minecraft.network.FriendlyByteBuf buffer) {
            buffer.writeUUID(packet.transferId);
        }

        public static YouTubeTransferFinishPacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
            return new YouTubeTransferFinishPacket(buffer.readUUID());
        }

        public static void handle(YouTubeTransferFinishPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {

            context.enqueueWork(() -> CointYouTubeClientTransfer.finish(packet.transferId));
        }
    }

    public record YouTubeTransferFailPacket(UUID transferId, String message) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        public static final Type<YouTubeTransferFailPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "radio_you_tube_transfer_fail_packet"));
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, YouTubeTransferFailPacket> CODEC = net.minecraft.network.codec.StreamCodec.of((buffer, packet) -> YouTubeTransferFailPacket.encode(packet, buffer), YouTubeTransferFailPacket::decode);
        @Override public Type<YouTubeTransferFailPacket> type() { return TYPE; }
        public static void encode(YouTubeTransferFailPacket packet, net.minecraft.network.FriendlyByteBuf buffer) {
            buffer.writeUUID(packet.transferId);
            buffer.writeUtf(packet.message == null ? "" : packet.message, 32767);
        }

        public static YouTubeTransferFailPacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
            return new YouTubeTransferFailPacket(
                    buffer.readUUID(),
                    buffer.readUtf(32767)
            );
        }

        public static void handle(YouTubeTransferFailPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {

            context.enqueueWork(() -> CointYouTubeClientTransfer.fail(packet.transferId, packet.message));
        }
    }

    public record PlayRadioPacket(String url, String stationId, String radioId) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        public static final Type<PlayRadioPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "radio_play_radio_packet"));
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, PlayRadioPacket> CODEC = net.minecraft.network.codec.StreamCodec.of((buffer, packet) -> PlayRadioPacket.encode(packet, buffer), PlayRadioPacket::decode);
        @Override public Type<PlayRadioPacket> type() { return TYPE; }
        public static void encode(PlayRadioPacket packet, net.minecraft.network.FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.url, 32767);
            buffer.writeUtf(packet.stationId, 256);
            buffer.writeUtf(packet.radioId, 256);
        }

        public static PlayRadioPacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
            return new PlayRadioPacket(buffer.readUtf(32767), buffer.readUtf(256), buffer.readUtf(256));
        }

        public static void handle(PlayRadioPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {

            context.enqueueWork(() -> CointRadioClientPacketHandler.play(packet.url, packet.stationId, packet.radioId));
        }
    }

    public record StopRadioPacket(String radioId) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        public static final Type<StopRadioPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "radio_stop_radio_packet"));
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, StopRadioPacket> CODEC = net.minecraft.network.codec.StreamCodec.of((buffer, packet) -> StopRadioPacket.encode(packet, buffer), StopRadioPacket::decode);
        @Override public Type<StopRadioPacket> type() { return TYPE; }
        public static void encode(StopRadioPacket packet, net.minecraft.network.FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.radioId, 256);
        }

        public static StopRadioPacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
            return new StopRadioPacket(buffer.readUtf(256));
        }

        public static void handle(StopRadioPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {

            context.enqueueWork(() -> {
                        CointYouTubeClientTransfer.cancelRadio(packet.radioId);
                        CointRadioClientLiveStream.cancelRadio(packet.radioId);
                        CointRadioClientPacketHandler.stop(packet.radioId);
                    });
        }
    }

    public record ToggleRadioPacket(String url, String stationId, String radioId) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        public static final Type<ToggleRadioPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "radio_toggle_radio_packet"));
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, ToggleRadioPacket> CODEC = net.minecraft.network.codec.StreamCodec.of((buffer, packet) -> ToggleRadioPacket.encode(packet, buffer), ToggleRadioPacket::decode);
        @Override public Type<ToggleRadioPacket> type() { return TYPE; }
        public static void encode(ToggleRadioPacket packet, net.minecraft.network.FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.url, 32767);
            buffer.writeUtf(packet.stationId, 256);
            buffer.writeUtf(packet.radioId, 256);
        }

        public static ToggleRadioPacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
            return new ToggleRadioPacket(buffer.readUtf(32767), buffer.readUtf(256), buffer.readUtf(256));
        }

        public static void handle(ToggleRadioPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {

            context.enqueueWork(() -> CointRadioClientPacketHandler.toggle(packet.url, packet.stationId, packet.radioId));
        }
    }

    public record SwitchRadioPacket(String url, String stationId, String radioId) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        public static final Type<SwitchRadioPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "radio_switch_radio_packet"));
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, SwitchRadioPacket> CODEC = net.minecraft.network.codec.StreamCodec.of((buffer, packet) -> SwitchRadioPacket.encode(packet, buffer), SwitchRadioPacket::decode);
        @Override public Type<SwitchRadioPacket> type() { return TYPE; }
        public static void encode(SwitchRadioPacket packet, net.minecraft.network.FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.url, 32767);
            buffer.writeUtf(packet.stationId, 256);
            buffer.writeUtf(packet.radioId, 256);
        }

        public static SwitchRadioPacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
            return new SwitchRadioPacket(buffer.readUtf(32767), buffer.readUtf(256), buffer.readUtf(256));
        }

        public static void handle(SwitchRadioPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {

            context.enqueueWork(() -> CointRadioClientPacketHandler.switchStation(packet.url, packet.stationId, packet.radioId));
        }
    }

    public record OpenRadioScreenPacket(
            BlockPos pos,
            List<String> stations,
            String currentStation,
            boolean active,
            int radius,
            String customUrl
    ) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        public static final Type<OpenRadioScreenPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "radio_open_radio_screen_packet"));
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, OpenRadioScreenPacket> CODEC = net.minecraft.network.codec.StreamCodec.of((buffer, packet) -> OpenRadioScreenPacket.encode(packet, buffer), OpenRadioScreenPacket::decode);
        @Override public Type<OpenRadioScreenPacket> type() { return TYPE; }
        public static void encode(OpenRadioScreenPacket packet, net.minecraft.network.FriendlyByteBuf buffer) {
            buffer.writeBlockPos(packet.pos);
            buffer.writeVarInt(packet.stations.size());

            for (String station : packet.stations) {
                buffer.writeUtf(station, 256);
            }

            buffer.writeUtf(packet.currentStation, 256);
            buffer.writeBoolean(packet.active);
            buffer.writeVarInt(packet.radius);
            buffer.writeUtf(packet.customUrl == null ? "" : packet.customUrl, 2048);
        }

        public static OpenRadioScreenPacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
            BlockPos pos = buffer.readBlockPos();

            int size = Math.min(buffer.readVarInt(), 128);
            List<String> stations = new ArrayList<>();

            for (int i = 0; i < size; i++) {
                stations.add(buffer.readUtf(256));
            }

            String currentStation = buffer.readUtf(256);
            boolean active = buffer.readBoolean();
            int radius = buffer.readVarInt();
            String customUrl = buffer.readUtf(2048);

            return new OpenRadioScreenPacket(pos, stations, currentStation, active, radius, customUrl);
        }

        public static void handle(OpenRadioScreenPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {

            context.enqueueWork(() -> CointRadioClientPacketHandler.openScreen(
                            packet.pos,
                            packet.stations,
                            packet.currentStation,
                            packet.active,
                            packet.radius,
                            packet.customUrl
                    ));
        }
    }

    public record SelectStationPacket(BlockPos pos, String stationId) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        public static final Type<SelectStationPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "radio_select_station_packet"));
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, SelectStationPacket> CODEC = net.minecraft.network.codec.StreamCodec.of((buffer, packet) -> SelectStationPacket.encode(packet, buffer), SelectStationPacket::decode);
        @Override public Type<SelectStationPacket> type() { return TYPE; }
        public static void encode(SelectStationPacket packet, net.minecraft.network.FriendlyByteBuf buffer) {
            buffer.writeBlockPos(packet.pos);
            buffer.writeUtf(packet.stationId, 256);
        }

        public static SelectStationPacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
            return new SelectStationPacket(buffer.readBlockPos(), buffer.readUtf(256));
        }

        public static void handle(SelectStationPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {

            context.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) context.player();

                if (!isValidRadioAccess(player, packet.pos)) {
                    return;
                }

                BlockEntity blockEntity = player.level().getBlockEntity(packet.pos);

                if (!(blockEntity instanceof CointRadioBlockEntity radio)) {
                    return;
                }

                radio.setStationId(packet.stationId);

                player.displayClientMessage(
                        Component.literal("§e[CointMusic] Станция выбрана: §f" + CointRadioConfig.getStationName(radio.getStationId())),
                        true
                );
            });
        }
    }

    public record ToggleActivePacket(BlockPos pos) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        public static final Type<ToggleActivePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "radio_toggle_active_packet"));
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, ToggleActivePacket> CODEC = net.minecraft.network.codec.StreamCodec.of((buffer, packet) -> ToggleActivePacket.encode(packet, buffer), ToggleActivePacket::decode);
        @Override public Type<ToggleActivePacket> type() { return TYPE; }
        public static void encode(ToggleActivePacket packet, net.minecraft.network.FriendlyByteBuf buffer) {
            buffer.writeBlockPos(packet.pos);
        }

        public static ToggleActivePacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
            return new ToggleActivePacket(buffer.readBlockPos());
        }

        public static void handle(ToggleActivePacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {

            context.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) context.player();

                if (!isValidRadioAccess(player, packet.pos)) {
                    return;
                }

                BlockEntity blockEntity = player.level().getBlockEntity(packet.pos);

                if (!(blockEntity instanceof CointRadioBlockEntity radio)) {
                    return;
                }

                radio.toggleActive();

                if (radio.isActive()) {
                    player.displayClientMessage(
                            Component.literal("§a[CointMusic] Радио включено. Радиус: §f" + radio.getRadius()),
                            true
                    );
                } else {
                    player.displayClientMessage(
                            Component.literal("§c[CointMusic] Радио выключено."),
                            true
                    );
                }
            });
        }
    }

    public record SetCustomUrlPacket(BlockPos pos, String customUrl) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        public static final Type<SetCustomUrlPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "radio_set_custom_url_packet"));
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, SetCustomUrlPacket> CODEC = net.minecraft.network.codec.StreamCodec.of((buffer, packet) -> SetCustomUrlPacket.encode(packet, buffer), SetCustomUrlPacket::decode);
        @Override public Type<SetCustomUrlPacket> type() { return TYPE; }
        public static void encode(SetCustomUrlPacket packet, net.minecraft.network.FriendlyByteBuf buffer) {
            buffer.writeBlockPos(packet.pos);
            buffer.writeUtf(packet.customUrl == null ? "" : packet.customUrl, 2048);
        }

        public static SetCustomUrlPacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
            return new SetCustomUrlPacket(
                    buffer.readBlockPos(),
                    buffer.readUtf(2048)
            );
        }

        public static void handle(SetCustomUrlPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {

            context.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) context.player();

                if (!isValidRadioAccess(player, packet.pos)) {
                    return;
                }

                BlockEntity blockEntity = player.level().getBlockEntity(packet.pos);

                if (!(blockEntity instanceof CointRadioBlockEntity radio)) {
                    return;
                }

                String cleanUrl = packet.customUrl == null ? "" : packet.customUrl.trim();

                if (!isAllowedCustomUrl(cleanUrl)) {
                    player.displayClientMessage(
                            Component.literal("§c[CointMusic] Нужна http/https ссылка на .ogg/.mp3, плейлист или онлайн-радио."),
                            true
                    );
                    return;
                }

                radio.setCustomUrl(cleanUrl);

                if (CointYouTubeResolver.isYouTubeUrl(cleanUrl) && !CointRadioExternalTools.isYouTubeAvailable()) {
                    player.displayClientMessage(
                            Component.literal("§e[CointMusic] URL сохранён, но YouTube-ссылки не поддерживаются серверным радио."),
                            true
                    );
                    return;
                }

                if (CointRadioTranscodeSession.shouldTranscode(cleanUrl) && !CointRadioExternalTools.isFfmpegAvailable()) {
                    player.displayClientMessage(
                            Component.literal("§e[CointMusic] URL сохранён, но этот формат ссылки не поддерживается серверным радио."),
                            true
                    );
                    return;
                }

                player.displayClientMessage(
                        Component.literal("§a[CointMusic] URL радио применён."),
                        true
                );
            });
        }
    }

    public record ClearCustomUrlPacket(BlockPos pos) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        public static final Type<ClearCustomUrlPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "radio_clear_custom_url_packet"));
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, ClearCustomUrlPacket> CODEC = net.minecraft.network.codec.StreamCodec.of((buffer, packet) -> ClearCustomUrlPacket.encode(packet, buffer), ClearCustomUrlPacket::decode);
        @Override public Type<ClearCustomUrlPacket> type() { return TYPE; }
        public static void encode(ClearCustomUrlPacket packet, net.minecraft.network.FriendlyByteBuf buffer) {
            buffer.writeBlockPos(packet.pos);
        }

        public static ClearCustomUrlPacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
            return new ClearCustomUrlPacket(buffer.readBlockPos());
        }

        public static void handle(ClearCustomUrlPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {

            context.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) context.player();

                if (!isValidRadioAccess(player, packet.pos)) {
                    return;
                }

                BlockEntity blockEntity = player.level().getBlockEntity(packet.pos);

                if (!(blockEntity instanceof CointRadioBlockEntity radio)) {
                    return;
                }

                radio.clearCustomUrl();

                player.displayClientMessage(
                        Component.literal("§e[CointMusic] URL радио очищен. Используется станция: §f" + CointRadioConfig.getStationName(radio.getStationId())),
                        true
                );
            });
        }
    }

    public record NextStationPacket(BlockPos pos) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        public static final Type<NextStationPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "radio_next_station_packet"));
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, NextStationPacket> CODEC = net.minecraft.network.codec.StreamCodec.of((buffer, packet) -> NextStationPacket.encode(packet, buffer), NextStationPacket::decode);
        @Override public Type<NextStationPacket> type() { return TYPE; }
        public static void encode(NextStationPacket packet, net.minecraft.network.FriendlyByteBuf buffer) {
            buffer.writeBlockPos(packet.pos);
        }

        public static NextStationPacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
            return new NextStationPacket(buffer.readBlockPos());
        }

        public static void handle(NextStationPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {

            context.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) context.player();

                if (!isValidRadioAccess(player, packet.pos)) {
                    return;
                }

                BlockEntity blockEntity = player.level().getBlockEntity(packet.pos);

                if (!(blockEntity instanceof CointRadioBlockEntity radio)) {
                    return;
                }

                String nextStation = radio.nextStation();

                player.displayClientMessage(
                        Component.literal("§e[CointMusic] Следующая станция: §f" + CointRadioConfig.getStationName(nextStation)),
                        true
                );
            });
        }
    }

    public record RandomStationPacket(BlockPos pos) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        public static final Type<RandomStationPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "radio_random_station_packet"));
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, RandomStationPacket> CODEC = net.minecraft.network.codec.StreamCodec.of((buffer, packet) -> RandomStationPacket.encode(packet, buffer), RandomStationPacket::decode);
        @Override public Type<RandomStationPacket> type() { return TYPE; }
        public static void encode(RandomStationPacket packet, net.minecraft.network.FriendlyByteBuf buffer) {
            buffer.writeBlockPos(packet.pos);
        }

        public static RandomStationPacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
            return new RandomStationPacket(buffer.readBlockPos());
        }

        public static void handle(RandomStationPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {

            context.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) context.player();

                if (!isValidRadioAccess(player, packet.pos)) {
                    return;
                }

                BlockEntity blockEntity = player.level().getBlockEntity(packet.pos);

                if (!(blockEntity instanceof CointRadioBlockEntity radio)) {
                    return;
                }

                String randomStation = radio.randomStation();

                player.displayClientMessage(
                        Component.literal("§d[CointMusic] Случайная станция: §f" + CointRadioConfig.getStationName(randomStation)),
                        true
                );
            });
        }
    }

    public record SetRadiusPacket(BlockPos pos, int radius) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        public static final Type<SetRadiusPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "radio_set_radius_packet"));
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, SetRadiusPacket> CODEC = net.minecraft.network.codec.StreamCodec.of((buffer, packet) -> SetRadiusPacket.encode(packet, buffer), SetRadiusPacket::decode);
        @Override public Type<SetRadiusPacket> type() { return TYPE; }
        public static void encode(SetRadiusPacket packet, net.minecraft.network.FriendlyByteBuf buffer) {
            buffer.writeBlockPos(packet.pos);
            buffer.writeVarInt(packet.radius);
        }

        public static SetRadiusPacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
            return new SetRadiusPacket(buffer.readBlockPos(), buffer.readVarInt());
        }

        public static void handle(SetRadiusPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {

            context.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) context.player();

                if (!isValidRadioAccess(player, packet.pos)) {
                    return;
                }

                BlockEntity blockEntity = player.level().getBlockEntity(packet.pos);

                if (!(blockEntity instanceof CointRadioBlockEntity radio)) {
                    return;
                }

                radio.setRadius(packet.radius);

                player.displayClientMessage(
                        Component.literal("§e[CointMusic] Радиус радио: §f" + radio.getRadius()),
                        true
                );
            });
        }
    }
}