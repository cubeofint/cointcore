package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.shop.MarketMinPrices;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Minimum prices for global market lots.
 *
 * <p>File: {@code config/cointcore/market-min-prices.json}: {@code "prices": {"minecraft:diamond": 50, "#c:ingots": 2}}
 * — gluons per single item. {@code /marketmod exportprices} writes a template with every item on sale now.
 */
public final class MarketMinPricesConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static MarketMinPrices prices = MarketMinPrices.NONE;

    private MarketMinPricesConfig() {
    }

    public static MarketMinPrices get() {
        return prices;
    }

    public static void load() {
        MarketMinPrices loaded = loadFromDisk(false);
        prices = loaded != null ? loaded : MarketMinPrices.NONE;
    }

    public static boolean reload() {
        MarketMinPrices loaded = loadFromDisk(true);
        if (loaded == null) {
            return false;
        }
        prices = loaded;
        LOGGER.info("Reloaded market min prices ({} items, {} tags)", loaded.byItem().size(), loaded.byTag().size());
        return true;
    }

    /** Writes {@code market-min-prices.template.json} next to the config; returns its path. */
    public static Path writeTemplate(Map<String, Long> suggested) throws IOException {
        FileData data = new FileData();
        data.prices = new LinkedHashMap<>(suggested);
        Path path = configPath().resolveSibling("market-min-prices.template.json");
        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
        return path;
    }

    private static MarketMinPrices loadFromDisk(boolean reloading) {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                    GSON.toJson(new FileData(), writer);
                }
                LOGGER.info("Created default market min prices config at {}", path);
                return MarketMinPrices.NONE;
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                return parse(data != null ? data : new FileData());
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load market min prices from {}", path, exception);
            return reloading ? null : MarketMinPrices.NONE;
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading market min prices from {}", path, exception);
            return reloading ? null : MarketMinPrices.NONE;
        }
    }

    private static MarketMinPrices parse(FileData data) {
        if (!data.enabled || data.prices == null) {
            return MarketMinPrices.NONE;
        }
        Map<String, Long> byItem = new HashMap<>();
        Map<String, Long> byTag = new HashMap<>();
        for (Map.Entry<String, Long> entry : data.prices.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0L) {
                continue;
            }
            String key = entry.getKey().trim().toLowerCase(Locale.ROOT);
            if (key.startsWith("#")) {
                byTag.put(withNamespace(key.substring(1)), entry.getValue());
            } else if (!key.isEmpty()) {
                byItem.put(withNamespace(key), entry.getValue());
            }
        }
        return new MarketMinPrices(Map.copyOf(byItem), Map.copyOf(byTag));
    }

    private static String withNamespace(String id) {
        return id.indexOf(':') >= 0 ? id : "minecraft:" + id;
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve("market-min-prices.json");
    }

    private static final class FileData {
        @SerializedName("enabled")
        boolean enabled = true;

        /** Gluons per single item; "#namespace:path" for item tags. The highest matching rule wins. */
        @SerializedName("prices")
        Map<String, Long> prices = new LinkedHashMap<>();
    }
}
