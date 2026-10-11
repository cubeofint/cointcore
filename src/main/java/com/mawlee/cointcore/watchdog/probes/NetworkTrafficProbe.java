package com.mawlee.cointcore.watchdog.probes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Per-player packet / byte windows when Connection averages are readable.
 */
public final class NetworkTrafficProbe {
    private final RankedCounter receivedPackets = new RankedCounter(256);
    private final RankedCounter sentPackets = new RankedCounter(256);
    private final RankedCounter receivedBytes = new RankedCounter(256);
    private final RankedCounter sentBytes = new RankedCounter(256);
    private volatile boolean available = true;

    public void record(String player, long recvPackets, long sendPackets, long recvBytes, long sendBytes) {
        if (player == null || player.isBlank()) {
            return;
        }
        available = true;
        if (recvPackets > 0L) {
            receivedPackets.increment(player, recvPackets);
        }
        if (sendPackets > 0L) {
            sentPackets.increment(player, sendPackets);
        }
        if (recvBytes > 0L) {
            receivedBytes.increment(player, recvBytes);
        }
        if (sendBytes > 0L) {
            sentBytes.increment(player, sendBytes);
        }
    }

    public void markUnavailable() {
        available = false;
    }

    public boolean isAvailable() {
        return available;
    }

    public List<String> formatLines(int top) {
        List<String> lines = new ArrayList<>();
        if (!available && receivedPackets.size() == 0 && sentPackets.size() == 0) {
            lines.add("network: skipped (no cheap packet counters)");
            return lines;
        }
        lines.add("network window (packets/bytes per player)");
        append(lines, "recv pkt", receivedPackets, top, false);
        append(lines, "sent pkt", sentPackets, top, false);
        append(lines, "recv bytes", receivedBytes, top, true);
        append(lines, "sent bytes", sentBytes, top, true);
        return lines;
    }

    public void resetWindow() {
        receivedPackets.clear();
        sentPackets.clear();
        receivedBytes.clear();
        sentBytes.clear();
    }

    private static void append(List<String> lines, String title, RankedCounter counter, int top, boolean bytes) {
        int index = 1;
        for (RankedCounter.Rank rank : counter.top(top)) {
            String value = bytes ? GcMemoryProbe.humanBytes(rank.value()) : Long.toString(rank.value());
            lines.add(String.format(Locale.ROOT, "%s %d) %s %s", title, index++, rank.key(), value));
        }
    }
}
