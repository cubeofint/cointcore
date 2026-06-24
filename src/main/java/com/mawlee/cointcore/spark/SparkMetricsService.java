package com.mawlee.cointcore.spark;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.OptionalDouble;

/**
 * Reads rolling TPS/MSPT from Spark when present, otherwise falls back to vanilla tick timing.
 */
public final class SparkMetricsService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SPARK_MOD_ID = "spark";

    private static SparkBinding sparkBinding;
    private static MetricsSnapshot snapshot = MetricsSnapshot.unavailable();
    private static int sparkBindRetriesRemaining;

    private SparkMetricsService() {
    }

    public static void bind() {
        if (!ModList.get().isLoaded(SPARK_MOD_ID)) {
            sparkBinding = null;
            sparkBindRetriesRemaining = 0;
            LOGGER.info("Spark is not loaded; cointcore placeholders will use vanilla tick timing");
            return;
        }

        sparkBindRetriesRemaining = 120;
        tryBindOnce(false);
    }

    private static void tryBindOnce(boolean silent) {
        try {
            SparkBinding loaded = SparkBinding.load();
            if (loaded == null) {
                if (!silent) {
                    LOGGER.info("Spark TPS/MSPT statistics are not ready yet; using vanilla tick timing until Spark is available");
                }
                return;
            }

            sparkBinding = loaded;
            sparkBindRetriesRemaining = 0;
            LOGGER.info("Bound CointCore metrics reader to Spark API");
        } catch (Exception exception) {
            sparkBinding = null;
            sparkBindRetriesRemaining = 0;
            LOGGER.warn("Failed to bind Spark metrics API; placeholders will use vanilla tick timing", exception);
        }
    }

    public static void tick(MinecraftServer server) {
        if (sparkBinding == null && sparkBindRetriesRemaining > 0 && ModList.get().isLoaded(SPARK_MOD_ID)) {
            sparkBindRetriesRemaining--;
            tryBindOnce(true);
        }

        if (sparkBinding != null) {
            try {
                snapshot = sparkBinding.read();
                return;
            } catch (ReflectiveOperationException exception) {
                LOGGER.debug("Spark metrics read failed, falling back to vanilla timing", exception);
            }
        }

        snapshot = MetricsSnapshot.fromVanilla(server);
    }

    public static void resetRuntimeState() {
        snapshot = MetricsSnapshot.unavailable();
        sparkBinding = null;
        sparkBindRetriesRemaining = 0;
    }

    public static MetricsSnapshot snapshot() {
        return snapshot;
    }

    public static OptionalDouble resolveTps(String window) {
        return snapshot.tps(window);
    }

    public static OptionalDouble resolveMsptMean(String window) {
        return snapshot.msptMean(window);
    }

    public static OptionalDouble resolveMsptMax(String window) {
        return snapshot.msptMax(window);
    }

    public static OptionalDouble resolveMsptPercentile95(String window) {
        return snapshot.msptPercentile95(window);
    }

    public static String formatTps(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    public static String formatMspt(double value) {
        return String.format(Locale.ROOT, "%.0f", value);
    }

    public static String coloredTps(double value) {
        String formatted = formatTps(value);
        if (value >= 19.5D) {
            return "&a" + formatted;
        }
        if (value >= 18.0D) {
            return "&e" + formatted;
        }
        if (value >= 15.0D) {
            return "&6" + formatted;
        }
        return "&c" + formatted;
    }

    public record MetricsSnapshot(
            boolean fromSpark,
            double tps5s,
            double tps10s,
            double tps1m,
            double tps5m,
            double tps15m,
            double msptMean10s,
            double msptMean1m,
            double msptMean5m,
            double msptMax1m,
            double msptP95_1m,
            double msptMax5m,
            double msptP95_5m
    ) {
        static MetricsSnapshot unavailable() {
            return new MetricsSnapshot(false, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        static MetricsSnapshot fromVanilla(MinecraftServer server) {
            double mspt = server.getAverageTickTimeNanos() / 1_000_000.0D;
            double tps = mspt <= 0.0D ? 20.0D : Math.min(20.0D, 1000.0D / mspt);
            return new MetricsSnapshot(
                    false,
                    tps,
                    tps,
                    tps,
                    tps,
                    tps,
                    mspt,
                    mspt,
                    mspt,
                    mspt,
                    mspt,
                    mspt,
                    mspt
            );
        }

        public OptionalDouble tps(String window) {
            return switch (normalizeWindow(window, "1m")) {
                case "5s" -> optional(tps5s);
                case "10s" -> optional(tps10s);
                case "1m" -> optional(tps1m);
                case "5m" -> optional(tps5m);
                case "15m" -> optional(tps15m);
                default -> OptionalDouble.empty();
            };
        }

        public OptionalDouble msptMean(String window) {
            return switch (normalizeWindow(window, "1m")) {
                case "10s" -> optional(msptMean10s);
                case "1m" -> optional(msptMean1m);
                case "5m" -> optional(msptMean5m);
                default -> OptionalDouble.empty();
            };
        }

        public OptionalDouble msptMax(String window) {
            return switch (normalizeWindow(window, "1m")) {
                case "1m" -> optional(msptMax1m);
                case "5m" -> optional(msptMax5m);
                default -> OptionalDouble.empty();
            };
        }

        public OptionalDouble msptPercentile95(String window) {
            return switch (normalizeWindow(window, "1m")) {
                case "1m" -> optional(msptP95_1m);
                case "5m" -> optional(msptP95_5m);
                default -> OptionalDouble.empty();
            };
        }

        private static OptionalDouble optional(double value) {
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                return OptionalDouble.empty();
            }
            return OptionalDouble.of(value);
        }

        private static String normalizeWindow(String window, String fallback) {
            if (window == null || window.isBlank()) {
                return fallback;
            }
            return window.trim().toLowerCase(Locale.ROOT);
        }
    }

    private static final class SparkBinding {
        private final Object tpsStatistic;
        private final Object msptStatistic;
        private final Method tpsPoll;
        private final Method msptPoll;
        private final Object tps5s;
        private final Object tps10s;
        private final Object tps1m;
        private final Object tps5m;
        private final Object tps15m;
        private final Object mspt10s;
        private final Object mspt1m;
        private final Object mspt5m;

        private SparkBinding(
                Object tpsStatistic,
                Object msptStatistic,
                Method tpsPoll,
                Method msptPoll,
                Object tps5s,
                Object tps10s,
                Object tps1m,
                Object tps5m,
                Object tps15m,
                Object mspt10s,
                Object mspt1m,
                Object mspt5m
        ) {
            this.tpsStatistic = tpsStatistic;
            this.msptStatistic = msptStatistic;
            this.tpsPoll = tpsPoll;
            this.msptPoll = msptPoll;
            this.tps5s = tps5s;
            this.tps10s = tps10s;
            this.tps1m = tps1m;
            this.tps5m = tps5m;
            this.tps15m = tps15m;
            this.mspt10s = mspt10s;
            this.mspt1m = mspt1m;
            this.mspt5m = mspt5m;
        }

        static SparkBinding load() throws ReflectiveOperationException {
            Class<?> providerClass = Class.forName("me.lucko.spark.api.SparkProvider");
            Object spark = providerClass.getMethod("get").invoke(null);

            Object tpsStatistic = spark.getClass().getMethod("tps").invoke(spark);
            Object msptStatistic = spark.getClass().getMethod("mspt").invoke(spark);
            if (tpsStatistic == null || msptStatistic == null) {
                return null;
            }

            Class<?> tpsWindowClass = Class.forName("me.lucko.spark.api.statistic.StatisticWindow$TicksPerSecond");
            Class<?> msptWindowClass = Class.forName("me.lucko.spark.api.statistic.StatisticWindow$MillisPerTick");

            Method tpsPoll = tpsStatistic.getClass().getMethod("poll", tpsWindowClass);
            Method msptPoll = msptStatistic.getClass().getMethod("poll", msptWindowClass);

            return new SparkBinding(
                    tpsStatistic,
                    msptStatistic,
                    tpsPoll,
                    msptPoll,
                    enumConstant(tpsWindowClass, "SECONDS_5"),
                    enumConstant(tpsWindowClass, "SECONDS_10"),
                    enumConstant(tpsWindowClass, "MINUTES_1"),
                    enumConstant(tpsWindowClass, "MINUTES_5"),
                    enumConstant(tpsWindowClass, "MINUTES_15"),
                    enumConstant(msptWindowClass, "SECONDS_10"),
                    enumConstant(msptWindowClass, "MINUTES_1"),
                    enumConstant(msptWindowClass, "MINUTES_5")
            );
        }

        MetricsSnapshot read() throws ReflectiveOperationException {
            return new MetricsSnapshot(
                    true,
                    pollTps(tps5s),
                    pollTps(tps10s),
                    pollTps(tps1m),
                    pollTps(tps5m),
                    pollTps(tps15m),
                    pollMsptMean(mspt10s),
                    pollMsptMean(mspt1m),
                    pollMsptMean(mspt5m),
                    pollMsptMax(mspt1m),
                    pollMsptPercentile95(mspt1m),
                    pollMsptMax(mspt5m),
                    pollMsptPercentile95(mspt5m)
            );
        }

        private double pollTps(Object window) throws ReflectiveOperationException {
            Object value = tpsPoll.invoke(tpsStatistic, window);
            return ((Number) value).doubleValue();
        }

        private double pollMsptMean(Object window) throws ReflectiveOperationException {
            Object average = msptPoll.invoke(msptStatistic, window);
            Object mean = average.getClass().getMethod("mean").invoke(average);
            return ((Number) mean).doubleValue();
        }

        private double pollMsptMax(Object window) throws ReflectiveOperationException {
            Object average = msptPoll.invoke(msptStatistic, window);
            Object max = average.getClass().getMethod("max").invoke(average);
            return ((Number) max).doubleValue();
        }

        private double pollMsptPercentile95(Object window) throws ReflectiveOperationException {
            Object average = msptPoll.invoke(msptStatistic, window);
            Object percentile = average.getClass().getMethod("percentile95th").invoke(average);
            return ((Number) percentile).doubleValue();
        }

        private static Object enumConstant(Class<?> enumClass, String name) {
            Object[] constants = enumClass.getEnumConstants();
            for (Object constant : constants) {
                if (constant.toString().equals(name) || ((Enum<?>) constant).name().equals(name)) {
                    return constant;
                }
            }
            throw new IllegalArgumentException("Missing enum constant " + name + " in " + enumClass.getName());
        }
    }
}
