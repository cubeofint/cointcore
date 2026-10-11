package com.mawlee.cointcore.watchdog.probes;

import com.mawlee.cointcore.watchdog.TickWatchdogService;
import com.mawlee.cointcore.watchdog.WatchdogWorldContext;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * Owns all extended probes. Tick path is increments / MXBean reads only.
 */
public final class WatchdogProbeHub {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final WatchdogProbeHub INSTANCE = new WatchdogProbeHub();
    private static final AtomicBoolean ACTIVE = new AtomicBoolean();

    private final GcMemoryProbe gcMemory = new GcMemoryProbe();
    private final ChunkLoadProbe chunks = new ChunkLoadProbe();
    private final DiskSaveProbe diskSave = new DiskSaveProbe();
    private final ServerTaskProbe serverTasks = new ServerTaskProbe();
    private final BlockUpdateProbe blockUpdates = new BlockUpdateProbe();
    private final GroundEntityProbe groundEntities = new GroundEntityProbe();
    private final PathfindingProbe pathfinding = new PathfindingProbe();
    private final NetworkTrafficProbe network = new NetworkTrafficProbe();
    private final ModNetworkProbe modNetworks = new ModNetworkProbe();
    private final TrendProbe trends = new TrendProbe();

    private volatile Thread serverThread;
    private volatile long lastPeriodicAtMs;
    private volatile long lastGroundScanAtMs;
    private volatile long lastTrendLogAtMs;
    private volatile long lastTriggerSampleAtMs;

    private WatchdogProbeHub() {
    }

    public static WatchdogProbeHub instance() {
        return INSTANCE;
    }

    public static boolean isActive() {
        return ACTIVE.get();
    }

    public void start() {
        ACTIVE.set(true);
        lastPeriodicAtMs = 0L;
        lastGroundScanAtMs = 0L;
        lastTrendLogAtMs = 0L;
        lastTriggerSampleAtMs = 0L;
    }

    public void stop() {
        ACTIVE.set(false);
        serverThread = null;
    }

    public ChunkLoadProbe chunks() {
        return chunks;
    }

    public DiskSaveProbe diskSave() {
        return diskSave;
    }

    public BlockUpdateProbe blockUpdates() {
        return blockUpdates;
    }

    public PathfindingProbe pathfinding() {
        return pathfinding;
    }

    public void onTickStart(Thread thread) {
        if (!ACTIVE.get()) {
            return;
        }
        if (thread != null) {
            serverThread = thread;
        }
    }

    public void onTickEnd(MinecraftServer server, long tickNanos, boolean slow) {
        if (!ACTIVE.get()) {
            return;
        }
        WatchdogProbeSettings.Snapshot settings = WatchdogProbeSettings.current();
        if (settings.gcMemory()) {
            gcMemory.onTickEnd(slow, settings.gcPauseFlagMs());
        }
        if (settings.trends()) {
            trends.recordTick(tickNanos);
        }
        if (slow) {
            maybeSampleTasks(settings);
        }
        long now = System.currentTimeMillis();
        if (now - lastPeriodicAtMs >= settings.analysisIntervalSeconds() * 1000L) {
            lastPeriodicAtMs = now;
            runPeriodic(server, settings, now);
        }
    }

    public void analyzeNow(MinecraftServer server) {
        WatchdogProbeSettings.Snapshot settings = WatchdogProbeSettings.current();
        runPeriodic(server, settings, System.currentTimeMillis());
        maybeSampleTasks(settings);
    }

    public String formatReport(MinecraftServer server) {
        WatchdogProbeSettings.Snapshot settings = WatchdogProbeSettings.current();
        if (settings.modNetworks()) {
            try {
                modNetworks.consumeStackSamples(TickWatchdogService.instance().sampler().aggregator().topBySelf(32));
            } catch (RuntimeException ignored) {
                // sampler may be idle
            }
        }
        int top = settings.topEntries();
        List<ProbeReportFormatter.Section> sections = new ArrayList<>();
        if (settings.gcMemory()) {
            sections.add(new ProbeReportFormatter.Section("GC и память", gcMemory.formatLines(gcMemory.snapshot())));
        }
        if (settings.chunks()) {
            sections.add(new ProbeReportFormatter.Section("Чанки", chunks.formatLines(top)));
        }
        if (settings.diskSave()) {
            sections.add(new ProbeReportFormatter.Section("Сохранения", diskSave.formatLines(top)));
        }
        if (settings.serverTasks()) {
            sections.add(new ProbeReportFormatter.Section("Задачи и события", serverTasks.formatLines(top)));
        }
        if (settings.blockUpdates()) {
            sections.add(new ProbeReportFormatter.Section("Блок-обновления", blockUpdates.formatLines(top)));
        }
        if (settings.groundEntities()) {
            sections.add(new ProbeReportFormatter.Section("Предметы / XP / снаряды", groundEntities.formatLines(top)));
        }
        if (settings.pathfinding()) {
            sections.add(new ProbeReportFormatter.Section("Pathfinding", pathfinding.formatLines(top)));
        }
        if (settings.network()) {
            sections.add(new ProbeReportFormatter.Section("Сеть", network.formatLines(top)));
        }
        if (settings.modNetworks()) {
            sections.add(new ProbeReportFormatter.Section("Сети модов", modNetworks.formatLines(top)));
        }
        if (settings.trends()) {
            sections.add(new ProbeReportFormatter.Section("Тренды", trends.formatLines(top)));
        }
        return ProbeReportFormatter.format(sections);
    }

    public String statusText() {
        WatchdogProbeSettings.Snapshot settings = WatchdogProbeSettings.current();
        GcMemoryProbe.Snapshot gc = gcMemory.snapshot();
        return String.format(
                Locale.ROOT,
                "Watchdog probes active=%s 1m=%.2f ms 5m=%.2f ms heap=%.1f%%%n"
                        + "gc=%s chunks=%s disk=%s tasks=%s blocks=%s ground=%s path=%s net=%s mods=%s trends=%s%n"
                        + "%s%n%s",
                ACTIVE.get(),
                trends.oneMinuteMspt(),
                trends.fiveMinuteMspt(),
                gc.heapUsedPercent(),
                onOff(settings.gcMemory()),
                onOff(settings.chunks()),
                onOff(settings.diskSave()),
                onOff(settings.serverTasks()),
                onOff(settings.blockUpdates()),
                onOff(settings.groundEntities()),
                onOff(settings.pathfinding()),
                onOff(settings.network()),
                onOff(settings.modNetworks()),
                onOff(settings.trends()),
                diskSave.statusLine(),
                serverTasks.statusLine()
        );
    }

    public void resetWindows() {
        gcMemory.resetWindow();
        chunks.resetWindow();
        diskSave.resetWindow();
        serverTasks.resetWindow();
        blockUpdates.resetWindow();
        groundEntities.resetWindow();
        pathfinding.resetWindow();
        network.resetWindow();
        modNetworks.resetWindow();
        trends.resetWindow();
    }

    private void runPeriodic(MinecraftServer server, WatchdogProbeSettings.Snapshot settings, long now) {
        if (server == null) {
            return;
        }
        try {
            if (settings.serverTasks()) {
                serverTasks.recordQueueLength(readPendingTasks(server));
            }
            if (settings.blockUpdates()) {
                sampleScheduledWork(server);
            }
            if (settings.network()) {
                sampleNetwork(server);
            }
            if (settings.modNetworks()) {
                sampleModNetworks();
            }
            if (settings.groundEntities() && now - lastGroundScanAtMs >= settings.groundScanIntervalSeconds() * 1000L) {
                lastGroundScanAtMs = now;
                sampleGroundEntities(server);
            }
            if (settings.trends() && now - lastTrendLogAtMs >= settings.trendLogIntervalSeconds() * 1000L) {
                lastTrendLogAtMs = now;
                sampleTrends(server);
                LOGGER.info(
                        "CointCore watchdog trends: 1m={}ms 5m={}ms {}",
                        String.format(Locale.ROOT, "%.2f", trends.oneMinuteMspt()),
                        String.format(Locale.ROOT, "%.2f", trends.fiveMinuteMspt()),
                        summarizeGrowth(settings.topEntries())
                );
            }
        } catch (RuntimeException exception) {
            LOGGER.debug("Watchdog probe periodic scan failed", exception);
        }
    }

    private void maybeSampleTasks(WatchdogProbeSettings.Snapshot settings) {
        if (!settings.serverTasks()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastTriggerSampleAtMs < 1_000L) {
            return;
        }
        lastTriggerSampleAtMs = now;
        serverTasks.sampleServerThreadAsync(serverThread, settings.stackSampleCount());
    }

    private void sampleScheduledWork(MinecraftServer server) {
        long fluids = 0L;
        long blocks = 0L;
        long light = 0L;
        for (ServerLevel level : server.getAllLevels()) {
            try {
                fluids += level.getFluidTicks().count();
                blocks += level.getBlockTicks().count();
            } catch (RuntimeException ignored) {
                // keep going
            }
            light += readLightBacklog(level);
        }
        blockUpdates.recordScheduled(fluids, blocks, light);
    }

    private void sampleGroundEntities(MinecraftServer server) {
        Map<String, int[]> counts = new HashMap<>();
        for (ServerLevel level : server.getAllLevels()) {
            String dim = WatchdogWorldContext.dimensionId(level);
            for (Entity entity : level.getAllEntities()) {
                int kind = groundKind(entity);
                if (kind < 0) {
                    continue;
                }
                ChunkPos pos = entity.chunkPosition();
                String key = ChunkLoadProbe.coordKey(dim, pos.x, pos.z);
                int[] bucket = counts.computeIfAbsent(key, ignored -> new int[3]);
                bucket[kind]++;
            }
        }
        groundEntities.replaceScan(counts);
    }

    private void sampleTrends(MinecraftServer server) {
        Map<String, int[]> totals = new HashMap<>();
        for (ServerLevel level : server.getAllLevels()) {
            String dim = WatchdogWorldContext.dimensionId(level);
            Map<Long, int[]> perChunk = new HashMap<>();
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof ServerPlayer) {
                    continue;
                }
                long packed = entity.chunkPosition().toLong();
                int[] bucket = perChunk.computeIfAbsent(packed, ignored -> new int[2]);
                bucket[1]++;
            }
            for (Map.Entry<Long, int[]> entry : perChunk.entrySet()) {
                ChunkPos pos = new ChunkPos(entry.getKey());
                int be = readBlockEntityCount(level, pos);
                entry.getValue()[0] = be;
                String base = claimOrCluster(level, dim, pos);
                int[] total = totals.computeIfAbsent(base, ignored -> new int[2]);
                total[0] += entry.getValue()[0];
                total[1] += entry.getValue()[1];
            }
        }
        List<TrendProbe.BaseCount> counts = new ArrayList<>(totals.size());
        totals.forEach((base, value) -> counts.add(new TrendProbe.BaseCount(base, value[0], value[1])));
        trends.recordBaseCounts(counts);
    }

    private void sampleNetwork(MinecraftServer server) {
        boolean any = false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Object connection = readConnection(player);
            if (connection == null) {
                continue;
            }
            long recvPkt = readNumber(connection, "getAverageReceivedPackets", "averageReceivedPackets");
            long sentPkt = readNumber(connection, "getAverageSentPackets", "averageSentPackets");
            long recvBytes = readNumber(connection, "getAverageReceivedBytes", "receivedBytes");
            long sentBytes = readNumber(connection, "getAverageSentBytes", "sentBytes");
            if (recvPkt > 0L || sentPkt > 0L || recvBytes > 0L || sentBytes > 0L) {
                any = true;
                network.record(player.getGameProfile().getName(), recvPkt, sentPkt, recvBytes, sentBytes);
            }
        }
        if (!any) {
            network.markUnavailable();
        }
    }

    private void sampleModNetworks() {
        boolean ae2 = isModLoaded("ae2");
        boolean rs = isModLoaded("refinedstorage") || isModLoaded("refinedstorage2");
        modNetworks.markSoftDependency(ae2, rs);
        if (ae2) {
            sampleAe2();
        }
        // RS 2 has no stable global tick clock; stack samples cover it.
    }

    private void sampleAe2() {
        Object handler = invokeStatic("appeng.hooks.ticking.TickHandler", "instance");
        if (handler == null) {
            handler = invokeStatic("appeng.me.helpers.TickHandler", "instance");
        }
        if (handler == null) {
            return;
        }
        Object grids = invokeNoArg(handler, "getGridList", "getGrids", "grids");
        if (grids instanceof Collection<?> collection) {
            int index = 0;
            for (Object grid : collection) {
                long nanos = readGridNanos(grid);
                if (nanos > 0L) {
                    modNetworks.recordApiNanos("ae2-grid-" + index, nanos);
                }
                index++;
                if (index >= 16) {
                    break;
                }
            }
        }
    }

    private static long readGridNanos(Object grid) {
        if (grid == null) {
            return 0L;
        }
        Object tickManager = invokeNoArg(grid, "getTickManager", "getTick");
        Object target = tickManager == null ? grid : tickManager;
        long value = readNumber(target, "getAverageTime", "getAverage", "getIdleTime");
        if (value > 1_000_000L) {
            return value;
        }
        if (value > 0L) {
            return value * 1_000_000L;
        }
        return 0L;
    }

    private String summarizeGrowth(int top) {
        List<TrendProbe.BaseGrowth> growth = trends.growth();
        if (growth.isEmpty()) {
            return "no base growth";
        }
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(top, growth.size());
        for (int i = 0; i < limit; i++) {
            TrendProbe.BaseGrowth item = growth.get(i);
            if (i > 0) {
                sb.append(" | ");
            }
            sb.append(item.base())
                    .append(" BE")
                    .append(signed(item.beDelta()))
                    .append(" ent")
                    .append(signed(item.entityDelta()));
        }
        return sb.toString();
    }

    private static String signed(int value) {
        return value >= 0 ? "+" + value : Integer.toString(value);
    }

    private static String claimOrCluster(ServerLevel level, String dim, ChunkPos pos) {
        try {
            String claim = WatchdogWorldContext.describeChunk(level, pos.x, pos.z).claimOwner();
            if (claim != null && !claim.isBlank() && !"-".equals(claim)) {
                return claim;
            }
        } catch (RuntimeException ignored) {
            // fall through
        }
        return dim + " cluster:" + (pos.x >> 2) + "," + (pos.z >> 2);
    }

    private static int readBlockEntityCount(ServerLevel level, ChunkPos pos) {
        try {
            ChunkAccess access = level.getChunk(pos.x, pos.z, ChunkStatus.FULL, false);
            if (access instanceof LevelChunk chunk) {
                return chunk.getBlockEntities().size();
            }
        } catch (RuntimeException ignored) {
            return 0;
        }
        return 0;
    }

    private static int groundKind(Entity entity) {
        if (entity instanceof ItemEntity) {
            return 0;
        }
        if (entity instanceof ExperienceOrb) {
            return 1;
        }
        if (entity instanceof Projectile) {
            return 2;
        }
        return -1;
    }

    private static int readPendingTasks(MinecraftServer server) {
        Number value = invokeNumber(server, "getPendingTasksCount", "getTaskCount");
        if (value != null) {
            return value.intValue();
        }
        Object queue = readField(server, "pendingRunnables", "pendingTasks", "blockingTasks");
        if (queue instanceof Collection<?> collection) {
            return collection.size();
        }
        return 0;
    }

    private static long readLightBacklog(ServerLevel level) {
        try {
            Object engine = level.getLightEngine();
            Number value = invokeNumber(engine, "getTaskCount", "getQueuedUpdates", "getPendingUpdates");
            return value == null ? 0L : value.longValue();
        } catch (RuntimeException ignored) {
            return 0L;
        }
    }

    private static Object readConnection(ServerPlayer player) {
        try {
            Object listener = player.connection;
            if (listener == null) {
                return null;
            }
            Object connection = invokeNoArg(listener, "getConnection", "getConnectionInternal");
            return connection == null ? readField(listener, "connection") : connection;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static void recordChunkLoad(ServerLevel level, LevelChunk chunk, boolean isNewChunk) {
        if (!isActive() || !WatchdogProbeSettings.current().chunks() || level == null || chunk == null) {
            return;
        }
        ChunkPos pos = chunk.getPos();
        String dim = WatchdogWorldContext.dimensionId(level);
        String cause = "unknown";
        String playerName = null;
        try {
            if (level.getForcedChunks().contains(pos.toLong())) {
                cause = "forced";
            }
            Player nearest = level.getNearestPlayer(
                    pos.getMiddleBlockX(),
                    80.0D,
                    pos.getMiddleBlockZ(),
                    128.0D,
                    (Predicate<Entity>) null
            );
            if (nearest instanceof ServerPlayer player) {
                playerName = player.getGameProfile().getName();
                boolean flying = player.getAbilities().flying
                        || player.isFallFlying()
                        || player.getDeltaMovement().horizontalDistanceSqr() > 0.36D;
                cause = flying ? "player_fly" : "player";
            }
        } catch (RuntimeException ignored) {
            // attribution is best-effort
        }
        instance().chunks.record(dim, pos.x, pos.z, isNewChunk, cause, playerName);
    }

    public static void recordNeighborUpdate() {
        if (!isActive() || !WatchdogProbeSettings.current().blockUpdates()) {
            return;
        }
        instance().blockUpdates.incrementNeighbor();
    }

    private static boolean isModLoaded(String modId) {
        try {
            return ModList.get().isLoaded(modId);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object invokeStatic(String className, String method) {
        try {
            Method found = Class.forName(className).getMethod(method);
            return found.invoke(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invokeNoArg(Object target, String... names) {
        if (target == null) {
            return null;
        }
        for (String name : names) {
            try {
                Method method = target.getClass().getMethod(name);
                return method.invoke(target);
            } catch (Throwable ignored) {
                // try next
            }
        }
        return null;
    }

    private static Number invokeNumber(Object target, String... names) {
        Object value = invokeNoArg(target, names);
        return value instanceof Number number ? number : null;
    }

    private static long readNumber(Object target, String... names) {
        Number number = invokeNumber(target, names);
        if (number != null) {
            return Math.max(0L, number.longValue());
        }
        for (String name : names) {
            Object field = readField(target, name);
            if (field instanceof Number value) {
                return Math.max(0L, value.longValue());
            }
        }
        return 0L;
    }

    private static Object readField(Object target, String... names) {
        if (target == null) {
            return null;
        }
        Class<?> type = target.getClass();
        while (type != null && type != Object.class) {
            for (String name : names) {
                try {
                    Field field = type.getDeclaredField(name);
                    field.setAccessible(true);
                    return field.get(target);
                } catch (Throwable ignored) {
                    // try next
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private static String onOff(boolean enabled) {
        return enabled ? "on" : "off";
    }
}
