package com.mawlee.cointcore.config;

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

/**
 * Soul Surge intervention flags ({@code config/cointcore/soul-surge-perf.json}).
 * When {@code enabled}, each surge accelerates only its attached block entity.
 * Extra ticker calls are admitted while that target is still under 3 extra
 * ticks this game tick (4x including the normal tick). IF Souls still loops
 * {@code ACCELERATION_TICK}; surplus calls are dropped. Acceleration is not
 * configurable here.
 * <p>
 * Unused / obsolete keys are not bound: {@code maxAccelerationTicks} never
 * capped the IF Souls tick count and is ignored if present, as are
 * {@code denyBlockIds}, {@code denyBlockTags} and {@code respectChunkLimits}.
 * Old files keep loading.
 */
public final class SoulSurgePerfConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Settings settings = Settings.defaults();

    private SoulSurgePerfConfig() {
    }

    public static boolean isEnabled() {
        return settings.enabled();
    }

    public static boolean isSingleTarget() {
        return settings.singleTarget();
    }

    public static Settings get() {
        return settings;
    }

    public static Path getConfigPath() {
        return configPath();
    }

    public static void load() {
        apply(loadFromDisk(false));
    }

    public static boolean reload() {
        Settings loaded = loadFromDisk(true);
        if (loaded == null) {
            return false;
        }
        apply(loaded);
        LOGGER.info(
                "Reloaded soul surge perf config (enabled={} singleTarget={})",
                settings.enabled(),
                settings.singleTarget()
        );
        return true;
    }

    private static void apply(Settings loaded) {
        settings = loaded != null ? loaded : Settings.defaults();
    }

    private static Settings loadFromDisk(boolean reloading) {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                FileData defaults = FileData.defaults();
                save(defaults, path);
                LOGGER.info("Created default soul surge perf config at {}", path);
                return Settings.from(defaults);
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                return Settings.from(data);
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load soul surge perf config from {}", configPath(), exception);
            return reloading ? null : Settings.defaults();
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading soul surge perf config from {}", configPath(), exception);
            return reloading ? null : Settings.defaults();
        }
    }

    private static void save(FileData data, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve("soul-surge-perf.json");
    }

    public record Settings(boolean enabled, boolean singleTarget) {
        public static Settings defaults() {
            return new Settings(true, true);
        }

        public static Settings from(FileData data) {
            if (data == null) {
                return defaults();
            }
            boolean on = data.enabled == null || data.enabled;
            boolean single = data.singleTarget == null || data.singleTarget;
            return new Settings(on, single);
        }
    }

    /**
     * On-disk shape. {@code maxAccelerationTicks} is intentionally absent so
     * new files do not write it; Gson drops the leftover key on load.
     */
    static final class FileData {
        @SerializedName("enabled")
        Boolean enabled;

        @SerializedName("singleTarget")
        Boolean singleTarget;

        static FileData defaults() {
            FileData data = new FileData();
            data.enabled = true;
            data.singleTarget = true;
            return data;
        }
    }
}
