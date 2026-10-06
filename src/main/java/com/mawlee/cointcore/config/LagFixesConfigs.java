package com.mawlee.cointcore.config;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Owns {@code config/cointcore/lag-fixes.json} — ATM10 8.2 lag mitigations.
 * Every section defaults to {@code enabled=false} so dropping the jar is a no-op
 * until an admin opts in.
 */
public final class LagFixesConfigs {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_NAME = "lag-fixes.json";

    private LagFixesConfigs() {
    }

    public static Path path() {
        return ConfigMergeSupport.path(FILE_NAME);
    }

    public static void load() {
        try {
            applyRoot(readRoot(), false);
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load {}; using defaults", FILE_NAME, exception);
            JdtPortalChunkConfig.applySection(JdtPortalChunkConfig.defaultFileData());
            EnderIoItemConduitConfig.applySection(EnderIoItemConduitConfig.defaultFileData());
            RelicsBackpackScanConfig.applySection(RelicsBackpackScanConfig.defaultFileData());
            ChunkLoaderRestrictConfig.applySection(ChunkLoaderRestrictConfig.defaultFileData());
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
                java.util.Map.of(),
                LagFixesConfigs::defaults,
                false,
                LOGGER
        );
    }

    private static JsonObject defaults() {
        JsonObject root = new JsonObject();
        root.add("jdt_portals", ConfigMergeSupport.toJsonObject(JdtPortalChunkConfig.defaultFileData()));
        root.add("enderio_item_conduits", ConfigMergeSupport.toJsonObject(EnderIoItemConduitConfig.defaultFileData()));
        root.add("relics_backpack_scan", ConfigMergeSupport.toJsonObject(RelicsBackpackScanConfig.defaultFileData()));
        root.add("chunk_loaders", ConfigMergeSupport.toJsonObject(ChunkLoaderRestrictConfig.defaultFileData()));
        return root;
    }

    private static void applyRoot(JsonObject root, boolean logReload) {
        JdtPortalChunkConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "jdt_portals", JdtPortalChunkConfig.Data.class, JdtPortalChunkConfig::defaultFileData
        ));
        EnderIoItemConduitConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "enderio_item_conduits", EnderIoItemConduitConfig.Data.class, EnderIoItemConduitConfig::defaultFileData
        ));
        RelicsBackpackScanConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "relics_backpack_scan", RelicsBackpackScanConfig.Data.class, RelicsBackpackScanConfig::defaultFileData
        ));
        ChunkLoaderRestrictConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "chunk_loaders", ChunkLoaderRestrictConfig.Data.class, ChunkLoaderRestrictConfig::defaultFileData
        ));
        if (logReload) {
            JdtPortalChunkConfig.logReload();
            EnderIoItemConduitConfig.logReload();
            RelicsBackpackScanConfig.logReload();
            ChunkLoaderRestrictConfig.logReload();
        }
    }
}
