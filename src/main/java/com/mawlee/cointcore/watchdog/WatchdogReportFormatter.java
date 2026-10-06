package com.mawlee.cointcore.watchdog;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Builds human-readable watchdog report text (Russian).
 */
public final class WatchdogReportFormatter {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
            .withZone(ZoneOffset.UTC);

    private WatchdogReportFormatter() {
    }

    public record ChunkContext(String claimOwner, String forceLoadSource) {
    }

    public static String format(
            Instant now,
            TickStatsWindow stats,
            List<OffenderAggregator.OffenderSnapshot> blockEntities,
            List<OffenderAggregator.OffenderSnapshot> entities,
            List<OffenderAggregator.OffenderSnapshot> chunks,
            List<OffenderAggregator.OffenderSnapshot> dimensions,
            List<StackSampleAggregator.MethodStat> methods,
            long stackSamples,
            java.util.function.Function<OffenderAggregator.OffenderSnapshot, ChunkContext> chunkContextLookup
    ) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("=== CointCore Tick Watchdog ===").append('\n');
        sb.append("Время: ").append(TIME.format(now)).append('\n');
        sb.append(String.format(
                Locale.ROOT,
                "Тики в окне: %d | avg=%.2f ms | p95=%.2f ms | max=%.2f ms | TPS≈%.2f | медленных: %d%n",
                stats.size(),
                stats.averageMillis(),
                stats.percentile95Millis(),
                stats.maxMillis(),
                stats.estimatedTps(),
                stats.slowTicks()
        ));
        sb.append('\n');

        sb.append("--- Топ block entity ---").append('\n');
        appendOffenders(sb, blockEntities, chunkContextLookup);
        sb.append('\n');

        sb.append("--- Топ entity ---").append('\n');
        appendOffenders(sb, entities, chunkContextLookup);
        sb.append('\n');

        sb.append("--- Топ чанки ---").append('\n');
        appendOffenders(sb, chunks, chunkContextLookup);
        sb.append('\n');

        sb.append("--- Топ измерения ---").append('\n');
        if (dimensions.isEmpty()) {
            sb.append("(нет данных)").append('\n');
        } else {
            int rank = 1;
            for (OffenderAggregator.OffenderSnapshot item : dimensions) {
                sb.append(String.format(
                        Locale.ROOT,
                        "%d) %s total=%.2f ms ticks=%d%n",
                        rank++,
                        item.dimension(),
                        item.totalMillis(),
                        item.ticks()
                ));
            }
        }
        sb.append('\n');

        sb.append("--- Топ методы (self-time, семплов: ").append(stackSamples).append(") ---").append('\n');
        if (methods.isEmpty()) {
            sb.append("(нет семплов — медленных тиков не было или семплирование выключено)").append('\n');
        } else {
            int rank = 1;
            for (StackSampleAggregator.MethodStat method : methods) {
                sb.append(String.format(
                        Locale.ROOT,
                        "%d) %s#%s мод=%s self=%.1f%% total=%.1f%%",
                        rank++,
                        shortClass(method.className()),
                        method.methodName(),
                        method.modId(),
                        method.selfPercent(),
                        method.totalPercent()
                ));
                if (method.correlatedTypeId() != null && !method.correlatedTypeId().isBlank()) {
                    sb.append(String.format(
                            Locale.ROOT,
                            " @ %s %s %d %d %d",
                            method.correlatedTypeId(),
                            method.correlatedDimension(),
                            method.correlatedX(),
                            method.correlatedY(),
                            method.correlatedZ()
                    ));
                }
                sb.append('\n');
                sb.append(String.format(
                        Locale.ROOT,
                        "   → метод %s мода %s занял ~%.1f%% времени медленных тиков%n",
                        method.methodName(),
                        method.modId(),
                        method.selfPercent()
                ));
            }
        }
        return sb.toString();
    }

    public static String conciseTopLine(List<OffenderAggregator.OffenderSnapshot> top, int limit) {
        if (top == null || top.isEmpty()) {
            return "нет данных";
        }
        StringBuilder sb = new StringBuilder();
        int count = Math.min(limit, top.size());
        for (int i = 0; i < count; i++) {
            OffenderAggregator.OffenderSnapshot item = top.get(i);
            if (i > 0) {
                sb.append(" | ");
            }
            sb.append(i + 1).append(") ");
            if (item.kind() == OffenderAggregator.Kind.BLOCK_ENTITY || item.kind() == OffenderAggregator.Kind.ENTITY) {
                sb.append(item.typeId())
                        .append(' ')
                        .append(item.dimension())
                        .append(' ')
                        .append(item.x()).append(' ').append(item.y()).append(' ').append(item.z())
                        .append(String.format(Locale.ROOT, " (%.1f ms)", item.totalMillis()));
            } else {
                sb.append(item.typeId())
                        .append(' ')
                        .append(item.dimension())
                        .append(String.format(Locale.ROOT, " (%.1f ms)", item.totalMillis()));
            }
        }
        return sb.toString();
    }

    private static void appendOffenders(
            StringBuilder sb,
            List<OffenderAggregator.OffenderSnapshot> items,
            java.util.function.Function<OffenderAggregator.OffenderSnapshot, ChunkContext> chunkContextLookup
    ) {
        if (items == null || items.isEmpty()) {
            sb.append("(нет данных)").append('\n');
            return;
        }
        int rank = 1;
        for (OffenderAggregator.OffenderSnapshot item : items) {
            ChunkContext ctx = chunkContextLookup == null ? null : chunkContextLookup.apply(item);
            String claim = ctx == null || ctx.claimOwner() == null || ctx.claimOwner().isBlank()
                    ? "-"
                    : ctx.claimOwner();
            String force = ctx == null || ctx.forceLoadSource() == null || ctx.forceLoadSource().isBlank()
                    ? "no"
                    : ctx.forceLoadSource();
            sb.append(String.format(
                    Locale.ROOT,
                    "%d) %s мод=%s dim=%s pos=%d,%d,%d chunk=%d,%d total=%.2f ms avg=%.3f ms ticks=%d claim=%s forceload=%s%n",
                    rank++,
                    item.typeId(),
                    item.modId(),
                    item.dimension(),
                    item.x(),
                    item.y(),
                    item.z(),
                    item.chunkX(),
                    item.chunkZ(),
                    item.totalMillis(),
                    item.averageMillis(),
                    item.ticks(),
                    claim,
                    force
            ));
        }
    }

    private static String shortClass(String className) {
        if (className == null) {
            return "?";
        }
        int dot = className.lastIndexOf('.');
        return dot >= 0 ? className.substring(dot + 1) : className;
    }
}
