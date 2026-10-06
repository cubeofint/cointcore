package com.mawlee.cointcore.config;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Owns {@code config/cointcore/chat.json} with sections {@code join_messages} and {@code admin_chat}.
 */
public final class ChatConfigs {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_NAME = "chat.json";

    private ChatConfigs() {
    }

    public static Path path() {
        return ConfigMergeSupport.path(FILE_NAME);
    }

    public static void load() {
        try {
            applyRoot(readRoot(), false);
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load {}; using defaults", FILE_NAME, exception);
            JoinMessagesConfig.applySection(JoinMessagesConfig.defaultFileData());
            AdminChatConfig.applySection(AdminChatConfig.defaultFileData());
        }
    }

    public static boolean reload() {
        try {
            applyRoot(readRoot(), true);
            return true;
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to reload {}; keeping previous values", FILE_NAME, exception);
            return false;
        }
    }

    private static JsonObject readRoot() throws IOException {
        return ConfigMergeSupport.loadMergedObject(
                FILE_NAME,
                ConfigMergeSupport.legacyMap(
                        "join_messages", "join-messages.json",
                        "admin_chat", "admin-chat.json"
                ),
                ChatConfigs::defaults,
                false,
                LOGGER
        );
    }

    private static JsonObject defaults() {
        JsonObject root = new JsonObject();
        root.add("join_messages", ConfigMergeSupport.toJsonObject(JoinMessagesConfig.defaultFileData()));
        root.add("admin_chat", ConfigMergeSupport.toJsonObject(AdminChatConfig.defaultFileData()));
        return root;
    }

    private static void applyRoot(JsonObject root, boolean logReload) {
        JoinMessagesConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "join_messages", JoinMessagesConfig.FileData.class, JoinMessagesConfig::defaultFileData
        ));
        AdminChatConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "admin_chat", AdminChatConfig.FileData.class, AdminChatConfig::defaultFileData
        ));
        if (logReload) {
            JoinMessagesConfig.logReload();
            AdminChatConfig.logReload();
        }
    }
}
