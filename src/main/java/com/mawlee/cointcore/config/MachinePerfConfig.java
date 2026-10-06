package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Server-side caps for expensive machine BE ticks (MGU Saw, RFTools Builder).
 *
 * <p>File: {@code config/cointcore/tick-throttles.json} section {@code machine}.
 */
public final class MachinePerfConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int MIN_SAW_INTERVAL = 10;
    private static final int MAX_SAW_INTERVAL = 200;
    private static final int MIN_SAW_KILLS = 1;
    private static final int MAX_SAW_KILLS = 64;
    private static final int MIN_BUILDER_BLOCKS = 0;
    private static final int MAX_BUILDER_BLOCKS = 64;
    private static final int MIN_BUILDER_INFUSION = 0;
    private static final int MAX_BUILDER_INFUSION = 64;
    private static final int MIN_BUILDER_SHAPED_INTERVAL = 1;
    private static final int MAX_BUILDER_SHAPED_INTERVAL = 40;
    private static final int MIN_BUILDER_GLOBAL = 0;
    private static final int MAX_BUILDER_GLOBAL = 256;
    private static final int MIN_BUILDER_FORMULA = 0;
    private static final int MAX_BUILDER_FORMULA = 8;
    private static final int MIN_BUILDER_CHUNK_LOADS = 0;
    private static final int MAX_BUILDER_CHUNK_LOADS = 8;
    private static final int MIN_BUILDER_COLLECT = 1;
    private static final int MAX_BUILDER_COLLECT = 200;

    private static boolean mguSawEnabled = true;
    private static int mguSawIntervalTicks = 20;
    private static int mguSawMaxKillsPerPulse = 4;

    private static boolean rfToolsBuilderEnabled = true;
    private static int rfToolsMaxQuarryBaseSpeed = 2;
    private static int rfToolsMaxInfusionSpeedFactor = 4;
    private static int rfToolsShapedTickInterval = 8;
    /** Shared across all Builders per server tick; {@code 0} = unlimited. */
    private static int rfToolsGlobalBlocksPerTick = 16;
    /** Shape-card {@code composeFormula} rebuilds per server tick; {@code 0} = unlimited. */
    private static int rfToolsFormulaRebuildsPerTick = 1;
    /** New quarry chunk-load tickets per server tick; {@code 0} = unlimited. */
    private static int rfToolsChunkLoadsPerTick = 1;
    /** Floor for RFTools {@code collectTimer}. Collect mode scans the whole chamber. */
    private static int rfToolsMinCollectTimer = 40;

    private MachinePerfConfig() {
    }

    public static boolean isMguSawEnabled() {
        return mguSawEnabled;
    }

    public static int getMguSawIntervalTicks() {
        return mguSawIntervalTicks;
    }

    public static int getMguSawMaxKillsPerPulse() {
        return mguSawMaxKillsPerPulse;
    }

    public static boolean isRfToolsBuilderEnabled() {
        return rfToolsBuilderEnabled;
    }

    public static int getRfToolsMaxQuarryBaseSpeed() {
        return rfToolsMaxQuarryBaseSpeed;
    }

    public static int getRfToolsMaxInfusionSpeedFactor() {
        return rfToolsMaxInfusionSpeedFactor;
    }

    public static int getRfToolsShapedTickInterval() {
        return rfToolsShapedTickInterval;
    }

    public static int getRfToolsGlobalBlocksPerTick() {
        return rfToolsGlobalBlocksPerTick;
    }

    public static int getRfToolsFormulaRebuildsPerTick() {
        return rfToolsFormulaRebuildsPerTick;
    }

    public static int getRfToolsChunkLoadsPerTick() {
        return rfToolsChunkLoadsPerTick;
    }

    public static int getRfToolsMinCollectTimer() {
        return rfToolsMinCollectTimer;
    }

    public static Path getConfigPath() {
        return TickThrottleConfigs.path();
    }

    public static void load() {
        TickThrottleConfigs.load();
    }

    public static boolean reload() {
        return TickThrottleConfigs.reload();
    }

    static void applySection(FileData data) {
        apply(parse(data != null ? data : defaultFileData()));
    }

    static void logReload() {
        LOGGER.info(
                "Reloaded machine perf (mguSaw={} interval={} maxKills={}, rfBuilder={} base={} infusion={} shapedEvery={} globalBudget={} formulaRebuilds={} chunkLoads={} minCollect={})",
                mguSawEnabled,
                mguSawIntervalTicks,
                mguSawMaxKillsPerPulse,
                rfToolsBuilderEnabled,
                rfToolsMaxQuarryBaseSpeed,
                rfToolsMaxInfusionSpeedFactor,
                rfToolsShapedTickInterval,
                rfToolsGlobalBlocksPerTick,
                rfToolsFormulaRebuildsPerTick,
                rfToolsChunkLoadsPerTick,
                rfToolsMinCollectTimer
        );
    }

    private static void apply(LoadedConfig loaded) {
        mguSawEnabled = loaded.mguSawEnabled();
        mguSawIntervalTicks = loaded.mguSawIntervalTicks();
        mguSawMaxKillsPerPulse = loaded.mguSawMaxKillsPerPulse();
        rfToolsBuilderEnabled = loaded.rfToolsBuilderEnabled();
        rfToolsMaxQuarryBaseSpeed = loaded.rfToolsMaxQuarryBaseSpeed();
        rfToolsMaxInfusionSpeedFactor = loaded.rfToolsMaxInfusionSpeedFactor();
        rfToolsShapedTickInterval = loaded.rfToolsShapedTickInterval();
        rfToolsGlobalBlocksPerTick = loaded.rfToolsGlobalBlocksPerTick();
        rfToolsFormulaRebuildsPerTick = loaded.rfToolsFormulaRebuildsPerTick();
        rfToolsChunkLoadsPerTick = loaded.rfToolsChunkLoadsPerTick();
        rfToolsMinCollectTimer = loaded.rfToolsMinCollectTimer();
    }

    private static LoadedConfig parse(FileData data) {
        boolean sawEnabled = data.mguSawEnabled == null || data.mguSawEnabled;
        int sawInterval = clamp(
                data.mguSawIntervalTicks != null ? data.mguSawIntervalTicks : 20,
                MIN_SAW_INTERVAL,
                MAX_SAW_INTERVAL
        );
        int sawKills = clamp(
                data.mguSawMaxKillsPerPulse != null ? data.mguSawMaxKillsPerPulse : 4,
                MIN_SAW_KILLS,
                MAX_SAW_KILLS
        );

        boolean builderEnabled = data.rfToolsBuilderEnabled == null || data.rfToolsBuilderEnabled;
        int baseSpeed = clamp(
                data.rfToolsMaxQuarryBaseSpeed != null ? data.rfToolsMaxQuarryBaseSpeed : 2,
                MIN_BUILDER_BLOCKS,
                MAX_BUILDER_BLOCKS
        );
        int infusion = clamp(
                data.rfToolsMaxInfusionSpeedFactor != null ? data.rfToolsMaxInfusionSpeedFactor : 4,
                MIN_BUILDER_INFUSION,
                MAX_BUILDER_INFUSION
        );
        int shapedInterval = clamp(
                data.rfToolsShapedTickInterval != null ? data.rfToolsShapedTickInterval : 8,
                MIN_BUILDER_SHAPED_INTERVAL,
                MAX_BUILDER_SHAPED_INTERVAL
        );
        int globalBudget = clamp(
                data.rfToolsGlobalBlocksPerTick != null ? data.rfToolsGlobalBlocksPerTick : 16,
                MIN_BUILDER_GLOBAL,
                MAX_BUILDER_GLOBAL
        );
        int formulaRebuilds = clamp(
                data.rfToolsFormulaRebuildsPerTick != null ? data.rfToolsFormulaRebuildsPerTick : 1,
                MIN_BUILDER_FORMULA,
                MAX_BUILDER_FORMULA
        );
        int chunkLoads = clamp(
                data.rfToolsChunkLoadsPerTick != null ? data.rfToolsChunkLoadsPerTick : 1,
                MIN_BUILDER_CHUNK_LOADS,
                MAX_BUILDER_CHUNK_LOADS
        );
        int minCollect = clamp(
                data.rfToolsMinCollectTimer != null ? data.rfToolsMinCollectTimer : 40,
                MIN_BUILDER_COLLECT,
                MAX_BUILDER_COLLECT
        );

        return new LoadedConfig(
                sawEnabled, sawInterval, sawKills,
                builderEnabled, baseSpeed, infusion, shapedInterval, globalBudget,
                formulaRebuilds, chunkLoads, minCollect
        );
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    static FileData defaultFileData() {
        FileData data = new FileData();
        data.mguSawEnabled = true;
        data.mguSawIntervalTicks = 20;
        data.mguSawMaxKillsPerPulse = 4;
        data.rfToolsBuilderEnabled = true;
        data.rfToolsMaxQuarryBaseSpeed = 2;
        data.rfToolsMaxInfusionSpeedFactor = 4;
        data.rfToolsShapedTickInterval = 8;
        data.rfToolsGlobalBlocksPerTick = 16;
        data.rfToolsFormulaRebuildsPerTick = 1;
        data.rfToolsChunkLoadsPerTick = 1;
        data.rfToolsMinCollectTimer = 40;
        return data;
    }

    private record LoadedConfig(
            boolean mguSawEnabled,
            int mguSawIntervalTicks,
            int mguSawMaxKillsPerPulse,
            boolean rfToolsBuilderEnabled,
            int rfToolsMaxQuarryBaseSpeed,
            int rfToolsMaxInfusionSpeedFactor,
            int rfToolsShapedTickInterval,
            int rfToolsGlobalBlocksPerTick,
            int rfToolsFormulaRebuildsPerTick,
            int rfToolsChunkLoadsPerTick,
            int rfToolsMinCollectTimer
    ) {
    }

    static final class FileData {
        @SerializedName("mguSawEnabled")
        Boolean mguSawEnabled;

        @SerializedName("mguSawIntervalTicks")
        Integer mguSawIntervalTicks;

        @SerializedName("mguSawMaxKillsPerPulse")
        Integer mguSawMaxKillsPerPulse;

        @SerializedName("rfToolsBuilderEnabled")
        Boolean rfToolsBuilderEnabled;

        @SerializedName("rfToolsMaxQuarryBaseSpeed")
        Integer rfToolsMaxQuarryBaseSpeed;

        @SerializedName("rfToolsMaxInfusionSpeedFactor")
        Integer rfToolsMaxInfusionSpeedFactor;

        @SerializedName("rfToolsShapedTickInterval")
        Integer rfToolsShapedTickInterval;

        @SerializedName("rfToolsGlobalBlocksPerTick")
        Integer rfToolsGlobalBlocksPerTick;

        @SerializedName("rfToolsFormulaRebuildsPerTick")
        Integer rfToolsFormulaRebuildsPerTick;

        @SerializedName("rfToolsChunkLoadsPerTick")
        Integer rfToolsChunkLoadsPerTick;

        @SerializedName("rfToolsMinCollectTimer")
        Integer rfToolsMinCollectTimer;
    }
}
