package com.mawlee.cointcore.config;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * File: {@code config/cointcore/cleanup.json} section {@code items}.
 */
public final class WorldCleanupConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static ItemClearSettings itemClear = ItemClearSettings.defaults();

    private WorldCleanupConfig() {
    }

    public static ItemClearSettings getItemClear() {
        return itemClear;
    }

    public static Path getConfigPath() {
        return CleanupConfigs.path();
    }

    public static void load() {
        CleanupConfigs.load();
    }

    public static boolean reload() {
        return CleanupConfigs.reload();
    }

    static void applySection(FileData data) {
        apply(parse(data != null ? data : defaultFileData()));
    }

    static void logReload() {
        LOGGER.info(
                "Reloaded world cleanup config (enabled: {}, interval: {} min, threshold: {} items, dimensions: {})",
                itemClear.enabled(),
                itemClear.checkIntervalMinutes(),
                itemClear.itemThreshold(),
                itemClear.checkDimensions().stream().map(key -> key.location().toString()).toList()
        );
    }

    private static void apply(LoadedConfig loaded) {
        itemClear = loaded.itemClear;
    }

    private static LoadedConfig parse(FileData data) {
        ItemClearFile source = data.itemClear != null ? data.itemClear : new ItemClearFile();
        return new LoadedConfig(normalizeItemClear(source));
    }

    private static ItemClearSettings normalizeItemClear(ItemClearFile source) {
        List<Integer> warnings = new ArrayList<>();
        if (source.warningsSecondsBefore != null) {
            for (Integer value : source.warningsSecondsBefore) {
                if (value != null && value > 0) {
                    warnings.add(value);
                }
            }
        }
        if (warnings.isEmpty()) {
            warnings.addAll(List.of(5, 20));
        } else {
            warnings.sort(Comparator.naturalOrder());
        }

        int intervalMinutes = source.checkIntervalMinutes != null ? source.checkIntervalMinutes : 30;
        intervalMinutes = Math.max(0, Math.min(1440, intervalMinutes));

        int threshold = source.itemThreshold != null ? source.itemThreshold : 1000;
        threshold = Math.max(0, threshold);

        List<ResourceKey<Level>> dimensions = parseDimensions(source);

        String warningText = source.warningText == null || source.warningText.isBlank()
                ? ItemClearSettings.defaults().warningText()
                : source.warningText.trim();
        String titleText = source.titleText == null || source.titleText.isBlank()
                ? ItemClearSettings.defaults().titleText()
                : source.titleText.trim();

        return new ItemClearSettings(
                source.enabled != null ? source.enabled : true,
                intervalMinutes,
                threshold,
                List.copyOf(warnings),
                warningText,
                source.showTitleOnFirstWarning != null ? source.showTitleOnFirstWarning : true,
                titleText,
                dimensions,
                source.requireItemThreshold != null ? source.requireItemThreshold : true
        );
    }

    private static List<ResourceKey<Level>> parseDimensions(ItemClearFile source) {
        Set<String> raw = new LinkedHashSet<>();
        if (source.checkDimensions != null) {
            for (String value : source.checkDimensions) {
                if (value != null && !value.isBlank()) {
                    raw.add(value.trim());
                }
            }
        }
        addDimensionStrings(raw, source.checkDimensionRaw);

        if (raw.isEmpty()) {
            return List.of(Level.OVERWORLD);
        }

        List<ResourceKey<Level>> dimensions = new ArrayList<>();
        for (String entry : raw) {
            ResourceLocation id = ResourceLocation.tryParse(entry);
            if (id == null) {
                LOGGER.warn("Invalid world cleanup dimension '{}', skipping", entry);
                continue;
            }
            dimensions.add(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id));
        }

        if (dimensions.isEmpty()) {
            LOGGER.warn("No valid world cleanup dimensions configured, using overworld");
            return List.of(Level.OVERWORLD);
        }

        return List.copyOf(dimensions);
    }

    private static void addDimensionStrings(Set<String> raw, JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
                addDimensionString(raw, entry);
            }
            return;
        }
        addDimensionString(raw, element);
    }

    private static void addDimensionString(Set<String> raw, JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) {
            return;
        }
        String value = element.getAsString();
        if (value != null && !value.isBlank()) {
            raw.add(value.trim());
        }
    }

    static FileData defaultFileData() {
        FileData data = new FileData();
        ItemClearFile itemClearFile = new ItemClearFile();
        itemClearFile.enabled = true;
        itemClearFile.checkIntervalMinutes = 30;
        itemClearFile.itemThreshold = 1000;
        itemClearFile.warningsSecondsBefore = List.of(5, 20);
        itemClearFile.warningText = "ITEMCLEAR IN % SECONDS";
        itemClearFile.showTitleOnFirstWarning = true;
        itemClearFile.titleText = "ITEMCLEAR INCOMING!";
        itemClearFile.checkDimensions = List.of("minecraft:overworld");
        itemClearFile.requireItemThreshold = true;
        data.itemClear = itemClearFile;
        return data;
    }

    public record ItemClearSettings(
            boolean enabled,
            int checkIntervalMinutes,
            int itemThreshold,
            List<Integer> warningsSecondsBefore,
            String warningText,
            boolean showTitleOnFirstWarning,
            String titleText,
            List<ResourceKey<Level>> checkDimensions,
            boolean requireItemThreshold
    ) {
        public static ItemClearSettings defaults() {
            return new ItemClearSettings(
                    true,
                    30,
                    1000,
                    List.of(5, 20),
                    "ITEMCLEAR IN % SECONDS",
                    true,
                    "ITEMCLEAR INCOMING!",
                    List.of(Level.OVERWORLD),
                    true
            );
        }
    }

    private record LoadedConfig(ItemClearSettings itemClear) {
    }

    static final class FileData {
        @SerializedName("itemClear")
        private ItemClearFile itemClear;
    }

    private static final class ItemClearFile {
        @SerializedName("enabled")
        private Boolean enabled;

        @SerializedName("checkIntervalMinutes")
        private Integer checkIntervalMinutes;

        @SerializedName("itemThreshold")
        private Integer itemThreshold;

        @SerializedName("warningsSecondsBefore")
        private List<Integer> warningsSecondsBefore;

        @SerializedName("warningText")
        private String warningText;

        @SerializedName("showTitleOnFirstWarning")
        private Boolean showTitleOnFirstWarning;

        @SerializedName("titleText")
        private String titleText;

        @SerializedName("checkDimensions")
        private List<String> checkDimensions;

        @SerializedName("requireItemThreshold")
        private Boolean requireItemThreshold;

        /** Accepts a string or array — use {@link #checkDimensions} for new configs. */
        @SerializedName("checkDimension")
        private JsonElement checkDimensionRaw;
    }
}
