package com.mawlee.cointcore.watchdog.probes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mawlee.cointcore.CointCore;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads the nested {@code probes} object from {@code config/cointcore/tick-watchdog.json}
 * without rewriting the rest of the file (avoids clashing with other watchdog PRs).
 */
public final class WatchdogProbeConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private WatchdogProbeConfig() {
    }

    public static void load() {
        applyFromDisk(false);
    }

    public static boolean reload() {
        return applyFromDisk(true);
    }

    public static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve("tick-watchdog.json");
    }

    private static boolean applyFromDisk(boolean reloading) {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                WatchdogProbeSettings.apply(WatchdogProbeSettings.Snapshot.defaults());
                return true;
            }
            JsonObject root;
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                root = parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
            }
            if (!root.has("probes") || !root.get("probes").isJsonObject()) {
                root.add("probes", WatchdogProbeSettings.defaultJson());
                Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
                LOGGER.info("Added default watchdog probe settings to {}", path);
            }
            WatchdogProbeSettings.apply(WatchdogProbeSettings.parse(root));
            return true;
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load watchdog probe config from {}", configPath(), exception);
            if (!reloading) {
                WatchdogProbeSettings.apply(WatchdogProbeSettings.Snapshot.defaults());
            }
            return false;
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading watchdog probe config from {}", configPath(), exception);
            if (!reloading) {
                WatchdogProbeSettings.apply(WatchdogProbeSettings.Snapshot.defaults());
            }
            return false;
        }
    }
}
