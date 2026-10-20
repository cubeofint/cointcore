package com.mawlee.cointcore.ftb;

/**
 * Decision for {@link ForceLoadIdleService}, kept free of Minecraft/FTB types for tests.
 * Mirrors FTB Chunks' own idle-unforce check (no online member, last login older than the window),
 * with exemptions for teams that have an extended (donor) member or a member whose rank is not known yet.
 */
public final class ForceLoadIdlePolicy {
    private ForceLoadIdlePolicy() {
    }

    public static boolean shouldUnforce(
            boolean serverTeam,
            boolean anyMemberOnline,
            boolean anyMemberExtended,
            boolean anyMemberUnknown,
            long lastLoginMs,
            long nowMs,
            long idleMs
    ) {
        if (serverTeam || anyMemberOnline || anyMemberExtended || anyMemberUnknown) {
            return false;
        }
        // 0 = FTB never recorded a login; do not treat unknown as idle.
        if (lastLoginMs <= 0L) {
            return false;
        }
        return nowMs - lastLoginMs > idleMs;
    }
}
