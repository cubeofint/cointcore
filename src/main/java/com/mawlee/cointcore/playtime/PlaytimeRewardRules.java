package com.mawlee.cointcore.playtime;

/**
 * Minecraft-free accrual: leftover online milliseconds carry across sessions.
 */
public final class PlaytimeRewardRules {
    private PlaytimeRewardRules() {
    }

    public static long intervalMillis(int intervalMinutes) {
        int minutes = Math.max(1, intervalMinutes);
        return minutes * 60_000L;
    }

    public static Accrual accrue(long accumulatedMs, long addedMs, long intervalMs) {
        long total = saturateAdd(Math.max(0L, accumulatedMs), Math.max(0L, addedMs));
        if (intervalMs <= 0L) {
            return new Accrual(total, 0);
        }
        long grants = total / intervalMs;
        if (grants > Integer.MAX_VALUE) {
            grants = Integer.MAX_VALUE;
        }
        long remaining = total - grants * intervalMs;
        return new Accrual(remaining, (int) grants);
    }

    public static long creditAmount(int grants, long amountPerInterval) {
        if (grants <= 0 || amountPerInterval <= 0L) {
            return 0L;
        }
        long per = amountPerInterval;
        if (grants > 1 && per > Long.MAX_VALUE / grants) {
            return Long.MAX_VALUE;
        }
        return per * grants;
    }

    private static long saturateAdd(long left, long right) {
        if (left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    public record Accrual(long remainingMs, int intervals) {
    }
}
