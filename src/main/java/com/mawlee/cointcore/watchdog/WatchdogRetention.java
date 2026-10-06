package com.mawlee.cointcore.watchdog;

import com.mawlee.cointcore.config.SparkProfilerConfig;
import com.mawlee.cointcore.config.TickWatchdogConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

public final class WatchdogRetention {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static long lastSparkApplyMs;
    private static long lastWatchdogApplyMs;

    private WatchdogRetention() {
    }

    public static void applySparkProfiles(MinecraftServer server) {
        long now = System.currentTimeMillis();
        if (now - lastSparkApplyMs < 60_000L) {
            return;
        }
        lastSparkApplyMs = now;
        Path dir = sparkProfileDir(server);
        apply(
                dir,
                SparkProfilerConfig.getMaxProfileAgeDays(),
                SparkProfilerConfig.getMaxProfileTotalSizeBytes(),
                path -> {
                    String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                    return name.endsWith(".spark")
                            || name.endsWith(".sparkprofile")
                            || name.endsWith(".json.gz")
                            || name.endsWith(".txt")
                            || name.endsWith(".log");
                },
                "spark profiles"
        );
    }

    public static void applyWatchdogReports(MinecraftServer server) {
        long now = System.currentTimeMillis();
        if (now - lastWatchdogApplyMs < 60_000L) {
            return;
        }
        lastWatchdogApplyMs = now;
        Path dir = watchdogReportDir(server);
        apply(
                dir,
                TickWatchdogConfig.getMaxReportAgeDays(),
                TickWatchdogConfig.getMaxReportTotalSizeBytes(),
                path -> {
                    String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                    return name.endsWith(".log") || name.endsWith(".txt");
                },
                "watchdog reports"
        );
    }

    public static Path sparkProfileDir(MinecraftServer server) {
        Path root = server.getServerDirectory();
        Path spark = root.resolve("spark");
        if (Files.isDirectory(spark)) {
            return spark;
        }
        Path nested = root.resolve("config").resolve("spark");
        if (Files.isDirectory(nested)) {
            return nested;
        }
        return spark;
    }

    public static Path watchdogReportDir(MinecraftServer server) {
        return server.getServerDirectory().resolve("logs").resolve("cointcore-watchdog");
    }

    public static RetentionPolicy.Result applyForTest(
            Path directory,
            int maxAgeDays,
            long maxBytes
    ) throws IOException {
        Duration age = maxAgeDays <= 0 ? Duration.ZERO : Duration.ofDays(maxAgeDays);
        return RetentionPolicy.apply(directory, Instant.now(), age, maxBytes, null);
    }

    private static void apply(
            Path dir,
            int maxAgeDays,
            long maxBytes,
            java.util.function.Predicate<Path> filter,
            String label
    ) {
        try {
            Duration age = maxAgeDays <= 0 ? Duration.ZERO : Duration.ofDays(maxAgeDays);
            RetentionPolicy.Result result = RetentionPolicy.apply(dir, Instant.now(), age, maxBytes, filter);
            if (result.deletedFiles() > 0) {
                LOGGER.info(
                        "CointCore retention ({}) deleted {} files ({} bytes), remaining {} files / {} bytes",
                        label,
                        result.deletedFiles(),
                        result.deletedBytes(),
                        result.remainingFiles(),
                        result.remainingBytes()
                );
            }
        } catch (IOException exception) {
            LOGGER.warn("CointCore retention failed for {}", label, exception);
        }
    }
}
