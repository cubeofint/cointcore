package com.mawlee.cointcore.watchdog;

import com.mawlee.cointcore.config.TickWatchdogConfig;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Server-thread watchdog: tick stats, BE/entity attribution, reports, admin alerts.
 */
public final class TickWatchdogService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final DateTimeFormatter FILE_DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneOffset.UTC);
    private static final AtomicBoolean TIMING_ENABLED = new AtomicBoolean();

    private static final TickWatchdogService INSTANCE = new TickWatchdogService();

    private final OffenderAggregator aggregator = new OffenderAggregator();
    private final StackSampler sampler = new StackSampler();
    private TickStatsWindow stats = new TickStatsWindow(1200, TickWatchdogConfig.getSlowTickThresholdNanos());
    private long lastReportAtMs;
    private long lastNotifyAtMs;
    private int consecutiveSlow;
    private boolean detailedArmed;
    private volatile boolean running;
    private List<OffenderAggregator.OffenderSnapshot> lastTop = List.of();

    private TickWatchdogService() {
    }

    public static TickWatchdogService instance() {
        return INSTANCE;
    }

    public static boolean isTimingEnabled() {
        return TIMING_ENABLED.get();
    }

    public void start(MinecraftServer server) {
        running = TickWatchdogConfig.isEnabled();
        stats = new TickStatsWindow(
                Math.max(200, TickWatchdogConfig.getWindowSeconds() * 20),
                TickWatchdogConfig.getSlowTickThresholdNanos()
        );
        aggregator.clear();
        sampler.resetAggregator();
        lastTop = List.of();
        consecutiveSlow = 0;
        detailedArmed = TickWatchdogConfig.isDetailedAlways();
        TIMING_ENABLED.set(running && detailedArmed);
        if (running) {
            sampler.start();
            Thread indexer = new Thread(WatchdogModIndex::rebuild, "cointcore-watchdog-modindex");
            indexer.setDaemon(true);
            indexer.start();
            LOGGER.info(
                    "Tick watchdog started (threshold={} ms, mode={}, sampling={} ms)",
                    TickWatchdogConfig.getSlowTickThresholdMs(),
                    TickWatchdogConfig.getDetailedTimingMode(),
                    TickWatchdogConfig.getSamplingIntervalMs()
            );
        }
    }

    public void stop() {
        running = false;
        TIMING_ENABLED.set(false);
        sampler.stop();
        TickProbe.clear();
    }

    public void setEnabled(boolean enabled) {
        running = enabled;
        if (running) {
            sampler.start();
        } else {
            TIMING_ENABLED.set(false);
            sampler.stop();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void onServerTickStart() {
        if (!isRunning()) {
            TIMING_ENABLED.set(false);
            return;
        }
        boolean next = TickWatchdogConfig.isDetailedAlways()
                || (TickWatchdogConfig.isDetailedOff() ? false : detailedArmed);
        TIMING_ENABLED.set(next && (TickWatchdogConfig.isAttributeBlockEntities()
                || TickWatchdogConfig.isAttributeEntities()));
    }

    public void onServerTickEnd(MinecraftServer server, long tickNanos) {
        if (!isRunning()) {
            return;
        }
        stats.record(tickNanos);
        boolean slow = tickNanos >= TickWatchdogConfig.getSlowTickThresholdNanos();
        if (slow) {
            consecutiveSlow++;
            if (consecutiveSlow >= TickWatchdogConfig.getAutoDetailedAfterSlowTicks()
                    && !TickWatchdogConfig.isDetailedOff()) {
                detailedArmed = true;
            }
        } else {
            consecutiveSlow = 0;
            if (!TickWatchdogConfig.isDetailedAlways()) {
                detailedArmed = false;
            }
        }

        long now = System.currentTimeMillis();
        if (now - lastReportAtMs >= TickWatchdogConfig.getReportIntervalSeconds() * 1000L) {
            writeReport(server);
            lastReportAtMs = now;
        }
        if (slow && consecutiveSlow >= TickWatchdogConfig.getSustainedSlowTicks()) {
            warnSustainedLag(server, tickNanos);
        }
    }

    public void recordBlockEntity(BlockEntityTiming timing) {
        if (!isRunning() || !TickWatchdogConfig.isAttributeBlockEntities() || timing == null) {
            return;
        }
        aggregator.addBlockEntity(
                timing.typeId(),
                timing.modId(),
                timing.dimension(),
                timing.x(),
                timing.y(),
                timing.z(),
                timing.nanos()
        );
    }

    public void recordEntity(EntityTiming timing) {
        if (!isRunning() || !TickWatchdogConfig.isAttributeEntities() || timing == null) {
            return;
        }
        aggregator.addEntity(
                timing.typeId(),
                timing.modId(),
                timing.dimension(),
                timing.x(),
                timing.y(),
                timing.z(),
                timing.nanos()
        );
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
        return writeReport(server);
    }

    public record BlockEntityTiming(String typeId, String modId, String dimension, int x, int y, int z, long nanos) {
    }

    public record EntityTiming(String typeId, String modId, String dimension, int x, int y, int z, long nanos) {
    }

    private String writeReport(MinecraftServer server) {
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
                snapshot -> lookupChunk(server, snapshot)
        );

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
}
