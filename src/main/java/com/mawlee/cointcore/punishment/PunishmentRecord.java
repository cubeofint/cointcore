package com.mawlee.cointcore.punishment;

import java.util.UUID;

public record PunishmentRecord(
        UUID id,
        PunishmentType type,
        UUID targetId,
        String targetName,
        String issuer,
        String reason,
        long issuedAt,
        long expiresAt
) {
    public boolean isPermanent() {
        return expiresAt == 0L;
    }

    public boolean hasExpiry() {
        return expiresAt > 0L;
    }
}
