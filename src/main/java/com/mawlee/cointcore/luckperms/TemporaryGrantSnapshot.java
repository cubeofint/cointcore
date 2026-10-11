package com.mawlee.cointcore.luckperms;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Temporary permission / inheritance grants collected from own + group nodes
 * (no {@code resolveDistinctInheritedNodes} copy/sort).
 */
public final class TemporaryGrantSnapshot {
    static final TemporaryGrantSnapshot EMPTY = new TemporaryGrantSnapshot(List.of());

    private final List<Grant> grants;

    TemporaryGrantSnapshot(List<Grant> grants) {
        this.grants = grants == null ? List.of() : List.copyOf(grants);
    }

    public static TemporaryGrantSnapshot of(List<Grant> grants) {
        return new TemporaryGrantSnapshot(grants);
    }

    public Optional<Instant> earliestActive(String permission) {
        Instant earliest = null;
        for (Grant grant : grants) {
            if (grant.expired || grant.expiry == null || !grant.matches(permission)) {
                continue;
            }
            if (earliest == null || grant.expiry.isBefore(earliest)) {
                earliest = grant.expiry;
            }
        }
        return Optional.ofNullable(earliest);
    }

    public boolean hadExpired(String permission) {
        for (Grant grant : grants) {
            if (grant.expired && grant.matches(permission)) {
                return true;
            }
        }
        return false;
    }

    public record Grant(String permissionKey, boolean inheritance, boolean expired, Instant expiry) {
        boolean matches(String permission) {
            if (inheritance) {
                return true;
            }
            return permission != null && permission.equals(permissionKey);
        }
    }

    static TemporaryGrantSnapshot collect(Iterable<Grant> discovered) {
        if (discovered == null) {
            return EMPTY;
        }
        List<Grant> grants = new ArrayList<>();
        for (Grant grant : discovered) {
            if (grant != null) {
                grants.add(grant);
            }
        }
        return grants.isEmpty() ? EMPTY : new TemporaryGrantSnapshot(grants);
    }
}
