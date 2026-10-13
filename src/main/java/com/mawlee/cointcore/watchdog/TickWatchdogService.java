package com.mawlee.cointcore.watchdog;

import com.mawlee.cointcore.config.TickWatchdogConfig;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mawlee.cointcore.watchdog.probes.WatchdogProbeConfig;
import com.mawlee.cointcore.watchdog.probes.WatchdogProbeHub;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Server-thread watchdog: tick stats, BE/entity attribution, reports, admin alerts.
 */
public final class TickWatchdogService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final DateTimeFormatter FILE_DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneOffset.UTC);

    private static final TickWatchdogService INSTANCE = new TickWatchdogService();

    private final OffenderAggregator aggregator = new OffenderAggregator();
    private final StackSampler sampler = new StackSampler();
    private TickStatsWindow stats = new TickStatsWindow(1200, TickWatchdogConfig.getSlowTickThresholdNanos());
    private long lastReportAtMs;
    private long lastNotifyAtMs;
    private long lastSustainedAverageAtMs;
    private int consecutiveSlow;
    private int holdRemaining;
    private int holdCooldown;
    private int tickIndex;
    private boolean holdJustStarted;
    private volatile boolean running;
    private List<OffenderAggregator.OffenderSnapshot> lastTop = List.of();

    private TickWatchdogService() {
    }

    public static TickWatchdogService instance() {
        return INSTANCE;
    }

    public static boolean isTimingEnabled() {
        return WatchdogHotFlags.TIME_BLOCK_ENTITIES || WatchdogHotFlags.TIME_ENTITIES;
    }

    public void start(MinecraftServer server) {
        running = TickWatchdogConfig.isEnabled();
        stats = new TickStatsWindow(
                Math.max(200, TickWatchdogConfig.getWindowSeconds() * 20),
                TickWatchdogConfig.getSlowTickThresholdNanos()
        );
        aggregator.clear();
        aggregator.setLabels(WatchdogWorldContext.LABELS);
        TickProbe.setLabels(WatchdogWorldContext.LABELS);
        sampler.resetAggregator();
        lastTop = List.of();
        consecutiveSlow = 0;
        holdRemaining = 0;
        holdCooldown = 0;
        tickIndex = 0;
        holdJustStarted = false;
        WatchdogHotFlags.RUNNING = running;
        WatchdogHotFlags.setTiming(false, false);
        WatchdogProbeConfig.load();
        if (running) {
            WatchdogProbeHub.instance().start();
            sampler.start();
            Thread indexer = new Thread(WatchdogModIndex::rebuild, "cointcore-watchdog-modindex");
            indexer.setDaemon(true);
            indexer.start();
            LOGGER.info(
                    "Tick watchdog started (threshold={} ms, mode={}, sampleEvery={}, hold={})",
                    TickWatchdogConfig.getSlowTickThresholdMs(),
                    TickWatchdogConfig.getDetailedTimingMode(),
                    TickWatchdogConfig.getSampleEveryNTicks(),
                    TickWatchdogConfig.getDetailedHoldTicks()
            );
        }
    }

    public void stop() {
        running = false;
        WatchdogHotFlags.clear();
        sampler.stop();
        WatchdogProbeHub.instance().stop();
        TickProbe.clear();
    }

    public void setEnabled(boolean enabled) {
        running = enabled;
        WatchdogHotFlags.RUNNING = enabled;
        if (running) {
            WatchdogProbeHub.instance().start();
            sampler.start();
        } else {
            WatchdogHotFlags.setTiming(false, false);
            sampler.stop();
            WatchdogProbeHub.instance().stop();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void onServerTickStart() {
        if (!isRunning()) {
            WatchdogHotFlags.RUNNING = false;
            WatchdogHotFlags.setTiming(false, false);
            return;
        }
        tickIndex++;
        boolean armed = WatchdogSampling.isArmed(
                TickWatchdogConfig.isDetailedAlways(),
                TickWatchdogConfig.isDetailedOff(),
                holdRemaining
        );
        boolean sample = WatchdogSampling.shouldSampleDetails(
                armed,
                holdJustStarted,
                tickIndex,
                TickWatchdogConfig.getSampleEveryNTicks()
        );
        holdJustStarted = false;
        if (holdRemaining > 0) {
            holdRemaining--;
            if (holdRemaining == 0) {
                holdCooldown = TickWatchdogConfig.getSampleEveryNTicks();
            }
        } else if (holdCooldown > 0) {
            holdCooldown--;
        }
        WatchdogHotFlags.setTiming(
                sample && TickWatchdogConfig.isAttributeBlockEntities(),
                sample && TickWatchdogConfig.isAttributeEntities()
        );
        WatchdogProbeHub.instance().onTickStart(Thread.currentThread());
    }

    public void onServerTickEnd(MinecraftServer server, long tickNanos) {
        if (!isRunning()) {
            return;
        }
        stats.record(tickNanos);
        boolean slow = tickNanos >= TickWatchdogConfig.getSlowTickThresholdNanos();
        WatchdogProbeHub.instance().onTickEnd(server, tickNanos, slow);
        if (slow) {
            consecutiveSlow++;
            if (consecutiveSlow >= TickWatchdogConfig.getAutoDetailedAfterSlowTicks()
                    && !TickWatchdogConfig.isDetailedOff()
                    && !TickWatchdogConfig.isDetailedAlways()
                    && holdRemaining <= 0
                    && holdCooldown <= 0) {
                holdRemaining = TickWatchdogConfig.getDetailedHoldTicks();
                holdJustStarted = true;
            }
        } else {
            consecutiveSlow = 0;
        }

        long now = System.currentTimeMillis();
        if (now - lastReportAtMs >= TickWatchdogConfig.getReportIntervalSeconds() * 1000L) {
            writeReport(server);
            lastReportAtMs = now;
        }
        if (WatchdogTriggers.consecutiveSlowTicks(
                slow,
                consecutiveSlow,
                TickWatchdogConfig.getSustainedSlowTicks()
        )) {
            warnSustainedLag(server, tickNanos);
        }
        int averageLookback = TickWatchdogConfig.getSustainedAverageMinTicks();
        double recentAverage = stats.recentAverageMillis(averageLookback);
        if (WatchdogTriggers.sustainedAverage(
                recentAverage,
                TickWatchdogConfig.getSustainedAverageMs(),
                Math.min(stats.size(), averageLookback),
                averageLookback
        )) {
            warnSustainedAverage(server, recentAverage);
        }
    }

    public void recordBlockEntity(Object type, Object dimension, int x, int y, int z, long nanos) {
        if (!isRunning() || !TickWatchdogConfig.isAttributeBlockEntities()) {
            return;
        }
        aggregator.addBlockEntity(type, dimension, x, y, z, nanos);
    }

    public void recordEntity(Object type, Object dimension, int x, int y, int z, long nanos) {
        if (!isRunning() || !TickWatchdogConfig.isAttributeEntities()) {
            return;
        }
        aggregator.addEntity(type, dimension, x, y, z, nanos);
    }

    public List<OffenderAggregator.OffenderSnapshot> currentTop(int limit) {
        List<OffenderAggregator.OffenderSnapshot> combined = new ArrayList<>();
        combined.addAll(aggregator.top(OffenderAggregator.Kind.BLOCK_ENTITY, limit));
        combined.addAll(aggregator.top(OffenderAggregator.Kind.ENTITY, limit));
        combined.sort((a, b) -> Long.compare(b.totalNanos(), a.totalNanos()));
        if (combined.size() > limit) {
            combined = combined.subList(0, limit);
        }
        lastTop = List.copyOf(combined);
        return lastTop;
    }

    public List<OffenderAggregator.OffenderSnapshot> lastTop() {
        if (lastTop.isEmpty()) {
            return currentTop(TickWatchdogConfig.getTopEntries());
        }
        return lastTop;
    }

    public TickStatsWindow stats() {
        return stats;
    }

    public StackSampler sampler() {
        return sampler;
    }

    public String writeReportNow(MinecraftServer server) {
        WatchdogProbeHub.instance().analyzeNow(server);
        return writeReport(server);
    }

    public String probeStatusText() {
        return WatchdogProbeHub.instance().statusText();
    }

    private String writeReport(MinecraftServer server) {
        return writeReport(server, List.of());
    }

    private String writeReport(MinecraftServer server, List<WatchdogReportFormatter.ExtraSection> extras) {
        int top = TickWatchdogConfig.getTopEntries();
        List<OffenderAggregator.OffenderSnapshot> be = aggregator.top(OffenderAggregator.Kind.BLOCK_ENTITY, top);
        List<OffenderAggregator.OffenderSnapshot> entities = aggregator.top(OffenderAggregator.Kind.ENTITY, top);
        List<OffenderAggregator.OffenderSnapshot> chunks = aggregator.top(OffenderAggregator.Kind.CHUNK, top);
        List<OffenderAggregator.OffenderSnapshot> dims = aggregator.top(OffenderAggregator.Kind.DIMENSION, top);
        lastTop = new ArrayList<>();
        lastTop.addAll(be);
        lastTop.addAll(entities);
        lastTop.sort((a, b) -> Long.compare(b.totalNanos(), a.totalNanos()));
        if (lastTop.size() > top) {
            lastTop = List.copyOf(lastTop.subList(0, top));
        } else {
            lastTop = List.copyOf(lastTop);
        }
        List<StackSampleAggregator.MethodStat> methods = sampler.aggregator().topBySelf(top);

        String text = WatchdogReportFormatter.format(
                Instant.now(),
                stats,
                be,
                entities,
                chunks,
                dims,
                methods,
                sampler.sampleCount(),
                snapshot -> lookupChunk(server, snapshot),
                extras
        );
        String probeText = WatchdogProbeHub.instance().formatReport(server);
        if (probeText != null && !probeText.isBlank()) {
            text = text + System.lineSeparator() + probeText;
        }

        Path dir = WatchdogRetention.watchdogReportDir(server);
        Path file = dir.resolve(FILE_DAY.format(Instant.now()) + ".log");
        try {
            Files.createDirectories(dir);
            Files.writeString(
                    file,
                    text + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (Exception exception) {
            LOGGER.warn("Failed to write tick watchdog report {}", file, exception);
        }
        aggregator.clear();
        stats.resetSlowTicks();
        sampler.resetAggregator();
        WatchdogProbeHub.instance().resetWindows();
        return text;
    }

    private WatchdogReportFormatter.ChunkContext lookupChunk(
            MinecraftServer server,
            OffenderAggregator.OffenderSnapshot snapshot
    ) {
        if (server == null || snapshot == null) {
            return new WatchdogReportFormatter.ChunkContext("-", "unknown");
        }
        ServerLevel level = findLevel(server, snapshot.dimension());
        return WatchdogWorldContext.describeChunk(level, snapshot.chunkX(), snapshot.chunkZ());
    }

    private static ServerLevel findLevel(MinecraftServer server, String dimension) {
        for (ServerLevel level : server.getAllLevels()) {
            if (WatchdogWorldContext.dimensionId(level).equals(dimension)) {
                return level;
            }
        }
        return null;
    }

    private void warnSustainedLag(MinecraftServer server, long tickNanos) {
        long now = System.currentTimeMillis();
        if (now - lastNotifyAtMs < TickWatchdogConfig.getNotifyCooldownSeconds() * 1000L) {
            return;
        }
        lastNotifyAtMs = now;
        List<OffenderAggregator.OffenderSnapshot> top = currentTop(3);
        String culprits = WatchdogReportFormatter.conciseTopLine(top, 3);
        double ms = tickNanos / 1_000_000.0D;
        String line = String.format(
                Locale.ROOT,
                "CointCore watchdog: sustained lag (tick=%.1f ms, TPS≈%.2f). Top: %s",
                ms,
                stats.estimatedTps(),
                culprits
        );
        LOGGER.warn(line);
        if (!TickWatchdogConfig.isNotifyAdmins()) {
            return;
        }
        Component message = Component.literal("§c" + line);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (PermissionService.has(player, CointPermissionNodes.WATCHDOG)) {
                player.sendSystemMessage(message);
            }
        }
    }

    private void warnSustainedAverage(MinecraftServer server, double averageMs) {
        long now = System.currentTimeMillis();
        if (now - lastSustainedAverageAtMs < TickWatchdogConfig.getNotifyCooldownSeconds() * 1000L) {
            return;
        }
        lastSustainedAverageAtMs = now;
        List<WatchdogReportFormatter.ExtraSection> extras = attributionExtras(server);
        String text = writeReport(server, extras);
        LOGGER.warn(String.format(
                Locale.ROOT,
                "CointCore watchdog: sustained average MSPT %.2f > %.1f over %ds. Queue + handler attribution attached.",
                averageMs,
                TickWatchdogConfig.getSustainedAverageMs(),
                TickWatchdogConfig.getSustainedAverageWindowSeconds()
        ));
        LOGGER.warn(text);
    }

    private List<WatchdogReportFormatter.ExtraSection> attributionExtras(MinecraftServer server) {
        List<WatchdogReportFormatter.ExtraSection> extras = new ArrayList<>();
        int queue = ServerTaskQueueProbe.pendingCount(server);
        extras.add(new WatchdogReportFormatter.ExtraSection(
                "Очередь server.execute / runnables",
                List.of(queue < 0
                        ? "очередь недоступна (нет поля pending*)"
                        : "pending=" + queue)
        ));

        List<MainThreadAttribution.Frame> frames = MainThreadAttribution.topFrames(
                Thread.currentThread().getStackTrace(),
                WatchdogModIndex.mapper(),
                16
        );
        List<MainThreadAttribution.PackageHit> packages = MainThreadAttribution.byPackage(frames, 8);
        List<String> handlerLines = new ArrayList<>();
        if (packages.isEmpty()) {
            handlerLines.add("нет кадров приложения в текущем стеке");
        } else {
            int rank = 1;
            for (MainThreadAttribution.PackageHit hit : packages) {
                handlerLines.add(String.format(
                        Locale.ROOT,
                        "%d) мод=%s пакет=%s кадров=%d%s",
                        rank++,
                        hit.modId(),
                        hit.packageName(),
                        hit.frames(),
                        hit.eventLike() ? " [event/handler]" : ""
                ));
            }
        }
        extras.add(new WatchdogReportFormatter.ExtraSection(
                "Стек server thread по моду/пакету",
                handlerLines
        ));

        List<String> frameLines = new ArrayList<>();
        int rank = 1;
        for (MainThreadAttribution.Frame frame : frames) {
            frameLines.add(String.format(
                    Locale.ROOT,
                    "%d) %s#%s мод=%s%s",
                    rank++,
                    shortClass(frame.className()),
                    frame.methodName(),
                    frame.modId(),
                    frame.eventLike() ? " [event/handler]" : ""
            ));
            if (rank > 8) {
                break;
            }
        }
        extras.add(new WatchdogReportFormatter.ExtraSection("Верх стека server thread", frameLines));
        return extras;
    }

    private static String shortClass(String className) {
        if (className == null) {
            return "?";
        }
        int dot = className.lastIndexOf('.');
        return dot >= 0 ? className.substring(dot + 1) : className;
    }
}
