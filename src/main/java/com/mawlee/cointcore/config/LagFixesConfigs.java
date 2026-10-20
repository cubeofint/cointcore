package com.mawlee.cointcore.config;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Owns {@code config/cointcore/lag-fixes.json} — ATM10 8.2 lag mitigations
 * plus dedicated-server crash compat. Most lag sections default {@code enabled=false};
 * crash-fix, {@code mekmm_wind_generator_scan},
 * {@code neovitae_missing_mappings_fastpath}, {@code pipez_armory_fast_reject}, and
 * {@code irons_spectral_hammer} default {@code enabled=true}.
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
            FunctionalStorageArmorySearchConfig.applySection(FunctionalStorageArmorySearchConfig.defaultFileData());
            RelicsServerTooltipConfig.applySection(RelicsServerTooltipConfig.defaultFileData());
            MekmmWindGeneratorScanConfig.applySection(MekmmWindGeneratorScanConfig.defaultFileData());
            VanishPacketFilterConfig.applySection(VanishPacketFilterConfig.defaultFileData());
            NeovitaeMissingMappingsConfig.applySection(NeovitaeMissingMappingsConfig.defaultFileData());
            CrowdedMobsConfig.applySection(CrowdedMobsConfig.defaultFileData());
            PipezArmoryFastRejectConfig.applySection(PipezArmoryFastRejectConfig.defaultFileData());
            IronsSpectralHammerConfig.applySection(IronsSpectralHammerConfig.defaultFileData());
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
        root.add("functionalstorage_armory_search", ConfigMergeSupport.toJsonObject(FunctionalStorageArmorySearchConfig.defaultFileData()));
        root.add("relics_server_tooltip", ConfigMergeSupport.toJsonObject(RelicsServerTooltipConfig.defaultFileData()));
        root.add("mekmm_wind_generator_scan", ConfigMergeSupport.toJsonObject(MekmmWindGeneratorScanConfig.defaultFileData()));
        root.add("vanish_packet_filter", ConfigMergeSupport.toJsonObject(VanishPacketFilterConfig.defaultFileData()));
        root.add(
                "neovitae_missing_mappings_fastpath",
                ConfigMergeSupport.toJsonObject(NeovitaeMissingMappingsConfig.defaultFileData())
        );
        root.add("crowded_mobs", ConfigMergeSupport.toJsonObject(CrowdedMobsConfig.defaultFileData()));
        root.add(
                "pipez_armory_fast_reject",
                ConfigMergeSupport.toJsonObject(PipezArmoryFastRejectConfig.defaultFileData())
        );
        root.add(
                "irons_spectral_hammer",
                ConfigMergeSupport.toJsonObject(IronsSpectralHammerConfig.defaultFileData())
        );
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
        FunctionalStorageArmorySearchConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root,
                "functionalstorage_armory_search",
                FunctionalStorageArmorySearchConfig.Data.class,
                FunctionalStorageArmorySearchConfig::defaultFileData
        ));
        RelicsServerTooltipConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "relics_server_tooltip", RelicsServerTooltipConfig.Data.class, RelicsServerTooltipConfig::defaultFileData
        ));
        MekmmWindGeneratorScanConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root,
                "mekmm_wind_generator_scan",
                MekmmWindGeneratorScanConfig.Data.class,
                MekmmWindGeneratorScanConfig::defaultFileData
        ));
        VanishPacketFilterConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root,
                "vanish_packet_filter",
                VanishPacketFilterConfig.Data.class,
                VanishPacketFilterConfig::defaultFileData
        ));
        NeovitaeMissingMappingsConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root,
                "neovitae_missing_mappings_fastpath",
                NeovitaeMissingMappingsConfig.Data.class,
                NeovitaeMissingMappingsConfig::defaultFileData
        ));
        CrowdedMobsConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "crowded_mobs", CrowdedMobsConfig.Data.class, CrowdedMobsConfig::defaultFileData
        ));
        PipezArmoryFastRejectConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root,
                "pipez_armory_fast_reject",
                PipezArmoryFastRejectConfig.Data.class,
                PipezArmoryFastRejectConfig::defaultFileData
        ));
        IronsSpectralHammerConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root,
                "irons_spectral_hammer",
                IronsSpectralHammerConfig.Data.class,
                IronsSpectralHammerConfig::defaultFileData
        ));
        if (logReload) {
            JdtPortalChunkConfig.logReload();
            EnderIoItemConduitConfig.logReload();
            RelicsBackpackScanConfig.logReload();
            ChunkLoaderRestrictConfig.logReload();
            FunctionalStorageArmorySearchConfig.logReload();
            RelicsServerTooltipConfig.logReload();
            MekmmWindGeneratorScanConfig.logReload();
            VanishPacketFilterConfig.logReload();
            NeovitaeMissingMappingsConfig.logReload();
            CrowdedMobsConfig.logReload();
            PipezArmoryFastRejectConfig.logReload();
            IronsSpectralHammerConfig.logReload();
        }
    }
}
