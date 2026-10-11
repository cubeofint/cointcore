package com.mawlee.cointcore.watchdog.probes;

import com.mawlee.cointcore.watchdog.StackSampleAggregator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * AE2 / RS tick time via soft reflection, otherwise stack-sample attribution.
 */
public final class ModNetworkProbe {
    private static final Set<String> NETWORK_MODS = Set.of(
            "ae2",
            "appliedenergistics2",
            "refinedstorage",
            "refinedstorage2",
            "appeng"
    );

    private final RankedCounter apiNanos = new RankedCounter(64);
    private final RankedCounter sampleHits = new RankedCounter(64);
    private volatile boolean ae2Present;
    private volatile boolean rsPresent;

    public void recordApiNanos(String source, long nanos) {
        if (source == null || source.isBlank() || nanos <= 0L) {
            return;
        }
        apiNanos.increment(source, nanos);
    }

    public void markSoftDependency(boolean ae2, boolean rs) {
        ae2Present = ae2;
        rsPresent = rs;
    }

    public void consumeStackSamples(List<StackSampleAggregator.MethodStat> methods) {
        if (methods == null) {
            return;
        }
        for (StackSampleAggregator.MethodStat method : methods) {
            String mod = method.modId() == null ? "" : method.modId().toLowerCase(Locale.ROOT);
            if (NETWORK_MODS.contains(mod)) {
                sampleHits.increment(mod + " " + shortClass(method.className()) + "#" + method.methodName());
            }
        }
    }

    public List<String> formatLines(int top) {
        List<String> lines = new ArrayList<>();
        lines.add(String.format(
                Locale.ROOT,
                "mod networks: ae2=%s rs=%s",
                ae2Present ? "present" : "absent",
                rsPresent ? "present" : "absent"
        ));
        int index = 1;
        for (RankedCounter.Rank rank : apiNanos.top(top)) {
            lines.add(String.format(
                    Locale.ROOT,
                    "api %d) %s %.2f ms",
                    index++,
                    rank.key(),
                    rank.value() / 1_000_000.0D
            ));
        }
        index = 1;
        for (RankedCounter.Rank rank : sampleHits.top(top)) {
            lines.add(String.format(Locale.ROOT, "sample %d) %s ×%d", index++, rank.key(), rank.value()));
        }
        if (apiNanos.size() == 0 && sampleHits.size() == 0) {
            lines.add("no AE2/RS timings this window (API missing or no samples)");
        }
        return lines;
    }

    public void resetWindow() {
        apiNanos.clear();
        sampleHits.clear();
    }

    private static String shortClass(String className) {
        if (className == null) {
            return "?";
        }
        int dot = className.lastIndexOf('.');
        return dot >= 0 ? className.substring(dot + 1) : className;
    }
}
