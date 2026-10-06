package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * File: {@code config/cointcore/chat.json} section {@code join_messages}.
 */
public final class JoinMessagesConfig {
    /** Starter template size when creating a missing config file; not a max limit. */
    private static final int DEFAULT_TEMPLATE_LINES = 6;

    private static final Logger LOGGER = LogUtils.getLogger();

    private static List<String> lines = defaultLines();

    private JoinMessagesConfig() {
    }

    public static List<String> getLines() {
        return lines;
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
        lines = normalizeLines(data != null ? data.lines : null);
    }

    static void logReload() {
        LOGGER.info("Reloaded join messages config ({} lines)", lines.size());
    }

    static FileData defaultFileData() {
        FileData data = new FileData();
        data.lines = new ArrayList<>(defaultLines());
        return data;
    }

    private static List<String> defaultLines() {
        return new ArrayList<>(Collections.nCopies(DEFAULT_TEMPLATE_LINES, ""));
    }

    private static List<String> normalizeLines(List<String> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }

        List<String> normalized = new ArrayList<>(source.size());
        for (String line : source) {
            normalized.add(line != null ? line : "");
        }
        return List.copyOf(normalized);
    }

    static final class FileData {
        @SerializedName("lines")
        private List<String> lines = new ArrayList<>();
    }
}
