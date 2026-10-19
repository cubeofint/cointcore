package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.mawlee.cointcore.CointCore;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Client-only placement of the inventory/creative remote-market icon.
 * File: {@code config/cointcore/market_button.json}
 */
public final class MarketButtonConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MIN_COORD = 0;
    private static final int MAX_COORD = 1024;
    /** Third row in the top-left icon cluster, below common utility-mod buttons. */
    public static final int DEFAULT_X = 4;
    public static final int DEFAULT_Y = 42;

    private static int inventoryX = DEFAULT_X;
    private static int inventoryY = DEFAULT_Y;
    private static int creativeX = DEFAULT_X;
    private static int creativeY = DEFAULT_Y;

    private MarketButtonConfig() {
    }

    public static int inventoryX() {
        return inventoryX;
    }

    public static int inventoryY() {
        return inventoryY;
    }

    public static int creativeX() {
        return creativeX;
    }

    public static int creativeY() {
        return creativeY;
    }

    public static void load() {
        apply(loadFromDisk());
    }

    static Loaded parse(FileData data) {
        FileData source = data == null ? defaults() : data;
        return new Loaded(
                clamp(source.inventoryX, DEFAULT_X),
                clamp(source.inventoryY, DEFAULT_Y),
                clamp(source.creativeX, DEFAULT_X),
                clamp(source.creativeY, DEFAULT_Y)
        );
    }

    private static void apply(Loaded loaded) {
        inventoryX = loaded.inventoryX();
        inventoryY = loaded.inventoryY();
        creativeX = loaded.creativeX();
        creativeY = loaded.creativeY();
    }

    private static Loaded loadFromDisk() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                FileData defaults = defaults();
                try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                    GSON.toJson(defaults, writer);
                }
                LOGGER.info("Created default market button config at {}", path);
                return parse(defaults);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                return parse(data);
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load market button config from {}", path, exception);
            return parse(defaults());
        }
    }

    private static int clamp(Integer value, int fallback) {
        int coord = value == null ? fallback : value;
        return Math.max(MIN_COORD, Math.min(MAX_COORD, coord));
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve("market_button.json");
    }

    private static FileData defaults() {
        FileData data = new FileData();
        data.inventoryX = DEFAULT_X;
        data.inventoryY = DEFAULT_Y;
        data.creativeX = DEFAULT_X;
        data.creativeY = DEFAULT_Y;
        return data;
    }

    record Loaded(int inventoryX, int inventoryY, int creativeX, int creativeY) {
    }

    static final class FileData {
        @SerializedName("inventory_x")
        Integer inventoryX;
        @SerializedName("inventory_y")
        Integer inventoryY;
        @SerializedName("creative_x")
        Integer creativeX;
        @SerializedName("creative_y")
        Integer creativeY;
    }
}
