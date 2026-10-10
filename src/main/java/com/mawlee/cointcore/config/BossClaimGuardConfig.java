package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Blocks FTB Chunks claims on End dragon fight chunks and Cataclysm boss structures.
 *
 * <p>File: {@code config/cointcore/claims.json} section {@code bossClaimGuard}.
 */
public final class BossClaimGuardConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static Settings settings = Settings.defaults();

    private BossClaimGuardConfig() {
    }

    public static Settings get() {
        return settings;
    }

    public static Path getConfigPath() {
        return ClaimsConfigs.path();
    }

    public static void load() {
        ClaimsConfigs.load();
    }

    public static boolean reload() {
        return ClaimsConfigs.reload();
    }

    static void applySection(FileData data) {
        apply(parse(data != null ? data : defaultFileData()));
    }

    static void logReload() {
        LOGGER.info(
                "Reloaded boss claim guard (enabled={}, endDragon={}, cataclysm={}, structures={}, endPad={})",
                settings.enabled(),
                settings.endDragon(),
                settings.cataclysmBossStructures(),
                settings.structures().size(),
                settings.endPortalChunkRadius()
        );
    }

    private static void apply(LoadedConfig loaded) {
        settings = loaded.settings;
    }

    private static LoadedConfig parse(FileData data) {
        boolean enabled = data.enabled == null || data.enabled;
        boolean endDragon = data.endDragon == null || data.endDragon;
        boolean cataclysm = data.cataclysmBossStructures == null || data.cataclysmBossStructures;
        int endPad = data.endPortalChunkRadius != null ? Math.max(0, data.endPortalChunkRadius) : 2;
        Set<ResourceLocation> structures = parseStructures(data.structures);
        return new LoadedConfig(new Settings(enabled, endDragon, cataclysm, endPad, Set.copyOf(structures)));
    }

    private static Set<ResourceLocation> parseStructures(List<String> raw) {
        List<String> source = raw == null || raw.isEmpty() ? defaultStructureIds() : raw;
        Set<ResourceLocation> out = new LinkedHashSet<>();
        for (String entry : source) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(entry.trim());
            if (id == null) {
                LOGGER.warn("Skipping invalid boss-claim-guard structure id: {}", entry);
                continue;
            }
            out.add(id);
        }
        if (out.isEmpty()) {
            for (String entry : defaultStructureIds()) {
                ResourceLocation id = ResourceLocation.tryParse(entry);
                if (id != null) {
                    out.add(id);
                }
            }
        }
        return out;
    }

    private static List<String> defaultStructureIds() {
        // Cataclysm eye_of_*_located tags — one structure per major boss.
        return List.of(
                "cataclysm:burning_arena",
                "cataclysm:soul_black_smith",
                "cataclysm:ruined_citadel",
                "cataclysm:ancient_factory",
                "cataclysm:sunken_city",
                "cataclysm:cursed_pyramid",
                "cataclysm:frosted_prison",
                "cataclysm:acropolis"
        );
    }

    static FileData defaultFileData() {
        FileData data = new FileData();
        data.enabled = true;
        data.endDragon = true;
        data.cataclysmBossStructures = true;
        data.endPortalChunkRadius = 2;
        data.structures = new ArrayList<>(defaultStructureIds());
        return data;
    }

    /**
     * @param endPortalChunkRadius Chebyshev radius in chunks around the End island origin / exit portal
     */
    public record Settings(
            boolean enabled,
            boolean endDragon,
            boolean cataclysmBossStructures,
            int endPortalChunkRadius,
            Set<ResourceLocation> structures
    ) {
        public static Settings defaults() {
            return new Settings(true, true, true, 2, Set.copyOf(parseStructures(null)));
        }
    }

    private record LoadedConfig(Settings settings) {
    }

    static final class FileData {
        @SerializedName("enabled")
        Boolean enabled;

        @SerializedName("endDragon")
        Boolean endDragon;

        @SerializedName("cataclysmBossStructures")
        Boolean cataclysmBossStructures;

        @SerializedName("endPortalChunkRadius")
        Integer endPortalChunkRadius;

        @SerializedName("structures")
        List<String> structures;
    }
}
