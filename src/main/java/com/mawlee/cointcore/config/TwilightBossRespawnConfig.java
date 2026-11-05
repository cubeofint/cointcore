package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * File: {@code config/cointcore/cataclysm-respawn.json} section {@code twilight_bosses}.
 * Puts Twilight Forest boss spawners back on every server start for bosses that were killed.
 */
public final class TwilightBossRespawnConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean enabled = true;
    private static int delayTicks = 200;
    private static boolean replaceBlocks = false;

    private TwilightBossRespawnConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /** Ticks after server start before spawners are put back. */
    public static int getDelayTicks() {
        return delayTicks;
    }

    /** Whether a solid block standing where the spawner goes may be replaced by it. */
    public static boolean isReplaceBlocks() {
        return replaceBlocks;
    }

    static void applySection(FileData data) {
        FileData source = data != null ? data : defaultFileData();
        enabled = source.enabled != null ? source.enabled : true;
        delayTicks = source.delayTicks != null && source.delayTicks >= 0 ? source.delayTicks : 200;
        replaceBlocks = source.replaceBlocks != null && source.replaceBlocks;
    }

    static void logReload() {
        LOGGER.info("Reloaded Twilight Forest boss respawn config (enabled={})", enabled);
    }

    static FileData defaultFileData() {
        FileData data = new FileData();
        data.enabled = true;
        data.delayTicks = 200;
        data.replaceBlocks = false;
        return data;
    }

    static final class FileData {
        @SerializedName("enabled")
        Boolean enabled;

        @SerializedName("delayTicks")
        Integer delayTicks;

        @SerializedName("replaceBlocks")
        Boolean replaceBlocks;
    }
}
