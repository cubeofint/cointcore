package com.mawlee.cointcore.config;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Owns {@code config/cointcore/claims.json} with sections {@code buffer}, {@code bonus}
 * and {@code bossClaimGuard}.
 */
public final class ClaimsConfigs {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_NAME = "claims.json";

    private ClaimsConfigs() {
    }

    public static Path path() {
        return ConfigMergeSupport.path(FILE_NAME);
    }

    public static void load() {
        try {
            applyRoot(readRoot(), false);
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load {}; using defaults", FILE_NAME, exception);
            ClaimBufferConfig.applySection(ClaimBufferConfig.defaultFileData());
            ChunkBonusConfig.applySection(ChunkBonusConfig.defaultFileData());
            BossClaimGuardConfig.applySection(BossClaimGuardConfig.defaultFileData());
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
                        "buffer", "claim-buffer.json",
                        "bonus", "chunk-bonus.json"
                ),
                ClaimsConfigs::defaults,
                false,
                LOGGER
        );
    }

    private static JsonObject defaults() {
        JsonObject root = new JsonObject();
        root.add("buffer", ConfigMergeSupport.toJsonObject(ClaimBufferConfig.defaultFileData()));
        root.add("bonus", ConfigMergeSupport.toJsonObject(ChunkBonusConfig.defaultFileData()));
        root.add("bossClaimGuard", ConfigMergeSupport.toJsonObject(BossClaimGuardConfig.defaultFileData()));
        return root;
    }

    private static void applyRoot(JsonObject root, boolean logReload) {
        ClaimBufferConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "buffer", ClaimBufferConfig.FileData.class, ClaimBufferConfig::defaultFileData
        ));
        ChunkBonusConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "bonus", ChunkBonusConfig.FileData.class, ChunkBonusConfig::defaultFileData
        ));
        BossClaimGuardConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root,
                "bossClaimGuard",
                BossClaimGuardConfig.FileData.class,
                BossClaimGuardConfig::defaultFileData
        ));
        if (logReload) {
            ClaimBufferConfig.logReload();
            ChunkBonusConfig.logReload();
            BossClaimGuardConfig.logReload();
        }
    }
}
