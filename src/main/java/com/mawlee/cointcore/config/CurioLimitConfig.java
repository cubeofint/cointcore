package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.mawlee.cointcore.CointCore;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Max copies of an item worn at once in Curios slots (cosmetic slots not counted).
 * File: {@code config/cointcore/curio-limits.json}. Clients never load it and use the defaults, which only matters
 * for the slot highlight: the server enforces the configured values.
 */
public final class CurioLimitConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static boolean enabled = true;
    private static long revision;
    private static Map<ResourceLocation, Integer> limits = defaultLimits();

    private CurioLimitConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static long getRevision() {
        return revision;
    }

    public static Map<ResourceLocation, Integer> getLimits() {
        return limits;
    }

    /** Limit for this item, or -1 when it is not limited. */
    public static int limitFor(Item item) {
        if (!enabled) {
            return -1;
        }
        Integer limit = limits.get(BuiltInRegistries.ITEM.getKey(item));
        return limit == null ? -1 : limit;
    }

    public static void load() {
        FileData data = loadFromDisk(false);
        apply(data != null ? data : defaultFileData());
    }

    public static boolean reload() {
        FileData data = loadFromDisk(true);
        if (data == null) {
            return false;
        }
        apply(data);
        LOGGER.info("Reloaded curio limits config (enabled={}, {} items)", enabled, limits.size());
        return true;
    }

    private static void apply(FileData data) {
        enabled = data.enabled == null || data.enabled;
        Map<ResourceLocation, Integer> parsed = new LinkedHashMap<>();
        if (data.limits != null) {
            for (Map.Entry<String, Integer> entry : data.limits.entrySet()) {
                ResourceLocation id = entry.getKey() == null ? null : ResourceLocation.tryParse(entry.getKey().trim());
                if (id == null || entry.getValue() == null || entry.getValue() < 0) {
                    LOGGER.warn("Skipping invalid curio limit {} = {}", entry.getKey(), entry.getValue());
                    continue;
                }
                if (!BuiltInRegistries.ITEM.containsKey(id)) {
                    LOGGER.warn("Unknown item in curio limits: {}", id);
                }
                parsed.put(id, entry.getValue());
            }
        }
        limits = Map.copyOf(parsed);
        revision++;
    }

    private static FileData loadFromDisk(boolean reloading) {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                FileData defaults = defaultFileData();
                try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                    GSON.toJson(defaults, writer);
                }
                LOGGER.info("Created default curio limits config at {}", path);
                return defaults;
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                return data != null ? data : defaultFileData();
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load curio limits config from {}", path, exception);
            return reloading ? null : defaultFileData();
        }
    }

    private static Map<ResourceLocation, Integer> defaultLimits() {
        return Map.of(ResourceLocation.fromNamespaceAndPath("relics", "ring_of_the_seven_deadly_sins"), 1);
    }

    private static FileData defaultFileData() {
        FileData data = new FileData();
        data.enabled = true;
        data.limits = new LinkedHashMap<>();
        defaultLimits().forEach((id, limit) -> data.limits.put(id.toString(), limit));
        return data;
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve("curio-limits.json");
    }

    private static final class FileData {
        @SerializedName("enabled")
        private Boolean enabled;

        /** Item id → max worn at once. */
        @SerializedName("limits")
        private Map<String, Integer> limits;
    }
}
