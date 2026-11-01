package com.mawlee.cointcore.config;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Owns the optional storage and transfer optimizations in {@code config/cointcore/storage-perf.json}.
 */
public final class StoragePerfConfigs {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_NAME = "storage-perf.json";

    private StoragePerfConfigs() {
    }

    public static Path path() {
        return ConfigMergeSupport.path(FILE_NAME);
    }

    public static void load() {
        try {
            applyRoot(readRoot(), false);
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load {}; using defaults", FILE_NAME, exception);
            Ae2PerfConfig.applySection(Ae2PerfConfig.defaultFileData());
            MekanismPerfConfig.applySection(MekanismPerfConfig.defaultFileData());
            IntegratedTransferPerfConfig.applySection(IntegratedTransferPerfConfig.defaultFileData());
            RsImporterPerfConfig.applySection(RsImporterPerfConfig.defaultFileData());
            ModularBeesRsConfig.applySection(ModularBeesRsConfig.defaultFileData());
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
        JsonObject root = ConfigMergeSupport.loadMergedObject(
                FILE_NAME,
                ConfigMergeSupport.legacyMap(
                        "ae2", "ae2_perf.json",
                        "rs_importer", "rs_importer_perf.json"
                ),
                StoragePerfConfigs::defaults,
                false,
                LOGGER
        );
        boolean updated = ConfigMergeSupport.addMissingSectionValues(root, "ae2", Ae2PerfConfig.defaultFileData());
        updated |= ConfigMergeSupport.addMissingSectionValues(root, "mekanism", MekanismPerfConfig.defaultFileData());
        updated |= ConfigMergeSupport.addMissingSectionValues(root, "integrated_tunnels", IntegratedTransferPerfConfig.defaultFileData());
        if (updated) ConfigMergeSupport.writeObject(path(), root);
        return root;
    }

    private static JsonObject defaults() {
        JsonObject root = new JsonObject();
        root.add("ae2", ConfigMergeSupport.toJsonObject(Ae2PerfConfig.defaultFileData()));
        root.add("mekanism", ConfigMergeSupport.toJsonObject(MekanismPerfConfig.defaultFileData()));
        root.add("integrated_tunnels", ConfigMergeSupport.toJsonObject(IntegratedTransferPerfConfig.defaultFileData()));
        root.add("rs_importer", ConfigMergeSupport.toJsonObject(RsImporterPerfConfig.defaultFileData()));
        root.add("modularbees_rs", ConfigMergeSupport.toJsonObject(ModularBeesRsConfig.defaultFileData()));
        return root;
    }

    private static void applyRoot(JsonObject root, boolean logReload) {
        Ae2PerfConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "ae2", Ae2PerfConfig.Data.class, Ae2PerfConfig::defaultFileData
        ));
        MekanismPerfConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "mekanism", MekanismPerfConfig.Data.class, MekanismPerfConfig::defaultFileData
        ));
        IntegratedTransferPerfConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "integrated_tunnels", IntegratedTransferPerfConfig.Data.class, IntegratedTransferPerfConfig::defaultFileData
        ));
        RsImporterPerfConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "rs_importer", RsImporterPerfConfig.Data.class, RsImporterPerfConfig::defaultFileData
        ));
        ModularBeesRsConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "modularbees_rs", ModularBeesRsConfig.Data.class, ModularBeesRsConfig::defaultFileData
        ));
        if (logReload) {
            Ae2PerfConfig.logReload();
            LOGGER.info("Reloaded Mekanism perf config (directItemContainerLookup={})",
                    MekanismPerfConfig.isDirectItemContainerLookup());
            LOGGER.info("Reloaded Integrated Tunnels perf config (emptySourceFastPath={})",
                    IntegratedTransferPerfConfig.isEmptySourceFastPath());
            RsImporterPerfConfig.logReload();
        }
    }
}
