package com.mawlee.cointcore.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Per-tick time budget for Logistics Networks' synchronous transfers. The mod processes every woken network in one
 * tick with no limit (10–31 ms average, spikes to 785 ms); past the budget the remaining networks wait one tick.
 * Deferred networks get FIFO priority, with at least one network processed per tick. Retries share the same budget.
 * Networks slower than {@code slow_network_log_ms} on
 * their own are logged with name and owner.
 *
 * <p>File: {@code config/cointcore/lag-fixes.json} section {@code logistics_networks_budget}. Defaults to enabled.
 * Only affects the synchronous path ({@code asyncPlanning = false} in Logistics Networks' config).
 */
public final class LogisticsNetworksBudgetConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean enabled = true;
    private static boolean optimizeSignals = true;
    private static long budgetNanos = 6_000_000L;
    private static long slowNetworkNanos = 20_000_000L;

    private LogisticsNetworksBudgetConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isOptimizeSignals() {
        return optimizeSignals;
    }

    public static long budgetNanos() {
        return budgetNanos;
    }

    public static long slowNetworkNanos() {
        return slowNetworkNanos;
    }

    static void applySection(Data data) {
        Data resolved = data != null ? data : defaultFileData();
        enabled = resolved.enabled;
        optimizeSignals = resolved.optimizeSignals;
        budgetNanos = Math.max(1, Math.min(50, resolved.budgetMs)) * 1_000_000L;
        slowNetworkNanos = Math.max(1, resolved.slowNetworkLogMs) * 1_000_000L;
    }

    static void logReload() {
        LOGGER.info("Reloaded Logistics Networks budget (enabled={}, budget={} ms, slow log={} ms, optimizeSignals={})",
                enabled, budgetNanos / 1_000_000L, slowNetworkNanos / 1_000_000L, optimizeSignals);
    }

    static Data defaultFileData() {
        return new Data();
    }

    static final class Data {
        @SerializedName("enabled")
        boolean enabled = true;

        /** Deduplicate exact signals within a pass and skip preparations with no enabled exporter. */
        @SerializedName("optimize_signals")
        boolean optimizeSignals = true;

        /** Milliseconds per tick Logistics Networks may spend before the rest of its networks wait a tick. */
        @SerializedName("budget_ms")
        int budgetMs = 6;

        /** A single network slower than this is logged (name, owner, nodes) at most once a minute. */
        @SerializedName("slow_network_log_ms")
        int slowNetworkLogMs = 20;
    }
}
