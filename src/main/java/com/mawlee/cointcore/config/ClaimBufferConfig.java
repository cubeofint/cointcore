package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Minimum free-chunk gap (Chebyshev) between claims of different FTB teams.
 *
 * <p>File: {@code config/cointcore/claims.json} section {@code buffer}.
 */
public final class ClaimBufferConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static Settings settings = Settings.defaults();

    private ClaimBufferConfig() {
    }

    public static Settings get() {
        return settings;
    }

    public static Path getConfigPath() {
        return ClaimsConfigs.path();
    }

    public static void load() {
        ClaimsConfigs.load();
    }

    public static boolean reload() {
        return ClaimsConfigs.reload();
    }

    static void applySection(FileData data) {
        apply(parse(data != null ? data : defaultFileData()));
    }

    static void logReload() {
        LOGGER.info(
                "Reloaded claim buffer config (enabled={}, freeChunks={})",
                settings.enabled(),
                settings.freeChunks()
        );
    }

    private static void apply(LoadedConfig loaded) {
        settings = loaded.settings;
    }

    private static LoadedConfig parse(FileData data) {
        boolean enabled = data.enabled == null || data.enabled;
        int free = data.freeChunks != null ? Math.max(0, data.freeChunks) : 3;
        return new LoadedConfig(new Settings(enabled, free));
    }

    static FileData defaultFileData() {
        FileData data = new FileData();
        data.enabled = true;
        data.freeChunks = 3;
        return data;
    }

    /**
     * @param freeChunks number of unclaimed chunks required between different teams
     *                   (Chebyshev: reject if foreign claim within {@code freeChunks})
     */
    public record Settings(boolean enabled, int freeChunks) {
        public static Settings defaults() {
            return new Settings(true, 3);
        }
    }

    private record LoadedConfig(Settings settings) {
    }

    static final class FileData {
        @SerializedName("enabled")
        private Boolean enabled;

        /** Free chunks required between different teams (Chebyshev radius). */
        @SerializedName("freeChunks")
        private Integer freeChunks;
    }
}
