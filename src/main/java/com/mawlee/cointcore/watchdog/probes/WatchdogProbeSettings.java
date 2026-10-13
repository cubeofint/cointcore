package com.mawlee.cointcore.watchdog.probes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Toggleable probe settings. Defaults are on only when the idle cost is a few
 * increments / MXBean reads (no per-block work beyond a counter).
 */
public final class WatchdogProbeSettings {
    public record Snapshot(
            boolean gcMemory,
            boolean chunks,
            boolean diskSave,
            boolean serverTasks,
            boolean blockUpdates,
            boolean groundEntities,
            boolean pathfinding,
            boolean network,
            boolean modNetworks,
            boolean trends,
            int analysisIntervalSeconds,
            int groundScanIntervalSeconds,
            int trendLogIntervalSeconds,
            int stackSampleCount,
            double gcPauseFlagMs,
            int topEntries
    ) {
        public static Snapshot defaults() {
            return new Snapshot(
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    15,
                    15,
                    60,
                    16,
                    20.0D,
                    8
            );
        }
    }

    private static volatile Snapshot current = Snapshot.defaults();

    private WatchdogProbeSettings() {
    }

    public static Snapshot current() {
        return current;
    }

    public static void apply(Snapshot snapshot) {
        current = snapshot == null ? Snapshot.defaults() : snapshot;
        WatchdogProbeHub.syncHotFlags();
    }

    public static Snapshot parse(JsonObject root) {
        Snapshot fallback = Snapshot.defaults();
        JsonObject probes = probesObject(root);
        return new Snapshot(
                bool(probes, "gcMemory", fallback.gcMemory()),
                bool(probes, "chunks", fallback.chunks()),
                bool(probes, "diskSave", fallback.diskSave()),
                bool(probes, "serverTasks", fallback.serverTasks()),
                bool(probes, "blockUpdates", fallback.blockUpdates()),
                bool(probes, "groundEntities", fallback.groundEntities()),
                bool(probes, "pathfinding", fallback.pathfinding()),
                bool(probes, "network", fallback.network()),
                bool(probes, "modNetworks", fallback.modNetworks()),
                bool(probes, "trends", fallback.trends()),
                clampInt(probes, "analysisIntervalSeconds", 5, 20, fallback.analysisIntervalSeconds()),
                clampInt(probes, "groundScanIntervalSeconds", 5, 120, fallback.groundScanIntervalSeconds()),
                clampInt(probes, "trendLogIntervalSeconds", 15, 600, fallback.trendLogIntervalSeconds()),
                clampInt(probes, "stackSampleCount", 4, 64, fallback.stackSampleCount()),
                clampDouble(probes, "gcPauseFlagMs", 1.0D, 5_000.0D, fallback.gcPauseFlagMs()),
                clampInt(probes, "topEntries", 3, 30, fallback.topEntries())
        );
    }

    public static JsonObject defaultJson() {
        Snapshot defaults = Snapshot.defaults();
        JsonObject probes = new JsonObject();
        probes.addProperty("gcMemory", defaults.gcMemory());
        probes.addProperty("chunks", defaults.chunks());
        probes.addProperty("diskSave", defaults.diskSave());
        probes.addProperty("serverTasks", defaults.serverTasks());
        probes.addProperty("blockUpdates", defaults.blockUpdates());
        probes.addProperty("groundEntities", defaults.groundEntities());
        probes.addProperty("pathfinding", defaults.pathfinding());
        probes.addProperty("network", defaults.network());
        probes.addProperty("modNetworks", defaults.modNetworks());
        probes.addProperty("trends", defaults.trends());
        probes.addProperty("analysisIntervalSeconds", defaults.analysisIntervalSeconds());
        probes.addProperty("groundScanIntervalSeconds", defaults.groundScanIntervalSeconds());
        probes.addProperty("trendLogIntervalSeconds", defaults.trendLogIntervalSeconds());
        probes.addProperty("stackSampleCount", defaults.stackSampleCount());
        probes.addProperty("gcPauseFlagMs", defaults.gcPauseFlagMs());
        probes.addProperty("topEntries", defaults.topEntries());
        return probes;
    }

    private static JsonObject probesObject(JsonObject root) {
        if (root == null) {
            return new JsonObject();
        }
        JsonElement probes = root.get("probes");
        if (probes != null && probes.isJsonObject()) {
            return probes.getAsJsonObject();
        }
        return new JsonObject();
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsBoolean();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static int clampInt(JsonObject object, String key, int min, int max, int fallback) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            int value = element.getAsInt();
            if (value < min || value > max) {
                return fallback;
            }
            return value;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static double clampDouble(JsonObject object, String key, double min, double max, double fallback) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            double value = element.getAsDouble();
            if (Double.isNaN(value) || Double.isInfinite(value) || value < min || value > max) {
                return fallback;
            }
            return value;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
