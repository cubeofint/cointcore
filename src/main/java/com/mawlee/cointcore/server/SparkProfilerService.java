package com.mawlee.cointcore.server;

import com.mawlee.cointcore.config.SparkProfilerConfig;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class SparkProfilerService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SPARK_MOD_ID = "spark";
    private static final DateTimeFormatter COMMENT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private static boolean profiling;
    private static long profilingStartedAtMs;
    private static long lastSaveAtMs;
    private static long belowClearThresholdSinceMs;

    private SparkProfilerService() {
    }

    public static void tick(MinecraftServer server) {
        if (!SparkProfilerConfig.isEnabled() || !isSparkAvailable()) {
            return;
        }

        double mspt = readMspt(server);

        if (profiling) {
            handleActiveProfiler(server, mspt);
            return;
        }

        if (mspt >= SparkProfilerConfig.getMsptThreshold()) {
            startProfiler(server, mspt);
        }
    }

    public static void resetRuntimeState() {
        profiling = false;
        profilingStartedAtMs = 0L;
        lastSaveAtMs = 0L;
        belowClearThresholdSinceMs = 0L;
    }

    private static void handleActiveProfiler(MinecraftServer server, double mspt) {
        long now = System.currentTimeMillis();
        long saveIntervalMs = SparkProfilerConfig.getSaveIntervalMinutes() * 60_000L;

        if (now - lastSaveAtMs >= saveIntervalMs) {
            saveProfiler(server, mspt);
            if (mspt >= SparkProfilerConfig.getMsptThreshold()) {
                startProfiler(server, mspt);
            } else {
                stopProfiler(server, false);
            }
            return;
        }

        if (mspt < SparkProfilerConfig.getMsptClearThreshold()) {
            if (belowClearThresholdSinceMs == 0L) {
                belowClearThresholdSinceMs = now;
            } else if (now - belowClearThresholdSinceMs >= SparkProfilerConfig.getClearDelaySeconds() * 1000L) {
                cancelProfiler(server, mspt);
            }
            return;
        }

        belowClearThresholdSinceMs = 0L;
    }

    private static void startProfiler(MinecraftServer server, double mspt) {
        if (profiling) {
            return;
        }

        String startCommand = buildStartCommand();
        if (!runSparkCommand(server, startCommand)) {
            return;
        }

        long now = System.currentTimeMillis();
        profiling = true;
        profilingStartedAtMs = now;
        lastSaveAtMs = now;
        belowClearThresholdSinceMs = 0L;
        LOGGER.info(
                "Started Spark profiler automatically (command={}, MSPT={} ms, threshold={} ms)",
                startCommand,
                formatMspt(mspt),
                SparkProfilerConfig.getMsptThreshold()
        );
    }

    private static String buildStartCommand() {
        int onlyTicksOverMs = SparkProfilerConfig.getOnlyTicksOverMs();
        if (onlyTicksOverMs <= 0) {
            return "spark profiler start";
        }
        return "spark profiler start --only-ticks-over " + onlyTicksOverMs;
    }

    private static void saveProfiler(MinecraftServer server, double mspt) {
        String comment = buildSaveComment(mspt);
        String command = "spark profiler stop --save-to-file --comment \"" + escapeComment(comment) + "\"";
        if (!runSparkCommand(server, command)) {
            LOGGER.warn("Failed to save Spark profiler snapshot (MSPT={} ms)", formatMspt(mspt));
            return;
        }

        lastSaveAtMs = System.currentTimeMillis();
        profiling = false;
        LOGGER.info("Saved Spark profiler snapshot to file (MSPT={} ms, comment={})", formatMspt(mspt), comment);
    }

    private static void cancelProfiler(MinecraftServer server, double mspt) {
        runSparkCommand(server, "spark profiler cancel");
        stopProfiler(server, true);
        LOGGER.info("Cancelled Spark profiler after MSPT recovered (MSPT={} ms, clearThreshold={} ms)", formatMspt(mspt), SparkProfilerConfig.getMsptClearThreshold());
    }

    private static void stopProfiler(MinecraftServer server, boolean logRecovery) {
        profiling = false;
        profilingStartedAtMs = 0L;
        belowClearThresholdSinceMs = 0L;

        if (logRecovery) {
            return;
        }

        LOGGER.info("Spark profiler stopped without periodic save because MSPT dropped below start threshold");
    }

    private static String buildSaveComment(double mspt) {
        String prefix = SparkProfilerConfig.getCommentPrefix();
        return prefix + " mspt=" + formatMspt(mspt) + " at=" + LocalDateTime.now().format(COMMENT_TIME);
    }

    private static String escapeComment(String comment) {
        return comment.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static boolean runSparkCommand(MinecraftServer server, String command) {
        CommandSourceStack source = server.createCommandSourceStack()
                .withPermission(4)
                .withSuppressedOutput();

        try {
            server.getCommands().getDispatcher().execute(command, source);
            return true;
        } catch (CommandSyntaxException exception) {
            LOGGER.warn("Failed to execute Spark command '{}'", command, exception);
            return false;
        }
    }

    private static double readMspt(MinecraftServer server) {
        return server.getAverageTickTimeNanos() / 1_000_000.0D;
    }

    private static String formatMspt(double mspt) {
        return String.format("%.1f", mspt);
    }

    private static boolean isSparkAvailable() {
        return ModList.get().isLoaded(SPARK_MOD_ID);
    }
}
