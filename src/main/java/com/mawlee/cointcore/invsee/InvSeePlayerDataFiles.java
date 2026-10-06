package com.mawlee.cointcore.invsee;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

final class InvSeePlayerDataFiles {
    private static final Logger LOGGER = LogUtils.getLogger();

    private InvSeePlayerDataFiles() {
    }

    static Optional<CompoundTag> load(MinecraftServer server, UUID playerId) {
        Path playerFile = playerDataFile(server, playerId);
        if (!Files.isRegularFile(playerFile)) {
            return Optional.empty();
        }

        try {
            return Optional.of(NbtIo.readCompressed(playerFile, NbtAccounter.unlimitedHeap()));
        } catch (IOException exception) {
            LOGGER.error("Failed to read playerdata for {} from {}", playerId, playerFile, exception);
            return Optional.empty();
        }
    }

    static boolean save(MinecraftServer server, UUID playerId, CompoundTag data) {
        Path playerFile = playerDataFile(server, playerId);
        try {
            AtomicFileWriter.write(playerFile, compressed(data));
            return true;
        } catch (IOException exception) {
            LOGGER.error("Failed to write playerdata for {} to {}", playerId, playerFile, exception);
            return false;
        }
    }

    static long lastModified(MinecraftServer server, UUID playerId) {
        Path playerFile = playerDataFile(server, playerId);
        try {
            return Files.exists(playerFile) ? Files.getLastModifiedTime(playerFile).toMillis() : 0L;
        } catch (IOException exception) {
            LOGGER.warn("Failed to read mtime of {}", playerFile, exception);
            return 0L;
        }
    }

    private static byte[] compressed(CompoundTag data) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        NbtIo.writeCompressed(data, bytes);
        return bytes.toByteArray();
    }

    private static Path playerDataFile(MinecraftServer server, UUID playerId) {
        return server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(playerId + ".dat");
    }
}
