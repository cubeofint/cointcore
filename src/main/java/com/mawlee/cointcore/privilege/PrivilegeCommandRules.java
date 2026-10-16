package com.mawlee.cointcore.privilege;

/**
 * Minecraft-free rules for privilege command cooldowns and {@code /expadd} amounts.
 */
public final class PrivilegeCommandRules {
    private PrivilegeCommandRules() {
    }

    public static int remainingSeconds(long readyAtEpochMs, long nowEpochMs) {
        if (readyAtEpochMs <= nowEpochMs) {
            return 0;
        }
        long remaining = readyAtEpochMs - nowEpochMs;
        return (int) Math.ceil(remaining / 1000.0D);
    }

    public static long readyAtEpochMs(long nowEpochMs, int cooldownSeconds) {
        if (cooldownSeconds <= 0) {
            return nowEpochMs;
        }
        return nowEpochMs + cooldownSeconds * 1000L;
    }

    public static ExpAddAmountVerdict validateExpAddAmount(int amount, int maxAmount) {
        if (amount <= 0) {
            return ExpAddAmountVerdict.NON_POSITIVE;
        }
        if (amount > Math.max(1, maxAmount)) {
            return ExpAddAmountVerdict.OVER_MAX;
        }
        return ExpAddAmountVerdict.OK;
    }

    public enum ExpAddAmountVerdict {
        OK,
        NON_POSITIVE,
        OVER_MAX
    }
}
