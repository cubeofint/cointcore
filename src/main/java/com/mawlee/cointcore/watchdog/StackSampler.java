package com.mawlee.cointcore.watchdog;

import com.mawlee.cointcore.config.TickWatchdogConfig;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Daemon thread that samples the Server thread stack only after a tick has
 * already exceeded the slow threshold (and only while that tick is still running).
 */
public final class StackSampler {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Object lock = new Object();
    private Thread samplerThread;
    private volatile boolean running;
    private volatile Thread serverThread;
    private volatile boolean inTick;
    private volatile long tickGeneration;
    private final StackSampleAggregator aggregator = new StackSampleAggregator();

    public void start() {
        synchronized (lock) {
            if (running) {
                return;
            }
            running = true;
            samplerThread = new Thread(this::loop, "cointcore-tick-watchdog");
            samplerThread.setDaemon(true);
            samplerThread.setPriority(Thread.MIN_PRIORITY + 1);
            samplerThread.start();
        }
    }

    public void stop() {
        synchronized (lock) {
            running = false;
            inTick = false;
            lock.notifyAll();
            Thread thread = samplerThread;
            samplerThread = null;
            if (thread != null) {
                thread.interrupt();
            }
        }
        aggregator.clear();
        serverThread = null;
    }

    public void onTickStart(Thread thread) {
        if (thread == null) {
            return;
        }
        serverThread = thread;
        tickGeneration++;
        inTick = true;
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    public void onTickEnd() {
        inTick = false;
    }

    public StackSampleAggregator aggregator() {
        return aggregator;
    }

    public long sampleCount() {
        return aggregator.sampleCount();
    }

    public void resetAggregator() {
        aggregator.clear();
    }

    private void loop() {
        while (running) {
            try {
                long gen;
                synchronized (lock) {
                    while (running && !inTick) {
                        lock.wait(250L);
                    }
                    if (!running) {
                        return;
                    }
                    gen = tickGeneration;
                }

                long thresholdMs = Math.max(1L, (long) TickWatchdogConfig.getSlowTickThresholdMs());
                Thread.sleep(thresholdMs);
                if (!running || !inTick || tickGeneration != gen) {
                    continue;
                }

                long until = System.nanoTime() + Math.max(1, TickWatchdogConfig.getSamplingMaxDurationMs()) * 1_000_000L;
                long intervalMs = Math.max(1, TickWatchdogConfig.getSamplingIntervalMs());
                while (running && inTick && tickGeneration == gen && System.nanoTime() < until) {
                    Thread target = serverThread;
                    if (target != null && target.isAlive()) {
                        StackTraceElement[] stack = target.getStackTrace();
                        aggregator.addSample(stack, WatchdogModIndex.mapper(), TickProbe.snapshot());
                    }
                    Thread.sleep(intervalMs);
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            } catch (Throwable throwable) {
                LOGGER.debug("Tick watchdog sampler iteration failed", throwable);
                try {
                    Thread.sleep(50L);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
