package com.mawlee.cointcore.watchdog.probes;

import com.mawlee.cointcore.watchdog.ClassToModMapper;
import com.mawlee.cointcore.watchdog.WatchdogModIndex;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Server executor queue length plus on-trigger top-frame samples grouped by mod.
 */
public final class ServerTaskProbe {
    private final AtomicInteger lastQueueLength = new AtomicInteger();
    private final AtomicInteger maxQueueLength = new AtomicInteger();
    private final RankedCounter mods = new RankedCounter(256);
    private final RankedCounter frames = new RankedCounter(256);
    private final AtomicBoolean sampling = new AtomicBoolean();
    private volatile long lastSampleAtMs;

    public void recordQueueLength(int length) {
        int value = Math.max(0, length);
        lastQueueLength.set(value);
        maxQueueLength.accumulateAndGet(value, Math::max);
    }

    public void addSample(StackTraceElement[] stack, ClassToModMapper mapper) {
        if (stack == null || stack.length == 0) {
            return;
        }
        ClassToModMapper resolved = mapper == null ? ClassToModMapper.empty() : mapper;
        String topClass = stack[0].getClassName();
        String topMethod = stack[0].getMethodName();
        mods.increment(resolved.resolve(topClass));
        frames.increment(shortClass(topClass) + "#" + topMethod);
    }

    public void sampleServerThreadAsync(Thread serverThread, int sampleCount) {
        if (serverThread == null || !serverThread.isAlive() || !sampling.compareAndSet(false, true)) {
            return;
        }
        int samples = Math.max(1, sampleCount);
        Thread worker = new Thread(() -> {
            try {
                ThreadMXBean bean = ManagementFactory.getThreadMXBean();
                ClassToModMapper mapper = WatchdogModIndex.mapper();
                for (int i = 0; i < samples; i++) {
                    ThreadInfo info = bean.getThreadInfo(serverThread.threadId(), 12);
                    if (info != null) {
                        addSample(info.getStackTrace(), mapper);
                    }
                    Thread.sleep(2L);
                }
                lastSampleAtMs = System.currentTimeMillis();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException ignored) {
                // never break the server for sampling
            } finally {
                sampling.set(false);
            }
        }, "cointcore-watchdog-task-sample");
        worker.setDaemon(true);
        worker.setPriority(Thread.MIN_PRIORITY + 1);
        worker.start();
    }

    public List<String> formatLines(int top) {
        List<String> lines = new ArrayList<>();
        lines.add(String.format(
                Locale.ROOT,
                "server tasks: queue=%d max=%d samples=%s",
                lastQueueLength.get(),
                maxQueueLength.get(),
                lastSampleAtMs == 0L ? "none" : "ok"
        ));
        int index = 1;
        for (RankedCounter.Rank rank : mods.top(top)) {
            lines.add(String.format(Locale.ROOT, "event/mod %d) %s ×%d", index++, rank.key(), rank.value()));
        }
        index = 1;
        for (RankedCounter.Rank rank : frames.top(top)) {
            lines.add(String.format(Locale.ROOT, "frame %d) %s ×%d", index++, rank.key(), rank.value()));
        }
        return lines;
    }

    public String statusLine() {
        return String.format(Locale.ROOT, "tasks: queue=%d max=%d", lastQueueLength.get(), maxQueueLength.get());
    }

    public void resetWindow() {
        maxQueueLength.set(lastQueueLength.get());
        mods.clear();
        frames.clear();
    }

    static String shortClass(String className) {
        if (className == null || className.isBlank()) {
            return "?";
        }
        int dot = className.lastIndexOf('.');
        return dot >= 0 ? className.substring(dot + 1) : className;
    }
}
