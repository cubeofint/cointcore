package com.mawlee.cointcore.config;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Owns {@code config/cointcore/tick-throttles.json} with machine / pot / aura / FA / JDT / Powah / SFM sections.
 */
public final class TickThrottleConfigs {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_NAME = "tick-throttles.json";

    private TickThrottleConfigs() {
    }

    public static Path path() {
        return ConfigMergeSupport.path(FILE_NAME);
    }

    public static void load() {
        try {
            applyRoot(readRoot(), false);
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load {}; using defaults", FILE_NAME, exception);
            MachinePerfConfig.applySection(MachinePerfConfig.defaultFileData());
            BotanyPotsPerfConfig.applySection(BotanyPotsPerfConfig.defaultFileData());
            NaturesAuraPerfConfig.applySection(NaturesAuraPerfConfig.defaultFileData());
            ForbiddenArcanusPerfConfig.applySection(ForbiddenArcanusPerfConfig.defaultFileData());
            JustDireThingsPerfConfig.applySection(JustDireThingsPerfConfig.defaultFileData());
            PowahPerfConfig.applySection(PowahPerfConfig.defaultFileData());
            SfmPerfConfig.applySection(SfmPerfConfig.defaultFileData());
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
                        "machine", "machine_perf.json",
                        "botanypots", "botanypots-perf.json",
                        "naturesaura", "naturesaura_perf.json",
                        "forbidden_arcanus", "forbidden_arcanus_perf.json",
                        "justdirethings", "justdirethings-perf.json",
                        "powah", "powah-perf.json",
                        "sfm", "sfm-perf.json"
                ),
                TickThrottleConfigs::defaults,
                false,
                LOGGER
        );
    }

    private static JsonObject defaults() {
        JsonObject root = new JsonObject();
        root.add("machine", ConfigMergeSupport.toJsonObject(MachinePerfConfig.defaultFileData()));
        root.add("botanypots", ConfigMergeSupport.toJsonObject(BotanyPotsPerfConfig.defaultFileData()));
        root.add("naturesaura", ConfigMergeSupport.toJsonObject(NaturesAuraPerfConfig.defaultFileData()));
        root.add("forbidden_arcanus", ConfigMergeSupport.toJsonObject(ForbiddenArcanusPerfConfig.defaultFileData()));
        root.add("justdirethings", ConfigMergeSupport.toJsonObject(JustDireThingsPerfConfig.defaultFileData()));
        root.add("powah", ConfigMergeSupport.toJsonObject(PowahPerfConfig.defaultFileData()));
        root.add("sfm", ConfigMergeSupport.toJsonObject(SfmPerfConfig.defaultFileData()));
        return root;
    }

    private static void applyRoot(JsonObject root, boolean logReload) {
        MachinePerfConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "machine", MachinePerfConfig.FileData.class, MachinePerfConfig::defaultFileData
        ));
        BotanyPotsPerfConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "botanypots", BotanyPotsPerfConfig.FileData.class, BotanyPotsPerfConfig::defaultFileData
        ));
        NaturesAuraPerfConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "naturesaura", NaturesAuraPerfConfig.FileData.class, NaturesAuraPerfConfig::defaultFileData
        ));
        ForbiddenArcanusPerfConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "forbidden_arcanus", ForbiddenArcanusPerfConfig.FileData.class,
                ForbiddenArcanusPerfConfig::defaultFileData
        ));
        JustDireThingsPerfConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "justdirethings", JustDireThingsPerfConfig.Data.class, JustDireThingsPerfConfig::defaultFileData
        ));
        PowahPerfConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "powah", PowahPerfConfig.Data.class, PowahPerfConfig::defaultFileData
        ));
        SfmPerfConfig.applySection(ConfigMergeSupport.sectionOrDefault(
                root, "sfm", SfmPerfConfig.FileData.class, SfmPerfConfig::defaultFileData
        ));
        if (logReload) {
            MachinePerfConfig.logReload();
            BotanyPotsPerfConfig.logReload();
            NaturesAuraPerfConfig.logReload();
            ForbiddenArcanusPerfConfig.logReload();
            JustDireThingsPerfConfig.logReload();
            PowahPerfConfig.logReload();
            SfmPerfConfig.logReload();
        }
    }
}
