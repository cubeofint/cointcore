package com.mawlee.cointcore.watchdog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates stack samples into self-time and total-time method rankings.
 */
public final class StackSampleAggregator {
    public record MethodStat(
            String className,
            String methodName,
            String modId,
            long selfHits,
            long totalHits,
            double selfPercent,
            double totalPercent,
            String correlatedTypeId,
            String correlatedDimension,
            int correlatedX,
            int correlatedY,
            int correlatedZ
    ) {
    }

    private static final class MutableMethod {
        private long selfHits;
        private long totalHits;
        private final String className;
        private final String methodName;
        private final String modId;
        private String correlatedTypeId = "";
        private String correlatedDimension = "";
        private int correlatedX;
        private int correlatedY;
        private int correlatedZ;
        private long correlatedHits;

        private MutableMethod(String className, String methodName, String modId) {
            this.className = className;
            this.methodName = methodName;
            this.modId = modId;
        }
    }

    private final Map<String, MutableMethod> methods = new HashMap<>();
    private long sampleCount;

    public void addSample(StackTraceElement[] stack, ClassToModMapper mapper, TickProbe.Snapshot probe) {
        if (stack == null || stack.length == 0) {
            return;
        }
        sampleCount++;
        ClassToModMapper safeMapper = mapper == null ? ClassToModMapper.empty() : mapper;

        // self = topmost application frame (skip java/sun/jdk frames when possible)
        StackTraceElement selfFrame = pickSelfFrame(stack);
        if (selfFrame != null) {
            MutableMethod self = methods.computeIfAbsent(
                    key(selfFrame),
                    ignored -> new MutableMethod(
                            selfFrame.getClassName(),
                            selfFrame.getMethodName(),
                            safeMapper.resolve(selfFrame.getClassName())
                    )
            );
            self.selfHits++;
            correlate(self, probe);
        }

        // total = every frame once per sample
        Map<String, Boolean> seen = new HashMap<>();
        for (StackTraceElement frame : stack) {
            if (frame == null || shouldSkip(frame.getClassName())) {
                continue;
            }
            String frameKey = key(frame);
            if (seen.putIfAbsent(frameKey, Boolean.TRUE) != null) {
                continue;
            }
            MutableMethod total = methods.computeIfAbsent(
                    frameKey,
                    ignored -> new MutableMethod(
                            frame.getClassName(),
                            frame.getMethodName(),
                            safeMapper.resolve(frame.getClassName())
                    )
            );
            total.totalHits++;
            correlate(total, probe);
        }
    }

    public List<MethodStat> topBySelf(int limit) {
        return top(limit, true);
    }

    public List<MethodStat> topByTotal(int limit) {
        return top(limit, false);
    }

    public long sampleCount() {
        return sampleCount;
    }

    public void clear() {
        methods.clear();
        sampleCount = 0L;
    }

    private List<MethodStat> top(int limit, boolean bySelf) {
        int capped = Math.max(0, limit);
        double denom = sampleCount <= 0L ? 1.0D : (double) sampleCount;
        List<MethodStat> list = new ArrayList<>(methods.size());
        for (MutableMethod method : methods.values()) {
            list.add(new MethodStat(
                    method.className,
                    method.methodName,
                    method.modId,
                    method.selfHits,
                    method.totalHits,
                    100.0D * method.selfHits / denom,
                    100.0D * method.totalHits / denom,
                    method.correlatedTypeId,
                    method.correlatedDimension,
                    method.correlatedX,
                    method.correlatedY,
                    method.correlatedZ
            ));
        }
        Comparator<MethodStat> comparator = bySelf
                ? Comparator.comparingLong(MethodStat::selfHits).reversed()
                : Comparator.comparingLong(MethodStat::totalHits).reversed();
        list.sort(comparator.thenComparing(MethodStat::className).thenComparing(MethodStat::methodName));
        if (list.size() > capped) {
            return List.copyOf(list.subList(0, capped));
        }
        return List.copyOf(list);
    }

    private static void correlate(MutableMethod method, TickProbe.Snapshot probe) {
        if (probe == null || !probe.active()) {
            return;
        }
        method.correlatedHits++;
        // Keep the most frequently seen probe for this method.
        if (method.correlatedTypeId.isEmpty() || method.correlatedHits == 1L) {
            method.correlatedTypeId = probe.typeId();
            method.correlatedDimension = probe.dimension();
            method.correlatedX = probe.x();
            method.correlatedY = probe.y();
            method.correlatedZ = probe.z();
        }
    }

    private static StackTraceElement pickSelfFrame(StackTraceElement[] stack) {
        for (StackTraceElement frame : stack) {
            if (frame != null && !shouldSkip(frame.getClassName())) {
                return frame;
            }
        }
        return stack[0];
    }

    private static boolean shouldSkip(String className) {
        if (className == null) {
            return true;
        }
        return className.startsWith("java.")
                || className.startsWith("jdk.")
                || className.startsWith("sun.")
                || className.startsWith("com.sun.")
                || className.startsWith("com.mawlee.cointcore.watchdog.");
    }

    private static String key(StackTraceElement frame) {
        return frame.getClassName() + '#' + frame.getMethodName();
    }
}
