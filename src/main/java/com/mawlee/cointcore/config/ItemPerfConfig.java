package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.mawlee.cointcore.CointCore;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Grounded {@link net.minecraft.world.entity.item.ItemEntity} sleep/throttle
 * ({@code config/cointcore/item-perf.json}).
 */
public final class ItemPerfConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DEFAULT_RESOURCE = "/cointcore/default_configs/item-perf.json";

    private static boolean enabled = true;
    private static int minAgeBeforeSleep = 60;
    private static int minWaterStillTicksBeforeSleep = 10;
    private static double playerWakeRadius = 32.0;
    private static int sleepTickInterval = 20;
    private static boolean requireOnGround = true;
    /** When true, items in water never sleep (old behavior). Lava never sleeps either way. */
    private static boolean skipIfInFluid = false;
    private static double stillMotionThresholdSqr = 0.01 * 0.01;
    private static boolean massDropGuard = true;
    private static int massDropSoftEntitiesPerPos = 64;
    private static int massDropMaxEntitiesPerPos = 256;
    private static int massDropMaxEntitiesPerLevelTick = 1024;
    private static int massDropPileMaxEntries = 256;

    private ItemPerfConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static int minAgeBeforeSleep() {
        return minAgeBeforeSleep;
    }

    public static int minWaterStillTicksBeforeSleep() {
        return minWaterStillTicksBeforeSleep;
    }

    public static double playerWakeRadius() {
        return playerWakeRadius;
    }

    public static int sleepTickInterval() {
        return sleepTickInterval;
    }

    public static boolean requireOnGround() {
        return requireOnGround;
    }

    public static boolean skipIfInFluid() {
        return skipIfInFluid;
    }

    public static double stillMotionThresholdSqr() {
        return stillMotionThresholdSqr;
    }

    public static boolean massDropGuard() {
        return massDropGuard;
    }

    /** Item entities per block position per tick before drops switch to full stacks. */
    public static int massDropSoftEntitiesPerPos() {
        return massDropSoftEntitiesPerPos;
    }

    /** Item entities per block position per tick before the rest is packed into item piles. */
    public static int massDropMaxEntitiesPerPos() {
        return massDropMaxEntitiesPerPos;
    }

    public static int massDropMaxEntitiesPerLevelTick() {
        return massDropMaxEntitiesPerLevelTick;
    }

    public static int massDropPileMaxEntries() {
        return massDropPileMaxEntries;
    }

    public static Path getConfigPath() {
        return configPath();
    }

    public static void load() {
        apply(loadFromDisk(false));
    }

    public static boolean reload() {
        FileData data = loadFromDisk(true);
        if (data == null) {
            return false;
        }
        apply(data);
        LOGGER.info(
                "Reloaded item perf config (enabled={} minAge={} waterStill={} wakeRadius={} interval={} onGround={} skipFluid={} stillMotion={} massDrop={} soft/pos={} max/pos={} max/tick={} pileEntries={})",
                enabled,
                minAgeBeforeSleep,
                minWaterStillTicksBeforeSleep,
                playerWakeRadius,
                sleepTickInterval,
                requireOnGround,
                skipIfInFluid,
                Math.sqrt(stillMotionThresholdSqr),
                massDropGuard,
                massDropSoftEntitiesPerPos,
                massDropMaxEntitiesPerPos,
                massDropMaxEntitiesPerLevelTick,
                massDropPileMaxEntries
        );
        return true;
    }

    private static void apply(FileData data) {
        enabled = data.enabled == null || data.enabled;
        requireOnGround = data.requireOnGround == null || data.requireOnGround;
        boolean legacyConfig = data.stillMotionThreshold == null;
        skipIfInFluid = !legacyConfig && data.skipIfInFluid != null && data.skipIfInFluid;
        minAgeBeforeSleep = clamp(data.minAgeBeforeSleep != null ? data.minAgeBeforeSleep : 60, 0, 6000);
        minWaterStillTicksBeforeSleep = clamp(
                data.minWaterStillTicksBeforeSleep != null ? data.minWaterStillTicksBeforeSleep : 10,
                1,
                200
        );
        sleepTickInterval = clamp(data.sleepTickInterval != null ? data.sleepTickInterval : 20, 1, 200);
        double radius = data.playerWakeRadius != null ? data.playerWakeRadius : 32.0;
        playerWakeRadius = Math.max(0.0, Math.min(radius, 128.0));
        double still = data.stillMotionThreshold != null ? data.stillMotionThreshold : 0.01;
        still = Math.max(0.0, Math.min(still, 1.0));
        stillMotionThresholdSqr = still * still;
        massDropGuard = data.massDropGuard == null || data.massDropGuard;
        massDropSoftEntitiesPerPos = clamp(
                data.massDropSoftEntitiesPerPos != null ? data.massDropSoftEntitiesPerPos : 64,
                1,
                4096
        );
        massDropMaxEntitiesPerPos = clamp(
                data.massDropMaxEntitiesPerPos != null ? data.massDropMaxEntitiesPerPos : 256,
                massDropSoftEntitiesPerPos,
                8192
        );
        massDropMaxEntitiesPerLevelTick = clamp(
                data.massDropMaxEntitiesPerLevelTick != null ? data.massDropMaxEntitiesPerLevelTick : 1024,
                1,
                65536
        );
        massDropPileMaxEntries = clamp(
                data.massDropPileMaxEntries != null ? data.massDropPileMaxEntries : 256,
                1,
                1024
        );
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private static FileData loadFromDisk(boolean reloading) {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                createDefaultConfig(path);
                LOGGER.info("Created default item perf config at {}", path);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                return data != null ? data : new FileData();
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.error("Failed to load item perf config from {}", configPath(), exception);
            return reloading ? null : readBundledDefaults();
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while loading item perf config from {}", configPath(), exception);
            return reloading ? null : readBundledDefaults();
        }
    }

    private static void createDefaultConfig(Path path) throws IOException {
        try (InputStream in = ItemPerfConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in != null) {
                Files.copy(in, path);
                return;
            }
        }
        LOGGER.warn("Missing bundled {}; writing defaults", DEFAULT_RESOURCE);
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(new FileData(), writer);
        }
    }

    private static FileData readBundledDefaults() {
        try (InputStream in = ItemPerfConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null) {
                return new FileData();
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                FileData data = GSON.fromJson(reader, FileData.class);
                return data != null ? data : new FileData();
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.warn("Failed to read bundled item perf defaults", exception);
            return new FileData();
        }
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CointCore.MOD_ID).resolve("item-perf.json");
    }

    private static final class FileData {
        @SerializedName("enabled")
        private Boolean enabled = true;

        @SerializedName("minAgeBeforeSleep")
        private Integer minAgeBeforeSleep = 60;

        @SerializedName("minWaterStillTicksBeforeSleep")
        private Integer minWaterStillTicksBeforeSleep = 10;

        @SerializedName("playerWakeRadius")
        private Double playerWakeRadius = 32.0;

        @SerializedName("sleepTickInterval")
        private Integer sleepTickInterval = 20;

        @SerializedName("requireOnGround")
        private Boolean requireOnGround = true;

        @SerializedName("skipIfInFluid")
        private Boolean skipIfInFluid = false;

        @SerializedName("stillMotionThreshold")
        private Double stillMotionThreshold = 0.01;

        @SerializedName("massDropGuard")
        private Boolean massDropGuard = true;

        @SerializedName("massDropSoftEntitiesPerPos")
        private Integer massDropSoftEntitiesPerPos = 64;

        @SerializedName("massDropMaxEntitiesPerPos")
        private Integer massDropMaxEntitiesPerPos = 256;

        @SerializedName("massDropMaxEntitiesPerLevelTick")
        private Integer massDropMaxEntitiesPerLevelTick = 1024;

        @SerializedName("massDropPileMaxEntries")
        private Integer massDropPileMaxEntries = 256;
    }
}
