package com.mawlee.cointcore.teleport;

import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.mute.MuteService;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

public final class TplService {
    private static final SimpleCommandExceptionType INVALID_POSITION = new SimpleCommandExceptionType(
            Component.translatable("commands.teleport.invalidPosition")
    );

    private TplService() {
    }

    public static boolean teleportToPlayer(CommandSourceStack source, UUID targetId, String targetName) throws CommandSyntaxException {
        if (!(source.getEntity() instanceof ServerPlayer executor)) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.TPL_EXECUTOR_MUST_BE_PLAYER));
            return false;
        }

        if (executor.getUUID().equals(targetId)) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.TPL_CANNOT_SELF));
            return false;
        }

        MinecraftServer server = source.getServer();
        ServerPlayer onlineTarget = server.getPlayerList().getPlayer(targetId);
        if (onlineTarget != null) {
            teleportExecutor(executor, (ServerLevel) onlineTarget.level(), onlineTarget.getX(), onlineTarget.getY(), onlineTarget.getZ(), onlineTarget.getYRot(), onlineTarget.getXRot());
            source.sendSuccess(
                    () -> CointCoreMessages.forSource(source, CointCoreMessages.TPL_SUCCESS_ONLINE, targetName),
                    false
            );
            return true;
        }

        Optional<OfflinePlayerPosition> offlinePosition = loadOfflinePosition(server, targetId);
        if (offlinePosition.isEmpty()) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.TPL_NO_PLAYER_DATA, targetName));
            return false;
        }

        OfflinePlayerPosition position = offlinePosition.get();
        ServerLevel targetLevel = server.getLevel(position.dimension());
        if (targetLevel == null) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.TPL_DIMENSION_UNAVAILABLE, targetName));
            return false;
        }

        teleportExecutor(executor, targetLevel, position.x(), position.y(), position.z(), position.yaw(), position.pitch());
        source.sendSuccess(
                () -> CointCoreMessages.forSource(source, CointCoreMessages.TPL_SUCCESS_OFFLINE, targetName),
                false
        );
        return true;
    }

    private static void teleportExecutor(
            ServerPlayer executor,
            ServerLevel level,
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) throws CommandSyntaxException {
        if (!Level.isInSpawnableBounds(net.minecraft.core.BlockPos.containing(x, y, z))) {
            throw INVALID_POSITION.create();
        }

        if (!executor.teleportTo(level, x, y, z, EnumSet.noneOf(RelativeMovement.class), yaw, pitch)) {
            throw INVALID_POSITION.create();
        }

        executor.setDeltaMovement(executor.getDeltaMovement().multiply(1.0, 0.0, 1.0));
        executor.setOnGround(true);
    }

    public static Optional<OfflinePlayerPosition> loadOfflinePosition(MinecraftServer server, UUID playerId) {
        Path playerFile = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(playerId + ".dat");
        if (!playerFile.toFile().isFile()) {
            return Optional.empty();
        }

        try {
            CompoundTag tag = NbtIo.readCompressed(playerFile, NbtAccounter.unlimitedHeap());
            return parseOfflinePosition(tag);
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    static Optional<OfflinePlayerPosition> parseOfflinePosition(CompoundTag tag) {
        if (!tag.contains("Pos", Tag.TAG_LIST) || !tag.contains("Dimension", Tag.TAG_STRING)) {
            return Optional.empty();
        }

        ListTag pos = tag.getList("Pos", Tag.TAG_DOUBLE);
        if (pos.size() < 3) {
            return Optional.empty();
        }

        float yaw = 0.0F;
        float pitch = 0.0F;
        if (tag.contains("Rotation", Tag.TAG_LIST)) {
            ListTag rotation = tag.getList("Rotation", Tag.TAG_FLOAT);
            if (rotation.size() >= 2) {
                yaw = rotation.getFloat(0);
                pitch = rotation.getFloat(1);
            }
        }

        ResourceLocation dimensionId = ResourceLocation.parse(tag.getString("Dimension"));
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
        return Optional.of(new OfflinePlayerPosition(
                dimension,
                pos.getDouble(0),
                pos.getDouble(1),
                pos.getDouble(2),
                yaw,
                pitch
        ));
    }

    public static Optional<UUID> resolveTargetId(MinecraftServer server, String name) {
        return MuteService.resolvePlayerId(server, name);
    }

    public static Optional<String> resolveTargetName(MinecraftServer server, UUID playerId) {
        return MuteService.resolveName(server, playerId);
    }
}
