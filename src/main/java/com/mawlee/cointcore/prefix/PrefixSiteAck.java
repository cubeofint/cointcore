package com.mawlee.cointcore.prefix;

/**
 * Site ack for a {@code prefix_select} decision.
 *
 * <p>LuckPerms mutation/load failures are {@code failed} (retryable) and must not be
 * persisted in the idempotency ledger or reported as {@code rejected unavailable}.
 */
public record PrefixSiteAck(String status, String reason, String selected, boolean persist) {
    public static PrefixSiteAck from(PrefixSelectionRules.Decision decision) {
        return switch (decision.kind()) {
            case APPLIED, UNCHANGED -> new PrefixSiteAck("applied", decision.reason(), decision.selectedGroup(), true);
            case AUTO -> new PrefixSiteAck("applied", decision.reason(), null, true);
            case UNAVAILABLE -> new PrefixSiteAck("rejected", decision.reason(), null, true);
            case FAILED -> new PrefixSiteAck("failed", decision.reason(), null, false);
        };
    }
}
