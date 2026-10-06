package com.mawlee.cointcore.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.mawlee.cointcore.CointCore;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Survives restart so wipe runs on ServerStopped when no players are online.
 */
public final class DimensionWipePending {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private DimensionWipePending() {
    }

    public static Path path() {
        return FMLPaths.CONFIGDIR.get()
                .resolve(CointCore.MOD_ID)
                .resolve("dimension-wipe-pending.json");
    }

    public static boolean exists() {
        return Files.isRegularFile(path());
    }

    public static Optional<PendingWipe> read() {
        Path file = path();
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            PendingFile data = GSON.fromJson(reader, PendingFile.class);
            if (data == null || data.dimensions == null || data.dimensions.isEmpty()) {
                return Optional.empty();
            }
            List<String> dims = new ArrayList<>();
            for (String dim : data.dimensions) {
                if (dim != null && !dim.isBlank()) {
                    dims.add(dim.trim());
                }
            }
            if (dims.isEmpty()) {
                return Optional.empty();
            }
            String slotId = data.slotId == null ? "" : data.slotId;
            return Optional.of(new PendingWipe(
                    slotId,
                    List.copyOf(dims),
                    data.allowOverworldWipe,
                    data.spawnX,
                    data.spawnY,
                    data.spawnZ
            ));
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to read dimension wipe pending file {}", file, exception);
            return Optional.empty();
        }
    }

    public static boolean write(PendingWipe pending) {
        Path file = path();
        try {
            Files.createDirectories(file.getParent());
            PendingFile data = new PendingFile();
            data.slotId = pending.slotId();
            data.dimensions = new ArrayList<>(pending.dimensions());
            data.allowOverworldWipe = pending.allowOverworldWipe();
            data.spawnX = pending.spawnX();
            data.spawnY = pending.spawnY();
            data.spawnZ = pending.spawnZ();
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
            LOGGER.info("Wrote dimension wipe pending marker for slot {} ({} dims)", pending.slotId(), pending.dimensions().size());
            return true;
        } catch (IOException exception) {
            LOGGER.error("Failed to write dimension wipe pending file {}", file, exception);
            return false;
        }
    }

    public static void clear() {
        Path file = path();
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            LOGGER.warn("Failed to delete dimension wipe pending file {}", file, exception);
        }
    }

    public record PendingWipe(
            String slotId,
            List<String> dimensions,
            boolean allowOverworldWipe,
            double spawnX,
            double spawnY,
            double spawnZ
    ) {
    }

    private static final class PendingFile {
        @SerializedName("slotId")
        private String slotId;

        @SerializedName("dimensions")
        private List<String> dimensions;

        @SerializedName("allowOverworldWipe")
        private boolean allowOverworldWipe;

        @SerializedName("spawnX")
        private double spawnX;

        @SerializedName("spawnY")
        private double spawnY;

        @SerializedName("spawnZ")
        private double spawnZ;
    }
}
