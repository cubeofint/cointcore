package com.mawlee.cointcore.reminder;

import java.time.ZoneId;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Picks which regular message to send now. Minecraft-free, unit-tested.
 *
 * <p>A message is due once its latest slot is newer than the one it was last sent for. Due messages go out one
 * at a time, at least {@code gapMs} apart, earliest slot first and lower id first on a tie — so three messages on
 * the same slot go at +0, +15 and +30 minutes. A slot that passed before the planner first saw a message (server
 * start, {@code /addreg}) is not sent.
 */
public final class RegularPlanner {
    public record Item(int id, RegularSchedule schedule) {
    }

    private final Map<Integer, Long> lastSlotById = new HashMap<>();
    private long startMs;
    private long lastSendMs = Long.MIN_VALUE;

    public RegularPlanner(long startMs) {
        this.startMs = startMs;
    }

    public long startMs() {
        return startMs;
    }

    public void reset(long newStartMs) {
        lastSlotById.clear();
        startMs = newStartMs;
        lastSendMs = Long.MIN_VALUE;
    }

    public void forget(int id) {
        lastSlotById.remove(id);
    }

    /** Next message to send at {@code nowMs}; marks it sent. */
    public Optional<Item> poll(Collection<Item> items, long nowMs, ZoneId zone, long gapMs) {
        Item best = null;
        long bestSlot = 0L;
        for (Item item : items) {
            long slot = item.schedule().latestSlot(nowMs, startMs, zone);
            Long last = lastSlotById.get(item.id());
            if (last == null) {
                // First sight: whatever slot already passed counts as handled.
                lastSlotById.put(item.id(), slot);
                continue;
            }
            if (slot == RegularSchedule.NONE || slot <= last) {
                continue;
            }
            if (best == null || slot < bestSlot || slot == bestSlot && item.id() < best.id()) {
                best = item;
                bestSlot = slot;
            }
        }
        if (best == null || lastSendMs != Long.MIN_VALUE && nowMs - lastSendMs < gapMs) {
            return Optional.empty();
        }
        lastSlotById.put(best.id(), bestSlot);
        lastSendMs = nowMs;
        return Optional.of(best);
    }
}
