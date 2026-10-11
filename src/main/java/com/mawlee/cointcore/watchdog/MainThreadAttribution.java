package com.mawlee.cointcore.watchdog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Groups a server-thread stack by mod / package for watchdog reports.
 */
public final class MainThreadAttribution {
    private MainThreadAttribution() {
    }

    public record Frame(String className, String methodName, String modId, String packageName, boolean eventLike) {
    }

    public record PackageHit(String packageName, String modId, int frames, boolean eventLike) {
    }

    public static List<Frame> topFrames(StackTraceElement[] stack, ClassToModMapper mapper, int limit) {
        if (stack == null || stack.length == 0 || limit <= 0) {
            return List.of();
        }
        ClassToModMapper safe = mapper == null ? ClassToModMapper.empty() : mapper;
        List<Frame> frames = new ArrayList<>();
        for (StackTraceElement element : stack) {
            if (element == null || shouldSkip(element.getClassName())) {
                continue;
            }
            String className = element.getClassName();
            frames.add(new Frame(
                    className,
                    element.getMethodName(),
                    safe.resolve(className),
                    packageName(className),
                    looksLikeEventHandler(className)
            ));
            if (frames.size() >= limit) {
                break;
            }
        }
        return List.copyOf(frames);
    }

    public static List<PackageHit> byPackage(List<Frame> frames, int limit) {
        if (frames == null || frames.isEmpty() || limit <= 0) {
            return List.of();
        }
        Map<String, PackageHit> grouped = new LinkedHashMap<>();
        for (Frame frame : frames) {
            String key = frame.modId() + '\0' + frame.packageName();
            PackageHit current = grouped.get(key);
            if (current == null) {
                grouped.put(key, new PackageHit(frame.packageName(), frame.modId(), 1, frame.eventLike()));
            } else {
                grouped.put(key, new PackageHit(
                        current.packageName(),
                        current.modId(),
                        current.frames() + 1,
                        current.eventLike() || frame.eventLike()
                ));
            }
        }
        List<PackageHit> hits = new ArrayList<>(grouped.values());
        hits.sort(Comparator.comparingInt(PackageHit::frames).reversed()
                .thenComparing(PackageHit::modId)
                .thenComparing(PackageHit::packageName));
        if (hits.size() > limit) {
            return List.copyOf(hits.subList(0, limit));
        }
        return List.copyOf(hits);
    }

    public static boolean looksLikeEventHandler(String className) {
        if (className == null) {
            return false;
        }
        String name = className.toLowerCase(Locale.ROOT);
        return name.contains("event")
                || name.contains("luckperms")
                || name.contains("handler")
                || name.contains("listener");
    }

    static boolean shouldSkip(String className) {
        if (className == null) {
            return true;
        }
        return className.startsWith("java.")
                || className.startsWith("jdk.")
                || className.startsWith("sun.")
                || className.startsWith("com.sun.")
                || className.startsWith("com.mawlee.cointcore.watchdog.");
    }

    static String packageName(String className) {
        if (className == null) {
            return "";
        }
        int dot = className.lastIndexOf('.');
        return dot < 0 ? "" : className.substring(0, dot);
    }
}
