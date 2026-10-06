package com.mawlee.cointcore.invsee;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

final class InvSeePlayerDataFiles {
    private InvSeePlayerDataFiles() {
    }

    static Optional<CompoundTag> load(MinecraftServer server, UUID playerId) {
        Path playerFile = playerDataFile(server, playerId);
        if (!playerFile.toFile().isFile()) {
            return Optional.empty();
        }

        try {
            return Optional.of(NbtIo.readCompressed(playerFile, NbtAccounter.unlimitedHeap()));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    static void save(MinecraftServer server, UUID playerId, CompoundTag data) {
        Path playerFile = playerDataFile(server, playerId);
        try {
            NbtIo.writeCompressed(data, playerFile);
        } catch (IOException ignored) {
        }
    }

    private static Path playerDataFile(MinecraftServer server, UUID playerId) {
        return server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(playerId + ".dat");
    }
}
