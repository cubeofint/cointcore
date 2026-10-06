package com.mawlee.cointcore.spark;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.OptionalDouble;

/**
 * Per-server-tick cache of Spark MSPT mean and derived perf gates.
 * Avoids repeated {@link SparkMetricsService#resolveMsptMean} work across BE / entity hot paths.
 */
public final class PerfTickCache {
    private static int cachedTick = Integer.MIN_VALUE;
    private static OptionalDouble mspt1m = OptionalDouble.empty();

    private PerfTickCache() {
    }

    public static void refresh(MinecraftServer server) {
        if (server == null) {
            return;
        }
        int tick = server.getTickCount();
        if (tick == cachedTick) {
            return;
        }
        cachedTick = tick;
        mspt1m = SparkMetricsService.resolveMsptMean("1m");
    }

    public static void refreshFromCurrentServer() {
        refresh(ServerLifecycleHooks.getCurrentServer());
    }

    public static OptionalDouble mspt1m() {
        ensure();
        return mspt1m;
    }

    public static boolean isMsptAtLeast(double threshold) {
        if (threshold <= 0.0D) {
            return false;
        }
        OptionalDouble mspt = mspt1m();
        return mspt.isPresent() && mspt.getAsDouble() >= threshold;
    }

    public static void reset() {
        cachedTick = Integer.MIN_VALUE;
        mspt1m = OptionalDouble.empty();
    }

    private static void ensure() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        refresh(server);
    }
}
