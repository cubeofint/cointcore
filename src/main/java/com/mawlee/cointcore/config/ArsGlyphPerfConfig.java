package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mawlee.cointcore.spark.PerfTickCache;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Server-side caps / caches for expensive Ars Nouveau glyphs
 * (Orbit, Crush, Toss, Wololo, Burst, Chaining, Propagate Plane, Trail, Linger, Wall).
 *
 * <p>File: {@code config/cointcore/ars-perf.json} section {@code glyph}.
 */
public final class ArsGlyphPerfConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int MIN_INTERVAL = 1;
    private static final int MAX_INTERVAL = 40;
    private static final int MIN_PROJECTILES = 1;
    private static final int MAX_PROJECTILES = 16;
    private static final int MIN_LIFETIME = 100;
    private static final int MAX_LIFETIME = 12000;
    private static final int MIN_EXTEND = 0;
    private static final int MAX_EXTEND = 20;
    private static final int MIN_AOE_BLOCKS = 1;
    private static final int MAX_AOE_BLOCKS = 512;
    private static final int MIN_COOLDOWN = 0;
    private static final int MAX_COOLDOWN = 100;
    private static final int MIN_TOSS_STACK = 1;
    private static final int MAX_TOSS_STACK = 256;
    private static final double MIN_MSPT = 0.0D;
    private static final double MAX_MSPT = 1000.0D;
    private static final int MIN_BURST_RADIUS = 1;
    private static final int MAX_BURST_RADIUS = 8;
    private static final int MIN_CHAIN_TARGETS = 1;
    private static final int MAX_CHAIN_TARGETS = 64;
    private static final int MIN_SEARCH_RADIUS = 1;
    private static final int MAX_SEARCH_RADIUS = 16;
    private static final int MIN_RESOLVES = 16;
    private static final int MAX_RESOLVES = 512;
    private static final int MIN_PLANE_RADIUS = 1;
    private static final int MAX_PLANE_RADIUS = 8;
    private static final int MIN_PLANE_PIERCE = 0;
    private static final int MAX_PLANE_PIERCE = 8;
    private static final int MIN_FIELD_RADIUS = 0;
    private static final int MAX_FIELD_RADIUS = 8;

    private static boolean enabled = true;

    private static boolean crushCacheRecipes = true;
    private static int crushMaxAoeBlocks = 64;
    private static boolean crushClaimFilter = true;

    private static int orbitResolveInterval = 4;
    private static int orbitBlockResolveInterval = 5;
    private static int orbitMsptResolveInterval = 8;
    private static double orbitMsptThreshold = 40.0D;
    private static int orbitMaxProjectiles = 5;
    private static int orbitMaxLifetimeTicks = 2400;
    private static int orbitMaxExtendTimes = 2;

    private static int tossCooldownTicks = 5;
    private static int tossMaxStackSize = 64;

    private static int wololoCooldownTicks = 4;
    private static boolean wololoSkipUnchanged = true;

    private static int burstMaxRadius = 4;
    private static int chainMaxBlocks = 32;
    private static int chainMaxEntities = 16;
    private static int chainMaxBlockSearchRadius = 4;
    private static double chainMaxEntitySearchRadius = 16.0D;
    private static int spellResolvesPerTick = 64;
    private static int planeMaxRadius = 4;
    private static int planeMaxPierce = 2;
    private static int fieldMaxRadius = 4;

    private static long resolveBudgetTick = Long.MIN_VALUE;
    private static int resolveBudgetUsed;

    private ArsGlyphPerfConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isCrushCacheRecipes() {
        return enabled && crushCacheRecipes;
    }

    public static int getCrushMaxAoeBlocks() {
        return crushMaxAoeBlocks;
    }

    public static boolean isCrushClaimFilter() {
        return enabled && crushClaimFilter;
    }

    public static int getOrbitResolveInterval() {
        return orbitResolveInterval;
    }

    public static int getOrbitBlockResolveInterval() {
        return orbitBlockResolveInterval;
    }

    public static int getOrbitMsptResolveInterval() {
        return orbitMsptResolveInterval;
    }

    public static double getOrbitMsptThreshold() {
        return orbitMsptThreshold;
    }

    public static int getOrbitMaxProjectiles() {
        return orbitMaxProjectiles;
    }

    public static int getOrbitMaxLifetimeTicks() {
        return orbitMaxLifetimeTicks;
    }

    public static int getOrbitMaxExtendTimes() {
        return orbitMaxExtendTimes;
    }

    public static int getTossCooldownTicks() {
        return tossCooldownTicks;
    }

    public static int getTossMaxStackSize() {
        return tossMaxStackSize;
    }

    public static int getWololoCooldownTicks() {
        return wololoCooldownTicks;
    }

    public static boolean isWololoSkipUnchanged() {
        return enabled && wololoSkipUnchanged;
    }

    public static int getBurstMaxRadius() {
        return burstMaxRadius;
    }

    public static int getChainMaxBlocks() {
        return chainMaxBlocks;
    }

    public static int getChainMaxEntities() {
        return chainMaxEntities;
    }

    public static int getChainMaxBlockSearchRadius() {
        return chainMaxBlockSearchRadius;
    }

    public static double getChainMaxEntitySearchRadius() {
        return chainMaxEntitySearchRadius;
    }

    public static int getPlaneMaxRadius() {
        return planeMaxRadius;
    }

    public static int getPlaneMaxPierce() {
        return planeMaxPierce;
    }

    public static int getFieldMaxRadius() {
        return fieldMaxRadius;
    }

    /**
     * One shared budget for child spell resolves from area glyphs.
     * The server thread keeps ticking once the budget for this tick is spent.
     */
    public static boolean tryConsumeSpellResolve(Level level) {
        if (!enabled) {
            return true;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return true;
        }
        long gameTime = serverLevel.getGameTime();
        if (gameTime != resolveBudgetTick) {
            resolveBudgetTick = gameTime;
            resolveBudgetUsed = 0;
        }
        if (resolveBudgetUsed >= spellResolvesPerTick) {
            return false;
        }
        resolveBudgetUsed++;
        return true;
    }

    /**
     * Effective Orbit resolve interval for this hit (entity vs block + MSPT soft bump).
     */
    public static int effectiveOrbitResolveInterval(boolean blockHit) {
        if (!enabled) {
            return 1;
        }
        int interval = blockHit ? orbitBlockResolveInterval : orbitResolveInterval;
        if (orbitMsptThreshold > 0.0D && PerfTickCache.isMsptAtLeast(orbitMsptThreshold)) {
            interval = Math.max(interval, orbitMsptResolveInterval);
        }
        return Math.max(1, interval);
    }

    public static boolean shouldSkipOrbitResolve(int tickCount, boolean blockHit) {
        int interval = effectiveOrbitResolveInterval(blockHit);
        if (interval <= 1) {
            return false;
        }
        return Math.floorMod(tickCount, interval) != 0;
    }

    public static int capOrbitProjectiles(int total) {
        if (!enabled) {
            return total;
        }
        return Math.min(total, orbitMaxProjectiles);
    }

    public static int capOrbitExtendTimes(int extendTimes) {
        if (!enabled) {
            return extendTimes;
        }
        return Math.min(Math.max(0, extendTimes), orbitMaxExtendTimes);
    }

    public static int capOrbitLifetime(int ticks) {
        if (!enabled) {
            return ticks;
        }
        return Math.min(ticks, orbitMaxLifetimeTicks);
    }

    public static int capTossStackSize(int size) {
        if (!enabled) {
            return size;
        }
        return Math.min(size, tossMaxStackSize);
    }

    public static Path getConfigPath() {
        return ArsPerfConfigs.path();
    }

    public static void load() {
        ArsPerfConfigs.load();
    }

    public static boolean reload() {
        return ArsPerfConfigs.reload();
    }

    static void applySection(FileData data) {
        apply(parse(data != null ? data : defaultFileData()));
    }

    static void logReload() {
        LOGGER.info(
                "Reloaded Ars glyph perf (enabled={} crushCache={} crushAoe={} orbitInterval={}/{} msptAt={}=>{} maxOrbits={} lifetime={} tossCd={} wololoCd={} burstRadius={} chainBlocks={} chainEntities={} resolvesPerTick={} planeRadius={} planePierce={} fieldRadius={})",
                enabled,
                crushCacheRecipes,
                crushMaxAoeBlocks,
                orbitResolveInterval,
                orbitBlockResolveInterval,
                orbitMsptThreshold,
                orbitMsptResolveInterval,
                orbitMaxProjectiles,
                orbitMaxLifetimeTicks,
                tossCooldownTicks,
                wololoCooldownTicks,
                burstMaxRadius,
                chainMaxBlocks,
                chainMaxEntities,
                spellResolvesPerTick,
                planeMaxRadius,
                planeMaxPierce,
                fieldMaxRadius
        );
    }

    private static void apply(LoadedConfig loaded) {
        enabled = loaded.enabled();
        crushCacheRecipes = loaded.crushCacheRecipes();
        crushMaxAoeBlocks = loaded.crushMaxAoeBlocks();
        crushClaimFilter = loaded.crushClaimFilter();
        orbitResolveInterval = loaded.orbitResolveInterval();
        orbitBlockResolveInterval = loaded.orbitBlockResolveInterval();
        orbitMsptResolveInterval = loaded.orbitMsptResolveInterval();
        orbitMsptThreshold = loaded.orbitMsptThreshold();
        orbitMaxProjectiles = loaded.orbitMaxProjectiles();
        orbitMaxLifetimeTicks = loaded.orbitMaxLifetimeTicks();
        orbitMaxExtendTimes = loaded.orbitMaxExtendTimes();
        tossCooldownTicks = loaded.tossCooldownTicks();
        tossMaxStackSize = loaded.tossMaxStackSize();
        wololoCooldownTicks = loaded.wololoCooldownTicks();
        wololoSkipUnchanged = loaded.wololoSkipUnchanged();
        burstMaxRadius = loaded.burstMaxRadius();
        chainMaxBlocks = loaded.chainMaxBlocks();
        chainMaxEntities = loaded.chainMaxEntities();
        chainMaxBlockSearchRadius = loaded.chainMaxBlockSearchRadius();
        chainMaxEntitySearchRadius = loaded.chainMaxEntitySearchRadius();
        spellResolvesPerTick = loaded.spellResolvesPerTick();
        planeMaxRadius = loaded.planeMaxRadius();
        planeMaxPierce = loaded.planeMaxPierce();
        fieldMaxRadius = loaded.fieldMaxRadius();
    }

    private static LoadedConfig parse(FileData data) {
        return new LoadedConfig(
                data.enabled == null || data.enabled,
                data.crushCacheRecipes == null || data.crushCacheRecipes,
                clamp(data.crushMaxAoeBlocks != null ? data.crushMaxAoeBlocks : 64, MIN_AOE_BLOCKS, MAX_AOE_BLOCKS),
                data.crushClaimFilter == null || data.crushClaimFilter,
                clamp(data.orbitResolveInterval != null ? data.orbitResolveInterval : 4, MIN_INTERVAL, MAX_INTERVAL),
                clamp(data.orbitBlockResolveInterval != null ? data.orbitBlockResolveInterval : 5, MIN_INTERVAL, MAX_INTERVAL),
                clamp(data.orbitMsptResolveInterval != null ? data.orbitMsptResolveInterval : 8, MIN_INTERVAL, MAX_INTERVAL),
                clamp(data.orbitMsptThreshold != null ? data.orbitMsptThreshold : 40.0D, MIN_MSPT, MAX_MSPT),
                clamp(data.orbitMaxProjectiles != null ? data.orbitMaxProjectiles : 5, MIN_PROJECTILES, MAX_PROJECTILES),
                clamp(data.orbitMaxLifetimeTicks != null ? data.orbitMaxLifetimeTicks : 2400, MIN_LIFETIME, MAX_LIFETIME),
                clamp(data.orbitMaxExtendTimes != null ? data.orbitMaxExtendTimes : 2, MIN_EXTEND, MAX_EXTEND),
                clamp(data.tossCooldownTicks != null ? data.tossCooldownTicks : 5, MIN_COOLDOWN, MAX_COOLDOWN),
                clamp(data.tossMaxStackSize != null ? data.tossMaxStackSize : 64, MIN_TOSS_STACK, MAX_TOSS_STACK),
                clamp(data.wololoCooldownTicks != null ? data.wololoCooldownTicks : 4, MIN_COOLDOWN, MAX_COOLDOWN),
                data.wololoSkipUnchanged == null || data.wololoSkipUnchanged,
                clamp(data.burstMaxRadius != null ? data.burstMaxRadius : 4, MIN_BURST_RADIUS, MAX_BURST_RADIUS),
                clamp(data.chainMaxBlocks != null ? data.chainMaxBlocks : 32, MIN_CHAIN_TARGETS, MAX_CHAIN_TARGETS),
                clamp(data.chainMaxEntities != null ? data.chainMaxEntities : 16, MIN_CHAIN_TARGETS, MAX_CHAIN_TARGETS),
                clamp(data.chainMaxBlockSearchRadius != null ? data.chainMaxBlockSearchRadius : 4, MIN_SEARCH_RADIUS, MAX_SEARCH_RADIUS),
                clamp(data.chainMaxEntitySearchRadius != null ? data.chainMaxEntitySearchRadius : 16.0D, 1.0D, 32.0D),
                clamp(data.spellResolvesPerTick != null ? data.spellResolvesPerTick : 64, MIN_RESOLVES, MAX_RESOLVES),
                clamp(data.planeMaxRadius != null ? data.planeMaxRadius : 4, MIN_PLANE_RADIUS, MAX_PLANE_RADIUS),
                clamp(data.planeMaxPierce != null ? data.planeMaxPierce : 2, MIN_PLANE_PIERCE, MAX_PLANE_PIERCE),
                clamp(data.fieldMaxRadius != null ? data.fieldMaxRadius : 4, MIN_FIELD_RADIUS, MAX_FIELD_RADIUS)
        );
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    static FileData defaultFileData() {
        FileData data = new FileData();
        data.enabled = true;
        data.crushCacheRecipes = true;
        data.crushMaxAoeBlocks = 64;
        data.crushClaimFilter = true;
        data.orbitResolveInterval = 4;
        data.orbitBlockResolveInterval = 5;
        data.orbitMsptResolveInterval = 8;
        data.orbitMsptThreshold = 40.0D;
        data.orbitMaxProjectiles = 5;
        data.orbitMaxLifetimeTicks = 2400;
        data.orbitMaxExtendTimes = 2;
        data.tossCooldownTicks = 5;
        data.tossMaxStackSize = 64;
        data.wololoCooldownTicks = 4;
        data.wololoSkipUnchanged = true;
        data.burstMaxRadius = 4;
        data.chainMaxBlocks = 32;
        data.chainMaxEntities = 16;
        data.chainMaxBlockSearchRadius = 4;
        data.chainMaxEntitySearchRadius = 16.0D;
        data.spellResolvesPerTick = 64;
        data.planeMaxRadius = 4;
        data.planeMaxPierce = 2;
        data.fieldMaxRadius = 4;
        return data;
    }

    private record LoadedConfig(
            boolean enabled,
            boolean crushCacheRecipes,
            int crushMaxAoeBlocks,
            boolean crushClaimFilter,
            int orbitResolveInterval,
            int orbitBlockResolveInterval,
            int orbitMsptResolveInterval,
            double orbitMsptThreshold,
            int orbitMaxProjectiles,
            int orbitMaxLifetimeTicks,
            int orbitMaxExtendTimes,
            int tossCooldownTicks,
            int tossMaxStackSize,
            int wololoCooldownTicks,
            boolean wololoSkipUnchanged,
            int burstMaxRadius,
            int chainMaxBlocks,
            int chainMaxEntities,
            int chainMaxBlockSearchRadius,
            double chainMaxEntitySearchRadius,
            int spellResolvesPerTick,
            int planeMaxRadius,
            int planeMaxPierce,
            int fieldMaxRadius
    ) {
    }

    static final class FileData {
        @SerializedName("enabled")
        Boolean enabled;
        @SerializedName("crushCacheRecipes")
        private Boolean crushCacheRecipes;
        @SerializedName("crushMaxAoeBlocks")
        private Integer crushMaxAoeBlocks;
        @SerializedName("crushClaimFilter")
        private Boolean crushClaimFilter;
        @SerializedName("orbitResolveInterval")
        private Integer orbitResolveInterval;
        @SerializedName("orbitBlockResolveInterval")
        private Integer orbitBlockResolveInterval;
        @SerializedName("orbitMsptResolveInterval")
        private Integer orbitMsptResolveInterval;
        @SerializedName("orbitMsptThreshold")
        private Double orbitMsptThreshold;
        @SerializedName("orbitMaxProjectiles")
        private Integer orbitMaxProjectiles;
        @SerializedName("orbitMaxLifetimeTicks")
        private Integer orbitMaxLifetimeTicks;
        @SerializedName("orbitMaxExtendTimes")
        private Integer orbitMaxExtendTimes;
        @SerializedName("tossCooldownTicks")
        private Integer tossCooldownTicks;
        @SerializedName("tossMaxStackSize")
        private Integer tossMaxStackSize;
        @SerializedName("wololoCooldownTicks")
        private Integer wololoCooldownTicks;
        @SerializedName("wololoSkipUnchanged")
        private Boolean wololoSkipUnchanged;
        @SerializedName("burstMaxRadius")
        private Integer burstMaxRadius;
        @SerializedName("chainMaxBlocks")
        private Integer chainMaxBlocks;
        @SerializedName("chainMaxEntities")
        private Integer chainMaxEntities;
        @SerializedName("chainMaxBlockSearchRadius")
        private Integer chainMaxBlockSearchRadius;
        @SerializedName("chainMaxEntitySearchRadius")
        private Double chainMaxEntitySearchRadius;
        @SerializedName("spellResolvesPerTick")
        private Integer spellResolvesPerTick;
        @SerializedName("planeMaxRadius")
        private Integer planeMaxRadius;
        @SerializedName("planeMaxPierce")
        private Integer planeMaxPierce;
        @SerializedName("fieldMaxRadius")
        private Integer fieldMaxRadius;
    }
}
