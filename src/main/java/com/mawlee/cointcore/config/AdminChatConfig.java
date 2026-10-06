package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * File: {@code config/cointcore/chat.json} section {@code admin_chat}.
 */
public final class AdminChatConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean enabled = true;

    private AdminChatConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static Path getConfigPath() {
        return ChatConfigs.path();
    }

    public static void load() {
        ChatConfigs.load();
    }

    public static boolean reload() {
        return ChatConfigs.reload();
    }

    static void applySection(FileData data) {
        apply(parse(data != null ? data : defaultFileData()));
    }

    static void logReload() {
        LOGGER.info("Reloaded admin chat config (enabled={})", enabled);
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
    }

    private static LoadedConfig parse(FileData data) {
        return new LoadedConfig(data.enabled);
    }

    static FileData defaultFileData() {
        FileData data = new FileData();
        data.enabled = true;
        return data;
    }

    private record LoadedConfig(boolean enabled) {
    }

    static final class FileData {
        @SerializedName("enabled")
        private boolean enabled = true;
    }
}
