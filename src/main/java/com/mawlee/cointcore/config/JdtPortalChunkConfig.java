package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Just Dire Things Portal Gun / Portal Gun V2: stop permanent force-loading of portal chunks.
 *
 * <p>File: {@code config/cointcore/lag-fixes.json} section {@code jdt_portals}.
 */
public final class JdtPortalChunkConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    public enum Mode {
        /** Never force-load portal chunks (teleport still works when chunks are already loaded). */
        NONE,
        /** Force-load only while the portal owner is online. */
        OWNER_ONLINE
    }

    private static boolean enabled = false;
    private static Mode mode = Mode.NONE;
    private static boolean clearStaleTicketsOnStart = true;

    private JdtPortalChunkConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static Mode getMode() {
        return mode;
    }

    public static boolean isClearStaleTicketsOnStart() {
        return clearStaleTicketsOnStart;
    }

    public static Path getConfigPath() {
        return LagFixesConfigs.path();
    }

    public static void load() {
        LagFixesConfigs.load();
    }

    public static boolean reload() {
        return LagFixesConfigs.reload();
    }

    static void applySection(Data data) {
        apply(parse(data != null ? data : defaultFileData()));
    }

    static void logReload() {
        LOGGER.info(
                "Reloaded JDT portal chunk config (enabled={}, mode={}, clearStaleOnStart={})",
                enabled,
                mode,
                clearStaleTicketsOnStart
        );
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
        mode = loaded.mode();
        clearStaleTicketsOnStart = loaded.clearStaleTicketsOnStart();
    }

    private static LoadedConfig parse(Data data) {
        Mode parsedMode = Mode.NONE;
        if (data.mode != null && !data.mode.isBlank()) {
            try {
                parsedMode = Mode.valueOf(data.mode.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                LOGGER.warn("Unknown jdt_portals.mode '{}', using NONE", data.mode);
            }
        }
        return new LoadedConfig(
                data.enabled,
                parsedMode,
                data.clearStaleTicketsOnStart
        );
    }

    static Data defaultFileData() {
        return new Data();
    }

    private record LoadedConfig(boolean enabled, Mode mode, boolean clearStaleTicketsOnStart) {
    }

    static final class Data {
        @SerializedName("enabled")
        boolean enabled = false;

        /** {@code none} or {@code owner_online}. */
        @SerializedName("mode")
        String mode = "none";

        @SerializedName("clear_stale_tickets_on_start")
        boolean clearStaleTicketsOnStart = true;
    }
}
