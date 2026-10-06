package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.mawlee.cointcore.CointCore;
import com.mojang.logging.LogUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Caps / toggles for Industrial Foregoing Souls Soul Surge acceleration.
 * Denylist lives in {@link SoulSurgeDenyConfig} ({@code soul-surge-deny.json}).
 */
public final class SoulSurgePerfConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final int MIN_ACCELERATION_TICKS = 0;
    private static final int MAX_ACCELERATION_TICKS = 16;

    private static boolean enabled = true;
    private static int maxAccelerationTicks = 1;
    private static boolean respectChunkLimits = true;
    private static boolean allowEntityAcceleration = false;
    private static boolean allowBlockRandomTickAcceleration = false;

    private SoulSurgePerfConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static int getMaxAccelerationTicks() {
        return maxAccelerationTicks;
    }

    public static boolean respectChunkLimits() {
        return respectChunkLimits;
    }

    public static boolean allowEntityAcceleration() {
        return allowEntityAcceleration;
    }

    public static boolean allowBlockRandomTickAcceleration() {
        return allowBlockRandomTickAcceleration;
    }

    public static boolean isDenied(BlockState state) {
        if (!enabled) {
            return false;
        }
        return SoulSurgeDenyConfig.isDenied(state);
    }

    public static Path getConfigPath() {
        return configPath();
    }

    public static void load() {
        apply(loadFromDisk(false));
    }

    public static boolean reload() {
        LoadedConfig loaded = loadFromDisk(true);
        if (loaded == null) {
            return false;
        }
        apply(loaded);
        LOGGER.info(
                "Reloaded soul surge perf config (enabled={} maxTicks={} respectChunkLimits={} "
                        + "entityAccel={} randomTickAccel={})",
                enabled,
                maxAccelerationTicks,
                respectChunkLimits,
                allowEntityAcceleration,
                allowBlockRandomTickAcceleration
        );
        return true;
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
        maxAccelerationTicks = loaded.maxAccelerationTicks();
        respectChunkLimits = loaded.respectChunkLimits();
        allowEntityAcceleration = loaded.allowEntityAcceleration();
        allowBlockRandomTickAcceleration = loaded.allowBlockRandomTickAcceleration();
    }

    private static LoadedConfig loadFromDisk(boolean reloading) {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                FileData defaults = defaultFileData();
                save(defaults, path);
                LOGGER.info("Created default soul surge perf config at {}", path);
                return parse(defaults);
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                return parse(data != null ? data : defaultFileData());
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load soul surge perf config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading soul surge perf config from {}", configPath(), exception);
            return reloading ? null : parse(defaultFileData());
        }
    }

    private static LoadedConfig parse(FileData data) {
        boolean on = data.enabled == null || data.enabled;
        int maxTicks = clamp(
                data.maxAccelerationTicks != null ? data.maxAccelerationTicks : 1,
                MIN_ACCELERATION_TICKS,
                MAX_ACCELERATION_TICKS
        );
        boolean respectLimits = data.respectChunkLimits == null || data.respectChunkLimits;
        boolean entityAccel = Boolean.TRUE.equals(data.allowEntityAcceleration);
        boolean randomTickAccel = Boolean.TRUE.equals(data.allowBlockRandomTickAcceleration);

        return new LoadedConfig(on, maxTicks, respectLimits, entityAccel, randomTickAccel);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static FileData defaultFileData() {
        FileData data = new FileData();
        data.enabled = true;
        data.maxAccelerationTicks = 1;
        data.respectChunkLimits = true;
        data.allowEntityAcceleration = false;
        data.allowBlockRandomTickAcceleration = false;
        return data;
    }

    private static void save(FileData data, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve("soul-surge-perf.json");
    }

    private record LoadedConfig(
            boolean enabled,
            int maxAccelerationTicks,
            boolean respectChunkLimits,
            boolean allowEntityAcceleration,
            boolean allowBlockRandomTickAcceleration
    ) {
    }

    private static final class FileData {
        @SerializedName("enabled")
        private Boolean enabled;

        @SerializedName("maxAccelerationTicks")
        private Integer maxAccelerationTicks;

        @SerializedName("respectChunkLimits")
        private Boolean respectChunkLimits;

        @SerializedName("allowEntityAcceleration")
        private Boolean allowEntityAcceleration;

        @SerializedName("allowBlockRandomTickAcceleration")
        private Boolean allowBlockRandomTickAcceleration;
    }
}
