package com.mawlee.cointcore.shop;

/**
 * Pure apply rules for a site operation against a known in-game balance.
 * Duplicate delivery is handled by the caller via {@link SiteOperationSavedData}.
 */
public final class SiteOperationApply {
    public static final String APPLIED = "applied";
    public static final String FAILED = "failed";
    public static final String INSUFFICIENT_SERVER_BALANCE = "insufficient_server_balance";

    private SiteOperationApply() {
    }

    public record Decision(
            String status,
            String error,
            long balanceAfter,
            boolean mutated,
            long movementAmount,
            long signedDelta
    ) {
    }

    public static Decision decide(SiteOperation.Kind kind, long amount, long currentBalance) {
        long current = Math.max(0L, currentBalance);
        return switch (kind) {
            case TO_SERVER -> credit(current, amount);
            case FROM_SERVER -> debit(current, amount);
            case ADJUST -> {
                if (amount > 0L) {
                    yield credit(current, amount);
                }
                if (amount < 0L) {
                    yield debit(current, amount == Long.MIN_VALUE ? Long.MAX_VALUE : -amount);
                }
                yield new Decision(APPLIED, null, current, false, 0L, 0L);
            }
        };
    }

    /**
     * Re-delivery of an already handled op: same status, but report the wallet as it is now.
     * The stored balance may be stale (pay/trader since), and the site compares balance_after
     * with its mirror for drift, so a stale value would enqueue a spurious reconcile.
     */
    public static Decision replay(String previousStatus, Long previousBalanceAfter, long currentBalance) {
        long balance = Math.max(0L, currentBalance);
        return new Decision(previousStatus, errorFor(previousStatus), balance, false, 0L, 0L);
    }

    private static Decision credit(long current, long amount) {
        if (amount <= 0L) {
            return new Decision(FAILED, "malformed_operation", current, false, 0L, 0L);
        }
        long next = current > Long.MAX_VALUE - amount ? Long.MAX_VALUE : current + amount;
        long delta = next - current;
        return new Decision(APPLIED, null, next, delta != 0L, Math.abs(delta), delta);
    }

    private static Decision debit(long current, long amount) {
        if (amount <= 0L) {
            return new Decision(FAILED, "malformed_operation", current, false, 0L, 0L);
        }
        if (current < amount) {
            return new Decision(FAILED, INSUFFICIENT_SERVER_BALANCE, current, false, 0L, 0L);
        }
        long next = current - amount;
        return new Decision(APPLIED, null, next, true, amount, -amount);
    }

    private static String errorFor(String status) {
        return FAILED.equals(status) ? INSUFFICIENT_SERVER_BALANCE : null;
    }
}
