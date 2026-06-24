package com.mawlee.cointcore.punishment;

import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.UUID;

public final class PunishmentHistory {
    public static final long NO_EXPIRY = -1L;
    public static final long PERMANENT = 0L;

    private PunishmentHistory() {
    }

    public static void record(
            MinecraftServer server,
            PunishmentType type,
            UUID targetId,
            String targetName,
            String issuer,
            String reason,
            long expiresAt
    ) {
        PunishmentRecord record = new PunishmentRecord(
                UUID.randomUUID(),
                type,
                targetId,
                targetName == null ? "?" : targetName,
                issuer == null ? "Console" : issuer,
                reason == null ? "" : reason,
                System.currentTimeMillis(),
                expiresAt
        );
        PunishmentHistorySavedData.get(server).append(record);
    }

    public static List<PunishmentRecord> getRecords(MinecraftServer server, UUID targetId, int limit) {
        return PunishmentHistorySavedData.get(server).getRecords(targetId, limit);
    }

    public static int count(MinecraftServer server, UUID targetId) {
        return PunishmentHistorySavedData.get(server).count(targetId);
    }
}
