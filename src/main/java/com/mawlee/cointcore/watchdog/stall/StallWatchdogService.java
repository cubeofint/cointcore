package com.mawlee.cointcore.watchdog.stall;

import com.mawlee.cointcore.config.StallWatchdogConfig;
import com.mawlee.cointcore.damage.DamageGuard;
import com.mawlee.cointcore.watchdog.TickProbe;
import com.mawlee.cointcore.watchdog.probes.WatchdogProbeHub;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Daemon thread that watches a volatile tick-start timestamp written by the server thread.
 */
public final class StallWatchdogService implements Runnable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final StallWatchdogService INSTANCE = new StallWatchdogService();
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC);

    private final StallWatchdogStages stages = new StallWatchdogStages();
    private volatile boolean running;
    private volatile long tickStartMs;
    private volatile long tickCounter;
    private volatile Thread serverThread;
    private volatile MinecraftServer server;
    private Thread daemon;

    private StallWatchdogService() {
    }

    public static StallWatchdogService instance() {
        return INSTANCE;
    }

    public void start(MinecraftServer minecraftServer) {
        this.server = minecraftServer;
        StallWatchdogHotFlags.ENABLED = StallWatchdogConfig.isEnabled();
        StallWatchdogHotFlags.clearAbort();
        stages.reset();
        tickStartMs = System.currentTimeMillis();
        if (daemon != null && daemon.isAlive()) {
            running = true;
            return;
        }
        running = true;
        daemon = new Thread(this, "cointcore-stall-watchdog");
        daemon.setDaemon(true);
        daemon.start();
        LOGGER.info(
                "Stall watchdog started (warn_after={}s, notify={}s, break={}s enabled={})",
                StallWatchdogConfig.getWarnAfterSeconds(),
                StallWatchdogConfig.getNotifyStaffAfterSeconds(),
                StallWatchdogConfig.getBreakLoopAfterSeconds(),
                StallWatchdogConfig.isBreakLoopEnabled()
        );
    }

    public void stop() {
        running = false;
        StallWatchdogHotFlags.ENABLED = false;
        StallWatchdogHotFlags.clearAbort();
        StallWatchdogHotFlags.TEST_STALL = false;
        server = null;
        serverThread = null;
        if (daemon != null) {
            daemon.interrupt();
        }
    }

    public void onServerTickStart(Thread thread) {
        serverThread = thread;
        tickStartMs = System.currentTimeMillis();
        tickCounter++;
        if (StallWatchdogHotFlags.ABORT_DAMAGE || StallWatchdogHotFlags.ABORT_ENTITY) {
            StallWatchdogHotFlags.clearAbort();
        }
    }

    public long tickStartMs() {
        return tickStartMs;
    }

    public long tickCounter() {
        return tickCounter;
    }

    public boolean isRunning() {
        return running && StallWatchdogHotFlags.ENABLED;
    }

    public String statusLine() {
        long elapsed = tickStartMs <= 0L ? 0L : System.currentTimeMillis() - tickStartMs;
        return String.format(
                Locale.ROOT,
                "Stall watchdog: enabled=%s running=%s last_tick_age=%dms ticks=%d in_stall=%s abort_damage=%s abort_entity=%s context=%s",
                StallWatchdogConfig.isEnabled(),
                running,
                elapsed,
                tickCounter,
                stages.inStall(),
                StallWatchdogHotFlags.ABORT_DAMAGE,
                StallWatchdogHotFlags.ABORT_ENTITY,
                StallContext.describe()
        );
    }

    public boolean shouldAbortEntity(Entity entity) {
        return StallWatchdogHotFlags.ABORT_ENTITY
                && entity != null
                && entity.getId() == StallWatchdogHotFlags.ABORT_ENTITY_ID;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(StallWatchdogConfig.getPollIntervalMs());
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
            if (!StallWatchdogConfig.isEnabled()) {
                StallWatchdogHotFlags.ENABLED = false;
                continue;
            }
            StallWatchdogHotFlags.ENABLED = true;
            StallWatchdogStages.Settings settings = new StallWatchdogStages.Settings(
                    StallWatchdogConfig.getWarnAfterMs(),
                    StallWatchdogConfig.getDumpIntervalMs(),
                    StallWatchdogConfig.getMaxDumps(),
                    StallWatchdogConfig.getNotifyStaffAfterMs(),
                    StallWatchdogConfig.getBreakLoopAfterMs(),
                    StallWatchdogConfig.isBreakLoopEnabled()
            );
            long now = System.currentTimeMillis();
            long started = tickStartMs;
            for (StallWatchdogStages.Action action : stages.evaluate(now, started, settings)) {
                handle(action, now, started);
            }
        }
    }

    private void handle(StallWatchdogStages.Action action, long now, long started) {
        long elapsedSec = started <= 0L ? 0L : Math.max(0L, (now - started) / 1000L);
        switch (action) {
            case NONE -> {
            }
            case WARN_DUMP, REPEAT_DUMP -> dump(action == StallWatchdogStages.Action.WARN_DUMP, elapsedSec);
            case NOTIFY_STAFF -> {
                String line = "Server tick stalled for " + elapsedSec + "s, suspect: " + StallContext.describe();
                LOGGER.warn(line);
                StallStaffNotify.notifyStaff(server, line);
            }
            case REQUEST_BREAK -> requestBreak();
            case RECOVERED -> {
                LOGGER.warn(
                        "Server tick recovered after {} ms. context={}",
                        stages.lastStallDurationMs(),
                        StallContext.describe()
                );
                StallWatchdogHotFlags.clearAbort();
            }
            default -> {
                StallWatchdogStages.Action ignored = action;
                if (ignored != StallWatchdogStages.Action.NONE) {
                    throw new IllegalStateException("unhandled stall action " + ignored);
                }
            }
        }
    }

    private void dump(boolean first, long elapsedSec) {
        Thread thread = serverThread;
        String serverStack = thread == null ? "<no server thread>" : formatStack(thread.getStackTrace());
        boolean collapsed = stages.noteStack(serverStack);
        String context = StallContext.describe();
        String nearby = nearbyPlayers();
        if (collapsed) {
            LOGGER.warn(
                    "Server tick still stalled ({}s), identical stack ×{}, context={}",
                    elapsedSec,
                    stages.collapsedStacks() + 1,
                    context
            );
        } else if (first) {
            LOGGER.warn(
                    "Server tick stalled for {}s, context={}, nearby=[{}]\n{}",
                    elapsedSec,
                    context,
                    nearby,
                    serverStack
            );
        } else {
            LOGGER.warn(
                    "Server tick stall dump #{}, {}s, context={}\n{}",
                    stages.dumps(),
                    elapsedSec,
                    context,
                    serverStack
            );
        }
        writeDumpFile(elapsedSec, context, nearby, serverStack, collapsed);
    }

    private void requestBreak() {
        int kind = StallContext.kind();
        int depth = DamageGuard.counters().currentDepth();
        if (kind == StallContext.KIND_DAMAGE || depth > 0) {
            StallWatchdogHotFlags.ABORT_DAMAGE = true;
            LOGGER.warn("Stall break: aborting further damage processing (depth={})", depth);
        }
        if (kind == StallContext.KIND_ENTITY) {
            StallWatchdogHotFlags.ABORT_ENTITY = true;
            StallWatchdogHotFlags.ABORT_ENTITY_ID = StallContext.entityId();
            StallWatchdogHotFlags.DISCARD_ENTITY = StallWatchdogConfig.isDiscardOffendingEntity();
            LOGGER.warn(
                    "Stall break: aborting entity tick #{} discard={}",
                    StallContext.entityId(),
                    StallWatchdogHotFlags.DISCARD_ENTITY
            );
        }
    }

    private String nearbyPlayers() {
        MinecraftServer current = server;
        if (current == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        String dim = StallContext.dimension();
        int cx = StallContext.x();
        int cz = StallContext.z();
        try {
            for (ServerPlayer player : current.getPlayerList().getPlayers()) {
                if (!dim.isEmpty() && !player.level().dimension().location().toString().equals(dim)) {
                    continue;
                }
                int dx = player.getBlockX() - cx;
                int dz = player.getBlockZ() - cz;
                if (dx * dx + dz * dz > 64 * 64) {
                    continue;
                }
                if (!builder.isEmpty()) {
                    builder.append(',');
                }
                builder.append(player.getGameProfile().getName());
            }
        } catch (Throwable exception) {
            return "?";
        }
        return builder.toString();
    }

    private void writeDumpFile(
            long elapsedSec,
            String context,
            String nearby,
            String serverStack,
            boolean collapsed
    ) {
        String stamp = FILE_TS.format(Instant.now());
        String body = buildDumpBody(elapsedSec, context, nearby, serverStack, collapsed);
        Path gameDir = server == null ? Path.of(".") : server.getServerDirectory();
        Path configured = gameDir.resolve(StallWatchdogConfig.getDumpDir());
        Path fallback = gameDir.resolve("config").resolve("cointcore").resolve("dumps");
        writeOne(configured.resolve("stall-" + stamp + ".txt"), body);
        if (!configured.equals(fallback) && StallWatchdogConfig.getDumpDir().contains("cointcore-stalls")) {
            writeOne(fallback.resolve("stall-" + stamp + ".txt"), body);
        }
    }

    private static void writeOne(Path file, String body) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, body, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            LOGGER.warn("Failed to write stall dump {}", file, exception);
        }
    }

    private String buildDumpBody(
            long elapsedSec,
            String context,
            String nearby,
            String serverStack,
            boolean collapsed
    ) {
        StringWriter writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);
        out.println("CointCore stall dump " + Instant.now());
        out.println("elapsed_s=" + elapsedSec);
        out.println("tick=" + tickCounter);
        out.println("context=" + context);
        out.println("nearby=" + nearby);
        out.println("probe=" + WatchdogProbeHub.instance().statusText().replace('\n', ' '));
        out.println("tick_probe=" + TickProbe.snapshot());
        out.println("collapsed=" + collapsed);
        out.println();
        out.println("=== Server thread ===");
        out.println(serverStack);
        out.println();
        out.println("=== All threads ===");
        try {
            ThreadMXBean bean = ManagementFactory.getThreadMXBean();
            ThreadInfo[] infos = bean.dumpAllThreads(true, true);
            for (ThreadInfo info : infos) {
                out.println(info.toString());
            }
        } catch (Throwable exception) {
            out.println("dumpAllThreads failed: " + exception);
        }
        out.flush();
        return writer.toString();
    }

    private static String formatStack(StackTraceElement[] stack) {
        StringBuilder builder = new StringBuilder(stack.length * 48);
        for (StackTraceElement frame : stack) {
            builder.append("  at ").append(frame).append('\n');
        }
        return builder.toString();
    }
}
